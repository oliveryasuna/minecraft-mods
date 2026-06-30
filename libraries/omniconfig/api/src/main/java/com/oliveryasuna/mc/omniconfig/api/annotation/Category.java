package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Groups entries under a named section (a sub-table in TOML, a nested object in
 * JSON) and a collapsible group in the GUI.
 * <p>
 * On a nested-object field or its type, overrides the section name (which
 * otherwise defaults to the field name). On a flat field, tags that field into
 * the named group without requiring a nested class.
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
