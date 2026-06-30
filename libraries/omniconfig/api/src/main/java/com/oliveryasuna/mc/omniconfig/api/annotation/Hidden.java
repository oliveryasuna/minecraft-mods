package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Excludes an entry (or every entry in a category) from the generated GUI.
 * <p>
 * The value is still serialized to and read from the file; it is only hidden
 * from the editing screen.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Hidden {

}
