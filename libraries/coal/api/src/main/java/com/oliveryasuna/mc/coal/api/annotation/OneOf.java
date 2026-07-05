package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Restricts a {@link String} entry to one of an explicit allow-list.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneOf {

    //==================================================
    // Values
    //==================================================

    /**
     * Permitted values.
     */
    String[] value();

}
