package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Rejects a missing/null value for this entry.
 * <p>
 * On load, a null value is reset to the field's default (correct-and-log),
 * rather than being accepted.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotNull {

}
