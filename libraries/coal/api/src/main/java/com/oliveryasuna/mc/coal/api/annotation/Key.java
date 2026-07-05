package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Overrides the on-disk key for a field.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Key {

    //==================================================
    // Properties
    //==================================================

    String value();

}
