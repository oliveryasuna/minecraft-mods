package com.oliveryasuna.mc.rubric.api;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.io.ConfigIO;
import com.oliveryasuna.mc.rubric.migration.MigrationRegistry;
import com.oliveryasuna.mc.rubric.platform.Platform;
import com.oliveryasuna.mc.rubric.schema.ConfigModel;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
import com.oliveryasuna.mc.rubric.value.ValueCodec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Static entry point.
 * <p>
 * The loader calls {@link #bootstrap} once with its {@link ConfigIO} and
 * {@link Platform}; mods then call {@link #register} per config class.
 * Registration reads the schema, performs the initial load
 * (migrating/seeding/correcting as needed), and returns a {@link ConfigHandle}.
 */
public final class Rubric {

    //==================================================
    // Static fields
    //==================================================

    private static volatile Bootstrap runtime;

    //==================================================
    // Static methods
    //==================================================

    public static void bootstrap(
            final ConfigIO io,
            final Platform platform,
            final CodecRegistry codecs
    ) {
        runtime = new Bootstrap(io, platform, codecs);
    }

    public static void bootstrap(
            final ConfigIO io,
            final Platform platform
    ) {
        bootstrap(io, platform, new CodecRegistry());
    }

    public static void registerCodec(
            final Class<?> type,
            final ValueCodec<?> codec
    ) {
        requireRuntime().codecs().registerCustom(type, codec);
    }

    public static <T> ConfigHandle<T> register(
            final Class<T> type,
            final MigrationRegistry migrations
    ) {
        final Bootstrap r = requireRuntime();
        final ConfigManager<T> manager = new ConfigManager<>(type, r.io(), r.platform(), r.codecs(), migrations);
        try {
            manager.load();
        } catch(final IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ConfigHandle<>(manager);
    }

    public static <T> ConfigHandle<T> register(final Class<T> type) {
        return register(type, null);
    }

    public static ConfigHandle<Map<String, Object>> register(
            final ConfigSpec spec,
            final MigrationRegistry migrations
    ) {
        final Bootstrap r = requireRuntime();
        final ConfigModel<Map<String, Object>> model = spec.toModel();
        final ConfigManager<java.util.Map<String, Object>> manager = new ConfigManager<>(model, r.io(), r.platform(), r.codecs(), migrations);
        try {
            manager.load();
        } catch(final IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ConfigHandle<>(manager);

    }

    public static ConfigHandle<Map<String, Object>> register(final ConfigSpec spec) {
        return register(spec, null);
    }

    private static Bootstrap requireRuntime() {
        final Bootstrap r = runtime;
        if(r == null) {
            throw new IllegalStateException("Rubric.bootstrap(...) must be called by the loader before register(...)");
        }
        return r;
    }

    //==================================================
    // Constructors
    //==================================================

    private Rubric() {
        super();

        throw new UnsupportedInstantiationException();
    }

    //==================================================
    // Nested
    //==================================================

    private record Bootstrap(
            ConfigIO io,
            Platform platform,
            CodecRegistry codecs
    ) {

    }


}
