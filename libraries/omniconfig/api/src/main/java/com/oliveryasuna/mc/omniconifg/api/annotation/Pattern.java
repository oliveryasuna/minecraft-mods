package com.oliveryasuna.mc.omniconifg.api.annotation;

import java.lang.annotation.*;

/**
 * Requires a {@link String} entry to fully match a
 * {@link java.util.regex.Pattern}.
 * <p>
 * Non-matching values are reset to default (correct-and-log) on load.
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
