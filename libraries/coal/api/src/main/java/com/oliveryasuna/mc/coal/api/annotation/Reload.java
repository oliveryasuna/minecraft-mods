package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Declares when a changed value takes effect.
 * <p>
 * On a type, sets the default tier for every entry in that config; on a field,
 * overrides it for that entry.
 * <p>
 * When unspecified, the effective tier is {@link Tier#WORLD}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Reload {

    //==================================================
    // Properties
    //==================================================

    /**
     * Reload tier.
     * <p>
     * Defaults to {@link Tier#WORLD}.
     */
    Tier value() default Tier.WORLD;

    //==================================================
    // Nested
    //==================================================

    /**
     * When a changed value is applied
     */
    enum Tier {

        //==================================================
        // Values
        //==================================================

        /**
         * Applied immediately on change.
         * <p>
         * Intended for client/cosmetic values.
         */
        LIVE,

        /**
         * Applied on the next world (re)load.
         * <p>
         * The default.
         */
        WORLD,

        /**
         * Applied only after a game restart.
         */
        RESTART

    }

}
