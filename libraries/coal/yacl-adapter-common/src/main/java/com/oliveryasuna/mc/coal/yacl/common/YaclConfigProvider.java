package com.oliveryasuna.mc.coal.yacl.common;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.io.ConfigIO;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.schema.ConfigModel;
import com.oliveryasuna.mc.coal.api.schema.Schema;
import com.oliveryasuna.mc.coal.api.schema.SchemaReader;
import com.oliveryasuna.mc.coal.api.spi.Capability;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.validation.Corrector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The COAL {@link ConfigProvider} implementation. Owns a
 * {@link SchemaReader} + {@link ConfigIO} + {@link Corrector} and dispatches
 * registrations to per-config {@link YaclConfigManager}s.
 * <p>
 * Format substitution: TOML/JSON5 requests silently fall back to JSON with a
 * one-shot {@code WARN} per §4.4 — v1 only supports {@link Format#JSON}.
 */
final class YaclConfigProvider implements ConfigProvider {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("coal-yacl-adapter");

    //==================================================
    // Fields
    //==================================================

    private final Platform platform;
    private final AnnotationSchemaReader schemaReader;
    private final JsonConfigIO defaultIO;
    private final DefaultCorrector corrector;
    private final Map<String, YaclConfigHandle<?>> handles;

    //==================================================
    // Constructors
    //==================================================

    YaclConfigProvider(final Platform platform) {
        super();

        this.platform = platform;
        this.schemaReader = new AnnotationSchemaReader();
        this.defaultIO = new JsonConfigIO();
        this.corrector = new DefaultCorrector();
        // ConcurrentHashMap so registeredConfigIds() / getById() stay
        // consistent under concurrent registration. Insertion order isn't part
        // of the spec — a Concurrent LinkedHashMap analogue isn't warranted.
        this.handles = new java.util.concurrent.ConcurrentHashMap<>();
    }

    //==================================================
    // Methods
    //==================================================

    // ConfigProvider
    //--------------------------------------------------

    @Override
    public String name() {
        return "coal-yacl-adapter";
    }

    @Override
    public <S> ConfigHandle<S> register(
            final Class<S> type,
            final MigrationSpec migrations
    ) {
        final ConfigModel<S> model = schemaReader.read(type);

        return install(model, migrations);
    }

    @Override
    public ConfigHandle<Map<String, Object>> register(
            final ConfigSpec spec,
            final MigrationSpec migrations
    ) {
        final ConfigModel<Map<String, Object>> model = schemaReader.read(spec);

        return install(model, migrations);
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public SchemaReader schemaReader() {
        return schemaReader;
    }

    @Override
    public Corrector corrector() {
        return corrector;
    }

    @Override
    public ConfigIO defaultIO() {
        return defaultIO;
    }

    @Override
    public Set<String> registeredConfigIds() {
        return Collections.unmodifiableSet(handles.keySet());
    }

    @Override
    public Optional<ConfigHandle<?>> getById(final String id) {
        return Optional.ofNullable(handles.get(id));
    }

    @Override
    public boolean supports(final Capability capability) {
        return switch(capability) {
            case MIGRATION, VALIDATION, GUI_DELEGATION -> true;
            case SYNC, FILE_WATCH, JSON5, CUSTOM_FORMATS -> false;
        };
    }

    // Registration
    //--------------------------------------------------

    private <S> ConfigHandle<S> install(
            final ConfigModel<S> model,
            final MigrationSpec migrations
    ) {
        final Schema schema = model.schema();
        final Path file = resolveFile(schema);
        final YaclConfigManager<S> manager = new YaclConfigManager<>(schema, model, defaultIO, corrector, file, migrations);
        final YaclConfigHandle<S> handle = new YaclConfigHandle<>(manager);

        // Atomic check-and-put: if two threads race on the same id, exactly one
        // wins and the loser gets IllegalArgumentException. The manager built
        // by the loser is discarded — no side effects yet (load() hasn't run).
        final YaclConfigHandle<?> existing = handles.putIfAbsent(schema.id(), handle);
        if(existing != null) {
            throw new IllegalArgumentException("coal-yacl-adapter: config id '" + schema.id() + "' already registered");
        }

        try {
            manager.load();
        } catch(final IOException e) {
            LOGGER.warn("[{}] initial load failed for '{}': {}", name(), schema.id(), e.getMessage());
        }

        LOGGER.info("[{}] registered '{}' -> {}", name(), schema.id(), file);

        return handle;
    }

    private Path resolveFile(final Schema schema) {
        final Path configDir = platform.configDir();
        final Format format = schema.format();
        if(!Format.JSON.equals(format)) {
            LOGGER.warn("[{}] config '{}' requested format '{}' — this adapter only supports JSON in v1; falling back", name(), schema.id(), format.id());
        }

        return configDir.resolve(schema.name() + ".json");
    }

}
