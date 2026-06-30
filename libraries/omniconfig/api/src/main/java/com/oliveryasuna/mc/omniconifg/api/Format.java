package com.oliveryasuna.mc.omniconifg.api;

/**
 * On-disk serialization format for a config.
 */
public enum Format {

    //==================================================
    // Values
    //==================================================

    /**
     * TOML 1.0/1.1.
     * <p>
     * Comment-preserving. The default and recommended format.
     */
    TOML,

    /**
     * Plain JSON.
     * <p>
     * No comment support.
     */
    JSON,

    /**
     * JSON5.
     * <p>
     * Relaxed JSON with comments and trailing commas.
     */
    JSON5

}
