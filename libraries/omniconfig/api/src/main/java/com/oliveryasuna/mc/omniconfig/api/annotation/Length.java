package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Constraints the length of a {@link String} or the size of a
 * {@link java.util.List}/{@link java.util.Map} to an inclusive
 * {@code [min, max]} range.
 * <p>
 * Violations are corrected-and-logged on load (over-long collections are
 * truncated; strings failing the bounds are reset to default).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Length {

    //==================================================
    // Properties
    //==================================================

    /**
     * Inclusive minimum length / size.
     */
    int min() default 0;

    /**
     * Inclusive maximum length / size.
     */
    int max() default Integer.MAX_VALUE;

}
