package com.oliveryasuna.mc.omniconifg.api.annotation;

import java.lang.annotation.*;

/**
 * Declares the synchronization scope of an entry.
 * <p>
 * On a type, sets the default scope for every entry; on a field, overrides it.
 * <p>
 * When unspecified, the effective scope is {@link Scope#CLIENT}: entries are
 * local and never transmitted unless the author opts in.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Sync {

    //==================================================
    // Properties
    //==================================================

    /**
     * Sync scope.
     * <p>
     * Defaults to {@link Scope#CLIENT}.
     */
    Scope value() default Scope.CLIENT;

    //==================================================
    // Nested
    //==================================================

    /**
     * Ownership and propagation of an entry.
     */
    enum Scope {

        //==================================================
        // Values
        //==================================================

        /**
         * Client-owned, local only, never synced.
         */
        CLIENT,

        /**
         * Server-authoritative.
         * <p>
         * Pushed to clients for the session: the client cannot override it and
         * it is not written to the client's disk.
         */
        SERVER,

        /**
         * Server-authoritative when connected; usable from local defaults in
         * singleplayer.
         * <p>
         * Enforced like {@link #SERVER} in multiplayer.
         */
        COMMON

    }

}
