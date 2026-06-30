package com.oliveryasuna.mc.omniconfig.api.annotation;

import java.lang.annotation.*;

/**
 * Overrides the on-disk key for a field.
 * <p>
 * By default the config key is the Java field name; {@link Key} decouples them
 * so a field can be renamed in Java without breaking existing config files, or
 * so the on-disk key can follow a different convention (e.g.,
 * {@code kebab-case} than the Java identifier.
 *
 * <pre>{@code
 * @Key("max-entities") int maxEntities = 64;  // stored as "max-entities"
 * }</pre>
 * <p>
 * Applies to value entries. Section (nested-object) names are controlled by
 * {@link Category}. A {@link Key} that collides with another key in the same
 * category is rejected at registration.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Key {

    //==================================================
    // Properties
    //==================================================

    String value();

}
