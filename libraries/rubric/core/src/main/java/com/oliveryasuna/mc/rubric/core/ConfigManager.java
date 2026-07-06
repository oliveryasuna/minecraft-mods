package com.oliveryasuna.mc.rubric.core;

import com.oliveryasuna.mc.rubric.api.Format;
import com.oliveryasuna.mc.rubric.event.ChangeEvent;
import com.oliveryasuna.mc.rubric.event.ConfigEventBus;
import com.oliveryasuna.mc.rubric.io.ConfigIO;
import com.oliveryasuna.mc.rubric.io.FileWatchService;
import com.oliveryasuna.mc.rubric.lifecycle.LoadResult;
import com.oliveryasuna.mc.rubric.lifecycle.ReloadController;
import com.oliveryasuna.mc.rubric.lifecycle.ReloadListener;
import com.oliveryasuna.mc.rubric.migration.MigrationRegistry;
import com.oliveryasuna.mc.rubric.migration.MigrationReport;
import com.oliveryasuna.mc.rubric.migration.Migrator;
import com.oliveryasuna.mc.rubric.platform.Platform;
import com.oliveryasuna.mc.rubric.schema.AnnotationConfigModel;
import com.oliveryasuna.mc.rubric.schema.ConfigModel;
import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.schema.SchemaEntry;
import com.oliveryasuna.mc.rubric.validation.Correction;
import com.oliveryasuna.mc.rubric.validation.Corrector;
import com.oliveryasuna.mc.rubric.validation.ValidationResult;
import com.oliveryasuna.mc.rubric.validation.Validator;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
import com.oliveryasuna.mc.rubric.value.ConfigSnapshot;
import com.oliveryasuna.mc.rubric.value.ValueTree;
import com.oliveryasuna.mc.rubric.value.ValueTreeMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Orchestrates one config's lifecycle over a {@link ConfigModel} (POJO-backed
 * for annotation configs, map-backed for builder configs): read the file (or
 * seed defaults), migrate older versions, decode, correct, snapshot, write back
 * when needed, and dispatch change events on reload.
 * <p>
 * {@code S} is the state type — the POJO class for annotation configs, or
 * {@code Map<String, Object>} for builder configs.}
 */
public final class ConfigManager<S> {

    //==================================================
    // Static fields
    //==================================================

    private static final int ASSUMED_VERSION_WHEN_ABSENT = 1;

    //==================================================
    // Static methods
    //==================================================

    private static String extension(final Format format) {
        return switch(format) {
            case TOML -> ".toml";
            case JSON -> ".json";
            case JSON5 -> ".json5";
        };
    }

    //==================================================
    // Fields
    //==================================================

    private final ConfigModel<S> model;
    private final Schema schema;
    private final ConfigIO io;
    private final Platform platform;
    private final CodecRegistry codecs;
    private final Path file;
    private final ValueTreeMapper mapper;
    private final Corrector corrector;
    private final ReloadController reload;
    private final Migrator migrator;
    private final MigrationRegistry migrations;  // nullable
    private final ConfigEventBus events;
    private final List<ReloadListener<S>> reloadListeners = new CopyOnWriteArrayList<>();
    /**
     * File mtime captured after our own {@link #save()} writes — used by
     * {@link #reloadIfExternal} to skip the redundant reload that fires when
     * the watch service sees its own write. Any external edit advances the
     * mtime beyond this and the watch reload runs as before.
     */
    private volatile long lastSelfWriteMillis = 0L;
    /**
     * Per-path provenance tag. Absent path = {@link Origin#DEFAULT} (never
     * touched since bootstrap). {@link Origin#LOCAL_EDIT} = set via
     * {@link #set}, loaded from disk, or applied as a server-confirmed
     * {@code com.oliveryasuna.mc.rubric.sync.protocol.SyncPayload.ClientEdit}.
     * {@link Origin#FROM_REMOTE} = applied client-side from a server
     * snapshot or delta. {@link #save} filters {@code FROM_REMOTE} entries
     * out of the written tree so the local TOML is not polluted with
     * server-authoritative values that would otherwise reappear at
     * disconnect or startup before the next snapshot.
     */
    private final ConcurrentMap<String, Origin> origins = new ConcurrentHashMap<>();

    private S instance;
    private volatile ConfigSnapshot snapshot;

    //==================================================
    // Constructors
    //==================================================

    public ConfigManager(
            final ConfigModel<S> model,
            final ConfigIO io,
            final Platform platform,
            final CodecRegistry codecs,
            final MigrationRegistry migrations
    ) {
        super();

        this.model = model;
        this.io = io;
        this.platform = platform;
        this.codecs = codecs;
        this.migrations = migrations;
        this.schema = this.model.schema();
        this.mapper = new ValueTreeMapper(codecs);
        this.corrector = new Corrector();
        this.reload = new ReloadController(this.schema);
        this.instance = this.model.newState();
        this.file = this.platform.configDir().resolve(this.schema.name() + extension(this.schema.format()));
        this.migrator = new Migrator();
        this.events = new ConfigEventBus();
    }

    public ConfigManager(
            final Class<S> type,
            final ConfigIO io,
            final Platform platform,
            final CodecRegistry codecs,
            final MigrationRegistry migrations
    ) {
        // Codecs are threaded into AnnotationConfigModel so the schema reader
        // honors any registerLeaf(...) entries — e.g. ResourceLocation should
        // be a single SCALAR entry, not recursed into as a nested category.
        this(new AnnotationConfigModel<>(type, codecs), io, platform, codecs, migrations);
    }

    public ConfigManager(
            final Class<S> type,
            final ConfigIO io,
            final Platform platform,
            final CodecRegistry codecs
    ) {
        this(new AnnotationConfigModel<>(type, codecs), io, platform, codecs, null);
    }


    //==================================================
    // Methods
    //==================================================

    public LoadResult load() throws IOException {
        final ConfigSnapshot previous = this.snapshot;
        final S previousInstance = this.instance;
        final S fresh = model.newState();
        final List<Correction> corrections = new ArrayList<>();
        MigrationReport migration = null;

        // Reset origins for the new instance; disk-loaded entries are
        // captured as LOCAL_EDIT below (disk content is user-authoritative).
        origins.clear();

        final Optional<ValueTree> loaded = io.read(file, schema);
        final boolean existed = loaded.isPresent();
        int fileVersion = schema.version();
        if(existed) {
            final ValueTree tree = loaded.get();
            fileVersion = migrator.readVersion(tree, ASSUMED_VERSION_WHEN_ABSENT);
            migration = migrator.migrate(tree, fileVersion, schema.version(), migrations);
            corrections.addAll(mapper.fromTree(schema, tree, fresh, path -> origins.put(path, Origin.LOCAL_EDIT)));
        }
        corrections.addAll(corrector.correct(schema, fresh));

        this.instance = fresh;
        final ConfigSnapshot next = ConfigSnapshot.capture(schema, fresh);
        this.snapshot = next;

        final boolean migrated = migration != null && migration.migrated();
        final boolean downgrade = migration != null && migration.downgrade();
        final boolean versionUpgrade = existed && fileVersion < schema.version();
        if(!downgrade && (!existed || !corrections.isEmpty() || migrated || versionUpgrade)) {
            save();
        }
        if(previous != null) {
            dispatch(reload.diff(previous, next));
            fireReloadListeners(previousInstance, fresh);
        }
        return new LoadResult(next, corrections, migration);

    }

    private void fireReloadListeners(final S previousInstance, final S currentInstance) {
        for(final ReloadListener<S> listener : reloadListeners) {
            platform.mainThreadExecutor().execute(() -> {
                try {
                    listener.onReload(previousInstance, currentInstance);
                } catch(final RuntimeException ignored) {
                    // A faulty listener must not break the reload pipeline.
                }
            });
        }
    }

    /**
     * Subscribes to reload events fired after every successful subsequent
     * {@link #load()} — i.e. NOT the first one, where there is no previous
     * snapshot to diff. The listener receives the {@code previous} instance
     * (about to be replaced) and the {@code current} instance (the freshly
     * decoded one) and runs on the platform's main thread.
     * <p>
     * Use case: an open GUI screen reconciles user-touched widget values
     * with the new on-disk values without clobbering in-flight edits.
     *
     * @param listener Listener to add.
     */
    public void addReloadListener(final ReloadListener<S> listener) {
        reloadListeners.add(listener);
    }

    public void removeReloadListener(final ReloadListener<S> listener) {
        reloadListeners.remove(listener);
    }

    /**
     * Wires a {@link FileWatchService} to call {@link #reload()} whenever the
     * backing file changes, marshaling the call through {@code mainThread}
     * because {@link ConfigManager} is not thread-safe. Reload exceptions are
     * logged via the platform logger and otherwise swallowed (a transient
     * IO error must not tear down the file-watch pump).
     *
     * @param watch      Watch service to register with.
     * @param mainThread Executor that schedules tasks onto the platform's
     *                   main thread — typically
     *                   {@link Platform#mainThreadExecutor()}.
     * @throws IOException if registering the watch on the backing file fails.
     */
    public void startFileWatch(
            final FileWatchService watch,
            final Executor mainThread
    ) throws IOException {
        watch.watch(file, () -> mainThread.execute(this::reloadIfExternal));
    }

    /**
     * File-watch callback. Compares the file's current mtime to the one we
     * recorded after our last {@link #save()} write; if they match, the
     * change came from us and the reload is skipped (avoids the harmless
     * but noisy save → watch → reload echo). Any external editor / delta
     * save / sync-driven save advances the mtime past
     * {@link #lastSelfWriteMillis} and the reload runs as before.
     */
    private void reloadIfExternal() {
        try {
            final long currentMillis = Files.getLastModifiedTime(file).toMillis();
            if(currentMillis == lastSelfWriteMillis) {
                return;
            }
            reload();
        } catch(final NoSuchFileException missing) {
            // File deleted out from under us — let reload() handle the
            // "missing file" case (it seeds defaults + writes a fresh one).
            reloadSafely();
        } catch(final IOException error) {
            platform.logger("rubric").error("file-watch reload failed", error);
        }
    }

    private void reloadSafely() {
        try {
            reload();
        } catch(final IOException error) {
            platform.logger("rubric").error("file-watch reload failed", error);
        }
    }

    public LoadResult reload() throws IOException {
        return load();
    }

    public void save() throws IOException {
        // FROM_REMOTE entries are filtered: they came from a server snapshot
        // or delta and must not be persisted to the client's local TOML,
        // where they would reappear on disconnect/restart before the next
        // server push. LOCAL_EDIT and DEFAULT both persist — DEFAULT preserves
        // the write-through invariant that on-disk shape mirrors schema.
        final ValueTree tree = mapper.toTree(schema, instance, path -> origins.get(path) != Origin.FROM_REMOTE);
        migrator.stampVersion(tree, schema.version());
        io.write(file, tree, schema);
        try {
            lastSelfWriteMillis = Files.getLastModifiedTime(file).toMillis();
        } catch(final IOException ignored) {
            // Best-effort; failure here means the next file-watch event
            // can't recognise this write as self-originated and triggers a
            // harmless redundant reload. No state corruption.
        }
    }

    private void dispatch(final List<ChangeEvent> changes) {
        for(final ChangeEvent change : changes) {
            platform.mainThreadExecutor().execute(() -> events.fire(change));
        }
    }

    public S get() {
        return instance;
    }

    /**
     * Sets one value programmatically (validating against the entry's
     * constraints), update the snapshot, and fires a change event.
     * <p>
     * Does not persist; call {@link #save()} to write. Throws if the path is
     * unknown or the value violates a constraint.
     *
     * @param path  Path to the value.
     * @param value Value to set.
     */
    public void set(
            final String path,
            final Object value
    ) {
        final SchemaEntry entry = schema.find(path)
                .orElseThrow(() -> new IllegalArgumentException("no such config entry: " + path));
        for(final Validator<?> validator : entry.getMetadata().getValidators()) {
            final ValidationResult result = validator.validateRaw(value);
            if(!result.isValid()) {
                throw new IllegalArgumentException("invalid value for " + path + ": " + result.getIssues().getFirst().message());
            }
        }
        final ConfigSnapshot previous = this.snapshot;
        entry.writeTo(instance, value);
        origins.put(path, Origin.LOCAL_EDIT);
        final ConfigSnapshot next = ConfigSnapshot.capture(schema, instance);
        this.snapshot = next;
        dispatch(reload.diff(previous, next));
    }

    /**
     * Records the provenance of {@code path}. Sync code calls this after
     * applying a server snapshot/delta (with {@link Origin#FROM_REMOTE}) or
     * a server-accepted {@code ClientEdit} (with {@link Origin#LOCAL_EDIT})
     * so {@link #save} can filter correctly.
     */
    public void markOrigin(
            final String path,
            final Origin origin
    ) {
        origins.put(path, origin);
    }

    public void markOrigins(
            final Collection<String> paths,
            final Origin origin
    ) {
        for(final String path : paths) {
            origins.put(path, origin);
        }
    }

    /**
     * @return The origin recorded for {@code path}, or {@link Origin#DEFAULT}
     * if the path has not been written since the last load.
     */
    public Origin originOf(final String path) {
        return origins.getOrDefault(path, Origin.DEFAULT);
    }

    /**
     * @return Unmodifiable snapshot of the origin map. Test seam.
     */
    public Map<String, Origin> originsView() {
        return Map.copyOf(origins);
    }

    //==================================================
    // Getters/setters
    //==================================================

    public Schema getSchema() {
        return schema;
    }

    public Path getFile() {
        return file;
    }

    public ConfigSnapshot getSnapshot() {
        return snapshot;
    }

    public ConfigEventBus getEvents() {
        return events;
    }

    /**
     * @return The codec registry threaded into schema construction. GUI
     * widgets read this so codec-mediated controls (e.g. text editor for a
     * {@code ResourceLocation} field) round-trip via the registered codec.
     */
    public CodecRegistry getCodecs() {
        return codecs;
    }

    //==================================================
    // Nested
    //==================================================

    /**
     * Provenance tag for a single config path. Drives {@link #save}'s
     * scope-filter: only {@link #FROM_REMOTE} values are excluded.
     */
    public enum Origin {

        /**
         * Initial POJO/builder default; never written since bootstrap.
         */
        DEFAULT,
        /**
         * Set by local action: programmatic {@link #set}, GUI commit, file
         * load/reload, or server-side application of a confirmed
         * {@code ClientEdit}. Persisted by {@link #save}.
         */
        LOCAL_EDIT,
        /**
         * Applied client-side from a server snapshot or delta. Excluded by
         * {@link #save} so the local TOML stays free of server-authoritative
         * values that the next snapshot will overwrite anyway.
         */
        FROM_REMOTE

    }

}
