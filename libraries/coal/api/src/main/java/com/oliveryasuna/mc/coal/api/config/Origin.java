package com.oliveryasuna.mc.coal.api.config;

public enum Origin {

    //==================================================
    // Values
    //==================================================

    /**
     * Never touched.
     */
    DEFAULT,

    /**
     * Set locally or read from disk.
     */
    LOCAL_EDIT,

    /**
     * Pushed by a server sync (from {@code coal-api-sync}).
     */
    FROM_REMOTE

}
