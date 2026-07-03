package com.oliveryasuna.mc.coal.api;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;

import java.util.Map;

public final class Coal {

    //==================================================
    //  Static methods
    //==================================================

    // Bootstrap
    //--------------------------------------------------

    public static void bootstrap() {
        // TODO: Implement.
    }

    public static void bootstrap(final ConfigProvider provider) {
        // TODO: Implement.
    }

    // Registration
    //--------------------------------------------------

    // Annotated POJO -> managed handle
    //

    public static <S> ConfigHandle<S> register(final Class<S> type) {
        // TODO: Implement.
    }

    public static <S> ConfigHandle<S> register(
            final Class<S> type,
            final MigrationSpec migrations
    ) {
        // TODO: Implement.
    }

    // Dynamically-shaped configs
    //

    public static ConfigHandle<Map<String, Object>> register(final ConfigSpec spec) {
        // TODO: Implement.
    }

    public static ConfigHandle<Map<String, Object>> register(
            final ConfigSpec spec,
            final MigrationSpec migrations
    ) {
        // TODO: Implement.
    }

    // Access to the installed provider (rarely needed by mods)
    //--------------------------------------------------

    public static ConfigProvider getProvider() {
        // TODO: Implement.
    }

    public static boolean isBootstrapped() {
        // TODO: Implement.
    }

    //==================================================
    // Constructors
    //==================================================

    private Coal() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
