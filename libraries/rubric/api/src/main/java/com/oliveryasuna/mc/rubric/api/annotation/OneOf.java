package com.oliveryasuna.mc.rubric.api.annotation;

import java.lang.annotation.*;

/**
 * Restricts a {@link String} entry to one of an explicit allow-list.
 * <p>
 * Renders as a dropdown in the GUI. (Enum fields are constraints implicitly and
 * do not need this annotation)
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
