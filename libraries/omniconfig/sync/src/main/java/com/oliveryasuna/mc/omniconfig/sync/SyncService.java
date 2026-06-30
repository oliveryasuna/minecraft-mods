package com.oliveryasuna.mc.omniconfig.sync;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.platform.PermissionGate;
import com.oliveryasuna.mc.omniconfig.sync.protocol.PayloadCodec;
import com.oliveryasuna.mc.omniconfig.sync.protocol.ProtocolVersion;
import com.oliveryasuna.mc.omniconfig.sync.protocol.SyncPayload;
import com.oliveryasuna.mc.omniconfig.sync.protocol.WireFormatException;
import com.oliveryasuna.mc.omniconfig.value.CodecRegistry;
import com.oliveryasuna.mc.omniconfig.value.Section;
import com.oliveryasuna.mc.omniconfig.value.TreePaths;
import com.oliveryasuna.mc.omniconfig.value.ValueTree;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Orchestrates server &lt;-&gt; client config sync across registered
 * {@link ConfigManager}s.
 *
 * <h2>Server-side flow</h2>
 * <ol>
 *     <li>{@link #register(ConfigManager)} every config we own.</li>
 *     <li>{@link #start()} subscribes to inbound bytes for handshake replies and
 *     (eventually) op-gated client edits.</li>
 *     <li>On client connect: {@link #onClientConnected(Object)} ships one
 *     {@link SyncPayload.Handshake} then one {@link SyncPayload.Snapshot} per
 *     registered config.</li>
 *     <li>On local change: {@link #broadcastSnapshot(String)} or
 *     {@link #broadcastDelta(String, List)} pushes the new authoritative state
 *     to every connected client.</li>
 * </ol>
 *
 * <h2>Client-side flow</h2>
 * <ol>
 *     <li>{@link #register(ConfigManager)} every config we participate in.</li>
 *     <li>{@link #start()} subscribes; inbound snapshots / deltas are
 *     validated by {@link InboundValidator} and applied via
 *     {@link ScopeEnforcer#applyAuthoritative}. {@code CLIENT}-scoped fields
 *     on the local instance are never touched, regardless of what the server
 *     sent.</li>
 * </ol>
 *
 * <h2>Op-gated inbound writes</h2>
 * Client-to-server config edits travel as {@link SyncPayload.ClientEdit}.
 * Server-side {@link #onPayloadBytes(Object, byte[])} routes them through
 * {@link #handleClientEdit(Object, SyncPayload.ClientEdit)}:
 * {@link PermissionGate#canEdit canEdit(actor, requiredLevel)} (level
 * {@value #CLIENT_EDIT_REQUIRED_LEVEL}) gates the whole payload;
 * {@link InboundValidator} drops per-entry violations
 * (decode, validator, caps); accepted entries are applied via
 * {@link ScopeEnforcer#applyAuthoritative} and broadcast back to every
 * connected client as a {@link SyncPayload.Delta} so the originator settles
 * on the server-confirmed state. Permission denial is silent — the
 * {@link Listener#onClientEditDenied} hook fires on the server for
 * logging / audit, but no reply payload is sent. Clients ship edit
 * requests via {@link #sendClientEdit(String, List)}.
 */
public final class SyncService implements AutoCloseable {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Op-level required to commit a {@link SyncPayload.ClientEdit} on the server.
     */
    public static final int CLIENT_EDIT_REQUIRED_LEVEL = 2;

    //==================================================
    // Fields
    //==================================================

    private final Role role;
    private final NetworkTransport transport;
    private final CodecRegistry codecs;
    private final PermissionGate permissionGate;
    private final ConcurrentMap<String, ConfigManager<?>> managers;
    private final List<Listener> listeners;

    //==================================================
    // Constructors
    //==================================================

    public SyncService(
            final Role role,
            final NetworkTransport transport,
            final CodecRegistry codecs
    ) {
        this(role, transport, codecs, (actor, level) -> false);
    }

    public SyncService(
            final Role role,
            final NetworkTransport transport,
            final CodecRegistry codecs,
            final PermissionGate permissionGate
    ) {
        super();

        this.role = Objects.requireNonNull(role, "role");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.codecs = Objects.requireNonNull(codecs, "codecs");
        this.permissionGate = Objects.requireNonNull(permissionGate, "permissionGate");
        this.managers = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
    }

    //==================================================
    // Methods
    //==================================================

    // Registration
    //--------------------------------------------------

    public void register(final ConfigManager<?> manager) {
        managers.put(manager.getSchema().id(), manager);
    }

    public void addListener(final Listener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void start() {
        transport.subscribe(this::onPayloadBytes);
    }

    @Override
    public void close() {
        transport.close();
    }

    // Server-side push
    //--------------------------------------------------

    public void onClientConnected(final Object clientHandle) {
        transport.sendToClient(clientHandle, PayloadCodec.encode(buildHandshake()));
        for(final ConfigManager<?> manager : managers.values()) {
            transport.sendToClient(clientHandle, PayloadCodec.encode(buildSnapshot(manager)));
        }
    }

    public void broadcastSnapshot(final String configId) {
        final ConfigManager<?> manager = requireManager(configId);
        transport.sendToAllClients(PayloadCodec.encode(buildSnapshot(manager)));
    }

    public void broadcastDelta(
            final String configId,
            final List<SyncPayload.Delta.Entry> entries
    ) {
        requireManager(configId);
        transport.sendToAllClients(PayloadCodec.encode(new SyncPayload.Delta(configId, entries)));
    }

    // Client-side inbound
    //--------------------------------------------------

    public void onPayloadBytes(
            final Object source,
            final byte[] bytes
    ) {
        final SyncPayload payload;
        try {
            payload = PayloadCodec.decode(bytes);
        } catch(final WireFormatException error) {
            for(final Listener l : listeners) l.onWireError(source, error);
            return;
        }
        switch(payload) {
            case SyncPayload.Handshake h -> handleHandshake(source, h);
            case SyncPayload.Snapshot s -> handleSnapshot(s);
            case SyncPayload.Delta d -> handleDelta(d);
            case SyncPayload.ClientEdit e -> handleClientEdit(source, e);
        }
    }

    /**
     * Client-side send: ship an edit request to the server. The server may
     * accept, reject (whole-payload, on permission), or partially accept
     * (per-entry, on validator / cap). Acceptance arrives back as a
     * {@link SyncPayload.Delta} broadcast — there is no per-call reply.
     *
     * @param configId Target config.
     * @param entries  Edits to apply. Must reference {@code SERVER} or
     *                 {@code COMMON} scoped entries; client-scoped entries
     *                 are dropped by {@link ScopeEnforcer} server-side
     *                 regardless.
     */
    public void sendClientEdit(
            final String configId,
            final List<SyncPayload.Delta.Entry> entries
    ) {
        transport.sendToServer(PayloadCodec.encode(new SyncPayload.ClientEdit(configId, entries)));
    }

    // Build outgoing
    //--------------------------------------------------

    private SyncPayload.Handshake buildHandshake() {
        return new SyncPayload.Handshake(ProtocolVersion.CURRENT, new ArrayList<>(managers.keySet()));
    }

    private SyncPayload.Snapshot buildSnapshot(final ConfigManager<?> manager) {
        final ValueTree authoritative = ScopeEnforcer.extractAuthoritative(manager.getSchema(), manager.get(), codecs);
        return new SyncPayload.Snapshot(manager.getSchema().id(), authoritative);
    }

    // Handle inbound
    //--------------------------------------------------

    private void handleHandshake(
            final Object source,
            final SyncPayload.Handshake handshake
    ) {
        final Set<String> mine = managers.keySet();
        final Set<String> theirs = new HashSet<>(handshake.configIds());
        final Set<String> theyHaveWeDont = new HashSet<>(theirs);
        theyHaveWeDont.removeAll(mine);
        final Set<String> weHaveTheyDont = new HashSet<>(mine);
        weHaveTheyDont.removeAll(theirs);
        final HandshakeReport report = new HandshakeReport(handshake, theyHaveWeDont, weHaveTheyDont);
        for(final Listener l : listeners) l.onHandshake(source, report);
    }

    private void handleSnapshot(final SyncPayload.Snapshot snapshot) {
        final ConfigManager<?> manager = managers.get(snapshot.configId());
        if(manager == null) return;
        applyTree(manager, snapshot.tree(), false);
    }

    private void handleDelta(final SyncPayload.Delta delta) {
        final ConfigManager<?> manager = managers.get(delta.configId());
        if(manager == null) return;
        final Section sparse = new Section();
        for(final SyncPayload.Delta.Entry entry : delta.entries()) {
            TreePaths.put(sparse, entry.path(), entry.value());
        }
        applyTree(manager, new ValueTree(sparse), true);
    }

    private void handleClientEdit(
            final Object source,
            final SyncPayload.ClientEdit edit
    ) {
        final ConfigManager<?> manager = managers.get(edit.configId());
        if(manager == null) {
            return;
        }
        if(!permissionGate.canEdit(source, CLIENT_EDIT_REQUIRED_LEVEL)) {
            for(final Listener l : listeners) l.onClientEditDenied(source, edit.configId(), edit.entries());
            return;
        }

        final Section sparse = new Section();
        for(final SyncPayload.Delta.Entry entry : edit.entries()) {
            TreePaths.put(sparse, entry.path(), entry.value());
        }
        final InboundValidator.InboundResult result = InboundValidator.validate(manager.getSchema(), new ValueTree(sparse), codecs);
        final Set<String> appliedPaths = ScopeEnforcer.applyAuthoritative(manager.getSchema(), result.accepted(), manager.get(), codecs);
        // Server-side handler: this IS the local edit. Mark LOCAL_EDIT so
        // the server's own save() persists the entry (the broadcast Delta
        // back to clients lands as FROM_REMOTE on their side).
        manager.markOrigins(appliedPaths, ConfigManager.Origin.LOCAL_EDIT);

        // Persist server-side so the edit survives restart. Failure is
        // logged + propagated to listeners; in-memory state stays applied
        // either way.
        IOException saveError = null;
        try {
            manager.save();
        } catch(final IOException error) {
            saveError = error;
        }

        // Build the broadcast Delta from the accepted entries. Stays a flat
        // (path, value) list — same shape the originator sent — so other
        // clients apply via the existing handleDelta path.
        final List<SyncPayload.Delta.Entry> accepted = new ArrayList<>();
        final Set<String> rejectedPaths = new HashSet<>();
        for(final InboundValidator.Rejection r : result.rejections()) {
            rejectedPaths.add(r.path());
        }
        for(final SyncPayload.Delta.Entry entry : edit.entries()) {
            if(!rejectedPaths.contains(entry.path())) {
                accepted.add(entry);
            }
        }
        if(!accepted.isEmpty()) {
            transport.sendToAllClients(PayloadCodec.encode(new SyncPayload.Delta(edit.configId(), accepted)));
        }
        for(final Listener l : listeners) l.onClientEditApplied(source, edit.configId(), result);
        if(saveError != null) {
            for(final Listener l : listeners) l.onClientEditSaveFailed(source, edit.configId(), saveError);
        }
    }

    private void applyTree(
            final ConfigManager<?> manager,
            final ValueTree incoming,
            final boolean isDelta
    ) {
        final InboundValidator.InboundResult result = InboundValidator.validate(manager.getSchema(), incoming, codecs);
        final Set<String> appliedPaths = ScopeEnforcer.applyAuthoritative(manager.getSchema(), result.accepted(), manager.get(), codecs);
        // Origin tagging is role-dependent: a client receiving the server's
        // snapshot/delta records FROM_REMOTE so save() excludes them. A
        // server receiving the same payload kind (rare; mostly self-broadcast
        // loopback in singleplayer, which transport-layer skip prevents)
        // would over-tag its own LOCAL_EDIT state — preserve LOCAL_EDIT in
        // that case by treating SERVER role apply as a no-op for origin.
        if(role == Role.CLIENT) {
            manager.markOrigins(appliedPaths, ConfigManager.Origin.FROM_REMOTE);
        }
        for(final Listener l : listeners) {
            if(isDelta) {
                l.onDeltaApplied(manager.getSchema().id(), result);
            } else {
                l.onSnapshotApplied(manager.getSchema().id(), result);
            }
        }
    }

    // Helpers
    //--------------------------------------------------

    private ConfigManager<?> requireManager(final String configId) {
        final ConfigManager<?> manager = managers.get(configId);
        if(manager == null) {
            throw new IllegalArgumentException("no config registered with id: " + configId);
        }
        return manager;
    }

    //==================================================
    // Getters/setters
    //==================================================

    public PermissionGate getPermissionGate() {
        return permissionGate;
    }

    public Set<String> getRegisteredConfigIds() {
        return Set.copyOf(managers.keySet());
    }

    //==================================================
    // Nested
    //==================================================

    /**
     * Sink for sync events. Default no-ops; override only what you need.
     */
    public interface Listener {

        //==================================================
        // Methods
        //==================================================

        default void onHandshake(
                final Object source,
                final HandshakeReport report
        ) {
        }

        default void onSnapshotApplied(
                final String configId,
                final InboundValidator.InboundResult result
        ) {
        }

        default void onDeltaApplied(
                final String configId,
                final InboundValidator.InboundResult result
        ) {
        }

        default void onWireError(
                final Object source,
                final WireFormatException error
        ) {
        }

        /**
         * Fired server-side after a {@link SyncPayload.ClientEdit} from
         * {@code source} has been validated and (partially) applied. The
         * {@code result} carries both the accepted tree and per-entry
         * rejections (cap / validator / decode failures).
         */
        default void onClientEditApplied(
                final Object source,
                final String configId,
                final InboundValidator.InboundResult result
        ) {
        }

        /**
         * Fired server-side when a {@link SyncPayload.ClientEdit} from
         * {@code source} was rejected wholesale by the {@link PermissionGate}
         * — none of the requested entries were applied.
         */
        default void onClientEditDenied(
                final Object source,
                final String configId,
                final List<SyncPayload.Delta.Entry> entries
        ) {
        }

        /**
         * Fired server-side when the post-apply {@code manager.save()} for a
         * client edit threw — the in-memory state already has the accepted
         * entries applied and the broadcast Delta already went out, so
         * clients see the new value, but the on-disk TOML did not get the
         * update.
         */
        default void onClientEditSaveFailed(
                final Object source,
                final String configId,
                final IOException error
        ) {
        }

    }

    /**
     * Which side of the sync this service runs on. Determines how applied
     * snapshot/delta paths are tagged on the local {@link ConfigManager}'s
     * origin map: client-side apply records {@link ConfigManager.Origin#FROM_REMOTE},
     * server-side apply leaves origin unchanged so locally-authored values
     * keep their existing {@link ConfigManager.Origin#LOCAL_EDIT} tag.
     */
    public enum Role {

        CLIENT,
        SERVER

    }

    public record HandshakeReport(
            SyncPayload.Handshake remote,
            Set<String> remoteHasWeDont,
            Set<String> weHaveRemoteDont
    ) {

        //==================================================
        // Constructors
        //==================================================

        public HandshakeReport {
            remoteHasWeDont = Set.copyOf(remoteHasWeDont);
            weHaveRemoteDont = Set.copyOf(weHaveRemoteDont);
        }

        //==================================================
        // Methods
        //==================================================

        public boolean isMatch() {
            return remoteHasWeDont.isEmpty() && weHaveRemoteDont.isEmpty();
        }

    }

}
