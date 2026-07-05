package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Constraints a numeric entry to an inclusive {@code [min, max]} range.
 * <p>
 * Applies to {@code byte/short/int/long/float/double} fields and their boxed
 * forms.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {

    //==================================================
    // Properties
    //==================================================

    /**
     * Inclusive lower bound.
     */
    double min() default Double.NEGATIVE_INFINITY;

    /**
     * Inclusive upper bound.
     */
    double max() default Double.POSITIVE_INFINITY;


}
