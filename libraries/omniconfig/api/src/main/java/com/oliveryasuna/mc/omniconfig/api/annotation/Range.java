package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Constraints a numeric entry to an inclusive {@code [min, max]} range.
 * <p>
 * Applies to {@code byte/short/int/long/float/double} fields and their boxed
 * forms.
 * <p>
 * Out-of-range values are clamped (correct-and-log) on load. The bounds also
 * drive slider extents in the GUI.
 * <p>
 * Bounds are declared as {@code double}; for {@code long} fields larger than
 * {@code 2^53}, the bound may be represented imprecisely.
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
