package com.oliveryasuna.mc.coal.api;

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
 * Package-private inline fallback {@link ConfigProvider} used by
 * {@link Coal} when nothing is discovered via {@link java.util.ServiceLoader}.
 * <p>
 * Behavior mirrors {@code coal-noop}'s {@code NoopProvider} — deep no-op, safe
 * to chain through {@code handle.manager().events().subscribe(...)} without
 * crashing. Reads of {@link ConfigHandle#get()} return a fresh POJO with
 * default field values (or a defaults-populated {@code Map} for builder
 * configs). Writes are discarded.
 * <p>
 * Consumers who want no-op behavior as a deliberate, published choice (rather
 * than a fallback fired because nothing was found) should add {@code coal-noop}
 * to their build.
 */
final class InlineFallbackProvider implements ConfigProvider {

    //==================================================
    // Fields
    //==================================================

    private final Platform platform;

    //==================================================
    // Constructors
    //==================================================

    InlineFallbackProvider(final Platform platform) {
        super();

        this.platform = platform;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public String name() {
        return "coal-api-inline-fallback";
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
                    "coal-api inline fallback cannot instantiate " + type.getName()
                    + " — needs a public no-arg constructor",
                    e
            );
        }

        return new FallbackHandle<>(instance);
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

        return new FallbackHandle<>(instance);
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public SchemaReader schemaReader() {
        throw new UnsupportedOperationException("coal-api inline fallback does not provide a SchemaReader");
    }

    @Override
    public Corrector corrector() {
        throw new UnsupportedOperationException("coal-api inline fallback does not provide a Corrector");
    }

    @Override
    public ConfigIO defaultIO() {
        throw new UnsupportedOperationException("coal-api inline fallback does not provide a ConfigIO");
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

    private static final class FallbackHandle<S> implements ConfigHandle<S> {

        //==================================================
        // Fields
        //==================================================

        private final S instance;
        private final FallbackManager<S> manager;
        private final FallbackSnapshot snapshot;

        //==================================================
        // Constructors
        //==================================================

        FallbackHandle(final S instance) {
            super();

            this.instance = instance;
            this.snapshot = new FallbackSnapshot();
            this.manager = new FallbackManager<>(this.instance, this.snapshot);
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
            return new FallbackConfigValue<>(dottedPath, type);
        }

    }

    private static final class FallbackManager<S> implements ConfigManager<S> {

        //==================================================
        // Fields
        //==================================================

        private final S instance;
        private final ConfigSnapshot snapshot;
        private final FallbackEventBus events;

        //==================================================
        // Constructors
        //==================================================

        FallbackManager(
                final S instance,
                final ConfigSnapshot snapshot
        ) {
            super();

            this.instance = instance;
            this.snapshot = snapshot;
            this.events = new FallbackEventBus();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public Schema schema() {
            throw new UnsupportedOperationException("coal-api inline fallback does not expose a Schema");
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
            throw new UnsupportedOperationException("coal-api inline fallback does not back a file");
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

    private static final class FallbackSnapshot implements ConfigSnapshot {

        //==================================================
        // Fields
        //==================================================

        private final Instant capturedAt;

        //==================================================
        // Constructors
        //==================================================

        FallbackSnapshot() {
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
            throw new UnsupportedOperationException("coal-api inline fallback does not expose a Schema");
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

    private static final class FallbackEventBus implements EventBus {

        //==================================================
        // Static fields
        //==================================================

        private static final Registration NOOP_REGISTRATION = () -> { /* NO-OP */ };

        //==================================================
        // Constructors
        //==================================================

        FallbackEventBus() {
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

    private static final class FallbackConfigValue<T> implements ConfigValue<T> {

        //==================================================
        // Fields
        //==================================================

        private final String path;
        private final Class<T> type;

        //==================================================
        // Constructors
        //==================================================

        FallbackConfigValue(
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
