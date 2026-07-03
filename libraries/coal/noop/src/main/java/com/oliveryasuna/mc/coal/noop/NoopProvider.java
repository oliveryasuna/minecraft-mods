package com.oliveryasuna.mc.coal.noop;

import com.oliveryasuna.mc.coal.api.config.*;
import com.oliveryasuna.mc.coal.api.event.ChangeEvent;
import com.oliveryasuna.mc.coal.api.event.ChangeListener;
import com.oliveryasuna.mc.coal.api.event.EventBus;
import com.oliveryasuna.mc.coal.api.event.ReloadListener;
import com.oliveryasuna.mc.coal.api.io.ConfigIO;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.schema.Schema;
import com.oliveryasuna.mc.coal.api.schema.SchemaReader;
import com.oliveryasuna.mc.coal.api.spi.Capability;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.validation.Corrector;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/**
 * The no-op {@link ConfigProvider}.
 * <p>
 * Deep no-op: every returned collaborator is itself a no-op. Mods can freely
 * chain {@code handle.manager().events().subscribe(...)} without crashing.
 * Reads of {@link ConfigHandle#get()} return a fresh instance populated with
 * either the POJO's default field values (for annotation-driven configs) or the
 * {@code ConfigSpec}'s declared defaults (for builder-driven configs). Writes
 * are silently discarded.
 */
public final class NoopProvider implements ConfigProvider {

    //==================================================
    // Fields
    //==================================================

    private final Platform platform;

    //==================================================
    // Constructors
    //==================================================

    public NoopProvider(final Platform platform) {
        super();

        this.platform = platform;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public String name() {
        return "coal-noop";
    }

    @Override
    public <S> ConfigHandle<S> register(
            final Class<S> type,
            final MigrationSpec migrations
    ) {
        final S instance;
        try {
            instance = type.getDeclaredConstructor().newInstance();
        } catch(final ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "coal-noop cannot instantiate " + type.getName()
                    + " — needs a public no-arg constructor",
                    e
            );
        }

        return new NoopHandle<>(instance);
    }

    @Override
    public ConfigHandle<Map<String, Object>> register(
            final ConfigSpec spec,
            final MigrationSpec migrations
    ) {
        final Map<String, Object> instance = new LinkedHashMap<>();
        for(final ConfigSpec.EntrySpec entry : spec.getEntries()) {
            final String path = entry.categoryPath().isEmpty()
                    ? entry.key()
                    : entry.categoryPath() + "." + entry.key();
            instance.put(path, entry.defaultValue());
        }

        return new NoopHandle<>(instance);
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public SchemaReader schemaReader() {
        throw new UnsupportedOperationException("coal-noop does not provide a SchemaReader");
    }

    @Override
    public Corrector corrector() {
        throw new UnsupportedOperationException("coal-noop does not provide a Corrector");
    }

    @Override
    public ConfigIO defaultIO() {
        throw new UnsupportedOperationException("coal-noop does not provide a ConfigIO");
    }

    @Override
    public Set<String> registeredConfigIds() {
        return Collections.emptySet();
    }

    @Override
    public Optional<ConfigHandle<?>> getById(final String id) {
        return Optional.empty();
    }

    @Override
    public boolean supports(final Capability capability) {
        return false;
    }

    //==================================================
    // Nested
    //==================================================

    // Public for tests.
    public static final class NoopHandle<S> implements ConfigHandle<S> {

        //==================================================
        // Fields
        //==================================================

        private final S instance;
        private final NoopManager<S> manager;
        private final NoopSnapshot snapshot;

        //==================================================
        // Constructors
        //==================================================

        NoopHandle(final S instance) {
            super();

            this.instance = instance;
            this.snapshot = new NoopSnapshot();
            this.manager = new NoopManager<>(this.instance, this.snapshot);
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public S get() {
            return instance;
        }

        @Override
        public void set(final String dottedPath, final Object value) {
            // NO-OP
        }

        @Override
        public void reload() {
            // NO-OP
        }

        @Override
        public void save() {
            // NO-OP
        }

        @Override
        public ConfigManager<S> manager() {
            return manager;
        }

        @Override
        public ConfigSnapshot snapshot() {
            return snapshot;
        }

        @Override
        public <T> ConfigValue<T> value(final String dottedPath, final Class<T> type) {
            return new NoopConfigValue<>(dottedPath, type);
        }

    }

    private static final class NoopManager<S> implements ConfigManager<S> {

        //==================================================
        // Fields
        //==================================================

        private final S instance;
        private final ConfigSnapshot snapshot;
        private final NoopEventBus events;

        //==================================================
        // Constructors
        //==================================================

        NoopManager(
                final S instance,
                final ConfigSnapshot snapshot
        ) {
            super();

            this.instance = instance;
            this.snapshot = snapshot;
            this.events = new NoopEventBus();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public Schema schema() {
            throw new UnsupportedOperationException("coal-noop does not expose a Schema");
        }

        @Override
        public S get() {
            return instance;
        }

        @Override
        public void set(final String dottedPath, final Object value) {
            // NO-OP
        }

        @Override
        public LoadResult load() {
            return new LoadResult(snapshot, Collections.emptyList(), Optional.empty());
        }

        @Override
        public void save() {
            // NO-OP
        }

        @Override
        public Path file() {
            throw new UnsupportedOperationException("coal-noop does not back a file");
        }

        @Override
        public EventBus events() {
            return events;
        }

        @Override
        public void addReloadListener(final ReloadListener<S> listener) {
            // NO-OP
        }

        @Override
        public Origin originOf(final String dottedPath) {
            return Origin.DEFAULT;
        }

        @Override
        public void markOrigins(final Collection<String> paths, final Origin origin) {
            // NO-OP
        }

        @Override
        public ConfigSnapshot snapshot() {
            return snapshot;
        }

    }

    private static final class NoopSnapshot implements ConfigSnapshot {

        //==================================================
        // Fields
        //==================================================

        private final Instant capturedAt;

        //==================================================
        // Constructors
        //==================================================

        NoopSnapshot() {
            super();

            this.capturedAt = Instant.EPOCH;
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public Instant capturedAt() {
            return capturedAt;
        }

        @Override
        public Schema schema() {
            throw new UnsupportedOperationException("coal-noop does not expose a Schema");
        }

        @Override
        public <T> Optional<T> get(final String dottedPath, final Class<T> type) {
            return Optional.empty();
        }

        @Override
        public Object getRaw(final String dottedPath) {
            return null;
        }

        @Override
        public Set<String> paths() {
            return Collections.emptySet();
        }

        @Override
        public boolean isPresent(final String dottedPath) {
            return false;
        }

    }

    private static final class NoopEventBus implements EventBus {

        //==================================================
        // Static fields
        //==================================================

        private static final Registration NOOP_REGISTRATION = () -> { /* NO-OP */ };

        //==================================================
        // Constructors
        //==================================================

        NoopEventBus() {
            super();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public Registration subscribe(final ChangeListener listener) {
            return NOOP_REGISTRATION;
        }

        @Override
        public Registration subscribe(final String pathPrefix, final ChangeListener listener) {
            return NOOP_REGISTRATION;
        }

        @Override
        public void dispatch(final ChangeEvent event) {
            // NO-OP
        }

    }

    private static final class NoopConfigValue<T> implements ConfigValue<T> {

        //==================================================
        // Fields
        //==================================================

        private final String path;
        private final Class<T> type;

        //==================================================
        // Constructors
        //==================================================

        NoopConfigValue(
                final String path,
                final Class<T> type
        ) {
            super();

            this.path = path;
            this.type = type;
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public T get() {
            return null;
        }

        @Override
        public void set(final T value) {
            // NO-OP
        }

        @Override
        public void onChange(final Consumer<T> listener) {
            // NO-OP
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public Class<T> type() {
            return type;
        }

    }

}
