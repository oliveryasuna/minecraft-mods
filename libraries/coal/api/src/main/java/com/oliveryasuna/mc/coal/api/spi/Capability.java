package com.oliveryasuna.mc.coal.api.spi;

public enum Capability {

    //==================================================
    // Values
    //==================================================

    /**
     * Implements {@code coal-api-sync} SPI.
     */
    SYNC,

    /**
     * Executes migrations on load.
     */
    MIGRATION,

    /**
     * Reloads when file changes on disk.
     */
    FILE_WATCH,

    /**
     * Runs validators + corrections.
     */
    VALIDATION,

    /**
     * Implements screen provider selection (via {@code coal-api-gui}).
     */
    GUI_DELEGATION,

    /**
     * Handles {@link com.oliveryasuna.mc.coal.api.Format#JSON5}.
     */
    JSON5,

    /**
     * {@link com.oliveryasuna.mc.coal.api.Format.Registry} accepts arbitrary
     * {@code Format.of(id)} at runtime.
     */
    CUSTOM_FORMATS

}
