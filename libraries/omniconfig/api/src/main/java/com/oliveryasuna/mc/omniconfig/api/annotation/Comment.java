package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Attaches a human-readable comment to a config entry or category.
 * <p>
 * Written to formats that support comments (TOML, HOCON, JSON5) and surfaced as
 * the entry description in the generated GUI. Each array element is one line.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Comment {

    //==================================================
    // Properties
    //==================================================

    /**
     * Comment lines, in order.
     */
    String[] value();

}
