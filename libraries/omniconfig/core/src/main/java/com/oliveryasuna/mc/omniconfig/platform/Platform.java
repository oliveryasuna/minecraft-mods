package com.oliveryasuna.mc.omniconfig.platform;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * Loader-supplied environment services.
 * <p>
 * Implemented by the Fabric and NeoForge adapters; consumed by the lifecycle
 * and reload layers. Deliberately free of Minecraft types so {@code core} stays
 * loader-independent.
 */
public interface Platform {

    //==================================================
    // Methods
    //==================================================

    /**
     * Base directory for config files (e.g., {@code .minecraft/config}.
     *
     * @return The config directory.
     */
    Path configDir();

    /**
     * Executor that runs tasks on the game/main thread.
     *
     * @return The executor.
     */
    Executor mainThreadExecutor();

    /**
     * A logger for the given name.
     *
     * @param name The logger name.
     * @return The logger.
     */
    Logger logger(String name);

    /**
     * Whether this is a client or dedicated-server environment.
     *
     * @return The environment.
     */
    Environment environment();

    //==================================================
    // Nested
    //==================================================

    enum Environment {

        //==================================================
        // Values
        //==================================================

        CLIENT,

        SERVER

    }

}
