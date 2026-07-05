package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Excludes an entry (or every entry in a category) from the generated GUI.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Hidden {

}
