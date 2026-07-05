package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Attaches a human-readable comment to a config entry or category.
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
