package com.oliveryasuna.mc.coal.adapter.common;

import com.oliveryasuna.mc.coal.api.config.ConfigManager;
import com.oliveryasuna.mc.coal.api.config.ConfigSnapshot;
import com.oliveryasuna.mc.coal.api.config.LoadResult;
import com.oliveryasuna.mc.coal.api.config.Origin;
import com.oliveryasuna.mc.coal.api.event.ChangeEvent;
import com.oliveryasuna.mc.coal.api.event.EventBus;
import com.oliveryasuna.mc.coal.api.event.ReloadListener;
import com.oliveryasuna.mc.coal.api.io.ConfigIO;
import com.oliveryasuna.mc.coal.api.migration.MigrationOp;
import com.oliveryasuna.mc.coal.api.migration.MigrationReport;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.migration.MigrationStep;
import com.oliveryasuna.mc.coal.api.schema.ConfigModel;
import com.oliveryasuna.mc.coal.api.schema.Schema;
import com.oliveryasuna.mc.coal.api.schema.SchemaEntry;
import com.oliveryasuna.mc.coal.api.validation.Correction;
import com.oliveryasuna.mc.coal.api.validation.Corrector;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The heart of the adapter. Owns state, wires reload/change dispatch, mediates
 * between the {@link Schema}, {@link ConfigIO}, {@link MigrationSpec}, and
 * {@link Corrector}. One instance per registered config.
 * <p>
 * State model:
 * <ul>
 *   <li>
 *       Annotation-driven ({@code S} = POJO): the manager holds a live instance
 *       whose fields are mutated in place by {@link SchemaEntry#writeTo}.
 *   </li>
 *   <li>
 *       Spec-driven ({@code S} = {@code Map<String, Object>}): the manager
 *       holds a flat {@code LinkedHashMap} keyed by full dotted path — that's
 *       the state {@link com.oliveryasuna.mc.coal.api.schema.SchemaEntry} the
 *       {@link AnnotationSchemaReader} produced for the spec expects.
 *   </li>
 * </ul>
 */
final class AdapterConfigManager<S> implements ConfigManager<S> {

    //==================================================
    // Static methods
    //==================================================

    // Tree hydration + extraction
    //--------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Object getByPath(
            final Map<String, Object> tree,
            final String dottedPath
    ) {
        Object cur = tree;
        for(final String seg : dottedPath.split("\\.")) {
            if(!(cur instanceof final Map<?, ?> m)) {
                return null;
            }

            cur = ((Map<String, Object>)m).get(seg);
            if(cur == null) {
                return null;
            }
        }

        return cur;
    }

    @SuppressWarnings("unchecked")
    private static boolean containsPath(
            final Map<String, Object> tree,
            final String dottedPath
    ) {
        Map<String, Object> cur = tree;
        final String[] segs = dottedPath.split("\\.");
        for(int i = 0; i < segs.length - 1; i++) {
            final Object next = cur.get(segs[i]);
            if(!(next instanceof Map<?, ?>)) {
                return false;
            }

            cur = (Map<String, Object>)next;
        }

        return cur.containsKey(segs[segs.length - 1]);
    }

    @SuppressWarnings("unchecked")
    private static void setByPath(
            final Map<String, Object> tree,
            final String dottedPath,
            final Object leaf
    ) {
        Map<String, Object> cur = tree;
        final String[] segs = dottedPath.split("\\.");
        for(int i = 0; i < segs.length - 1; i++) {
            cur = (Map<String, Object>)cur.computeIfAbsent(segs[i], k -> new LinkedHashMap<String, Object>());
        }
        cur.put(segs[segs.length - 1], leaf);
    }

    // Migration
    //--------------------------------------------------

    private static int readVersion(
            final Map<String, Object> tree,
            final int fallback
    ) {
        final Object v = tree.get("__version");
        if(v instanceof final Number n) {
            return n.intValue();
        }

        return fallback;
    }

    //==================================================
    // Fields
    //==================================================

    private final Schema schema;
    private final ConfigModel<S> model;
    private final ConfigIO io;
    private final Corrector corrector;
    private final Path file;
    private final MigrationSpec migrations;
    private final AdapterEventBus eventBus;
    private final List<ReloadListener<S>> reloadListeners;
    private final Map<String, Origin> origins;

    /**
     * Guards {@link #state}, {@link #origins}, and all read-modify-write flows
     * against concurrent mutation. Reads that only touch {@link #state} via its
     * volatile ref (e.g. {@link #get()}) don't acquire this lock.
     */
    private final Object lock = new Object();

    private volatile S state;

    //==================================================
    // Constructors
    //==================================================

    AdapterConfigManager(
            final Schema schema,
            final ConfigModel<S> model,
            final ConfigIO io,
            final Corrector corrector,
            final Path file,
            final MigrationSpec migrations
    ) {
        super();

        this.schema = schema;
        this.model = model;
        this.io = io;
        this.corrector = corrector;
        this.file = file;
        this.migrations = migrations;
        this.eventBus = new AdapterEventBus();
        this.reloadListeners = new CopyOnWriteArrayList<>();
        this.origins = new java.util.concurrent.ConcurrentHashMap<>();
        this.state = model.newState();

        for(final String p : schema.paths()) {
            this.origins.put(p, Origin.DEFAULT);
        }
    }

    //==================================================
    // Methods
    //==================================================

    // ConfigManager
    //--------------------------------------------------

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public S get() {
        return state;
    }

    @Override
    public Path file() {
        return file;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }

    @Override
    public void set(
            final String dottedPath,
            final Object value
    ) {
        final Optional<SchemaEntry> entry = schema.find(dottedPath);
        if(entry.isEmpty()) {
            return;
        }

        final Object oldValue;
        synchronized(lock) {
            oldValue = entry.get().readFrom(state);
            entry.get().writeTo(state, value);
            origins.put(dottedPath, Origin.LOCAL_EDIT);
        }
        // Dispatch outside the lock: listeners run synchronously on the caller
        // thread per spec §10.1 and MUST NOT block — but keeping them out of
        // the critical section avoids holding the lock across arbitrary code.
        eventBus.dispatch(new ChangeEvent(dottedPath, oldValue, value, Origin.LOCAL_EDIT, Instant.now()));
    }

    @Override
    public LoadResult load() throws IOException {
        final Optional<Map<String, Object>> parsed = io.read(file, schema);

        final S previous;
        final S newState;
        final Optional<MigrationReport> migrationReport;
        final List<Correction> corrections;

        synchronized(lock) {
            Optional<MigrationReport> report0 = Optional.empty();
            final Map<String, Object> tree = parsed.orElseGet(LinkedHashMap::new);

            if(parsed.isPresent() && !migrations.steps().isEmpty()) {
                final int fromVersion = readVersion(tree, schema.version());
                final MigrationReport report = applyMigrations(tree, fromVersion);
                if(!report.steps().isEmpty()) {
                    report0 = Optional.of(report);
                }
            }
            migrationReport = report0;

            newState = model.newState();
            if(parsed.isPresent()) {
                hydrate(newState, tree);
            }

            corrections = corrector.correct(schema, newState);

            previous = state;
            state = newState;

            for(final String p : schema.paths()) {
                origins.put(p, parsed.isPresent() ? Origin.LOCAL_EDIT : Origin.DEFAULT);
            }
        }

        for(final ReloadListener<S> l : reloadListeners) {
            try {
                l.onReload(previous, newState);
            } catch(final RuntimeException ignored) {
            }
        }

        // First-boot materialization: if the file didn't exist we're running on
        // pure defaults — write them to disk so the user has something to look
        // at (or hand-edit) without having to open the GUI first. Matches
        // AutoConfig / Cloth / ForgeConfigSpec behavior.
        if(parsed.isEmpty()) {
            save();
        }

        return new LoadResult(snapshot(), corrections, migrationReport);
    }

    @Override
    public void save() throws IOException {
        final Map<String, Object> tree;
        synchronized(lock) {
            tree = extractTree(state);
            tree.put("__version", schema.version());
        }
        // I/O outside the lock — no need to block concurrent reads during disk
        // write. The captured `tree` is a fresh nested map so it's immune to
        // further set()s.
        io.write(file, tree, schema);
    }

    @Override
    public void addReloadListener(final ReloadListener<S> listener) {
        reloadListeners.add(listener);
    }

    @Override
    public Origin originOf(final String dottedPath) {
        return origins.getOrDefault(dottedPath, Origin.DEFAULT);
    }

    @Override
    public void markOrigins(
            final Collection<String> paths,
            final Origin origin
    ) {
        for(final String p : paths) {
            origins.put(p, origin);
        }
    }

    @Override
    public ConfigSnapshot snapshot() {
        // Deep-copy under the lock — walking every schema path off a torn state
        // gives an inconsistent snapshot.
        final Object frozen;
        synchronized(lock) {
            frozen = deepCopyState();
        }
        return new AdapterConfigSnapshot(Instant.now(), schema, frozen);
    }

    // Accessors used by AdapterConfigValue
    //--------------------------------------------------

    Object rawAt(final String dottedPath) {
        return schema.find(dottedPath).map(e -> e.readFrom(state)).orElse(null);
    }

    // Tree hydration + extraction
    //--------------------------------------------------

    /**
     * Copy leaf values from a nested {@code tree} into the state instance.
     */
    private void hydrate(
            final S target,
            final Map<String, Object> tree
    ) {
        for(final String path : schema.paths()) {
            final Object leaf = getByPath(tree, path);
            if(leaf == null && !containsPath(tree, path)) {
                continue;
            }

            final Optional<SchemaEntry> entry = schema.find(path);
            if(entry.isEmpty()) {
                continue;
            }

            try {
                entry.get().writeTo(target, leaf);
            } catch(final RuntimeException ignored) {
                // Leaf-write errors are handled by the Corrector on the following pass.
            }
        }
    }

    /**
     * Build a nested {@code Map<String, Object>} from the current state instance.
     */
    private Map<String, Object> extractTree(final S source) {
        final Map<String, Object> tree = new LinkedHashMap<>();
        for(final String path : schema.paths()) {
            final Optional<SchemaEntry> entry = schema.find(path);
            if(entry.isEmpty()) {
                continue;
            }

            final Object leaf = entry.get().readFrom(source);
            setByPath(tree, path, leaf);
        }

        return tree;
    }

    // Migration
    //--------------------------------------------------

    private MigrationReport applyMigrations(
            final Map<String, Object> tree,
            final int fromVersion
    ) {
        final List<MigrationReport.AppliedStep> applied = new ArrayList<>();
        int current = fromVersion;
        for(final MigrationStep step : migrations.steps()) {
            if(step.fromVersion() != current) {
                continue;
            }

            int opCount = 0;
            for(final MigrationOp op : step.ops()) {
                op.apply(tree);
                opCount++;
            }

            applied.add(new MigrationReport.AppliedStep(step.fromVersion(), step.toVersion(), opCount));

            current = step.toVersion();
            if(current == schema.version()) {
                break;
            }
        }

        return new MigrationReport(fromVersion, current, Collections.unmodifiableList(applied));
    }

    // Snapshot deep copy
    //--------------------------------------------------

    /**
     * For POJO state: deep-copy is a shallow field-mirror (fresh instance from
     * {@code newState()}, copy every entry). For Map state: {@code new LinkedHashMap<>}
     * of the current map.
     */
    private Object deepCopyState() {
        final S fresh = model.newState();
        for(final String path : schema.paths()) {
            final Optional<SchemaEntry> entry = schema.find(path);
            if(entry.isEmpty()) {
                continue;
            }

            try {
                entry.get().writeTo(fresh, entry.get().readFrom(state));
            } catch(final RuntimeException ignored) {
            }
        }

        return fresh;
    }

}
