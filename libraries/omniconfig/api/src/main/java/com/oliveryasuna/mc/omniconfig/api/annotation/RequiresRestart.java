package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Convenience marker equivalent to {@code @Reload(Reload.Tier.RESTART)}.
 * <p>
 * If both are present on the same element, {@link Reload} wins.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RequiresRestart {

}
