package com.oliveryasuna.mc.coal.api.annotation;

import java.lang.annotation.*;

/**
 * Rejects a missing/null value for this entry.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotNull {

}
