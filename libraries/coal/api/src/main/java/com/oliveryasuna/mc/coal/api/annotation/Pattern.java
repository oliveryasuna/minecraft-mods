package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Requires a {@link String} entry to fully match a
 * {@link java.util.regex.Pattern}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Pattern {

    //==================================================
    // Properties
    //==================================================

    /**
     * A {@link java.util.regex.Pattern}-compatible regular expression.
     */
    String value();

}
