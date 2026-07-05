package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Groups entries under a named section.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Category {

    //==================================================
    // Properties
    //==================================================

    /**
     * Section / group name.
     */
    String value();

}
