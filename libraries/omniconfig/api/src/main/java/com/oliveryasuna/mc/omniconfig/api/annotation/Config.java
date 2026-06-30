package com.oliveryasuna.mc.omniconfig.api.annotation;

import com.oliveryasuna.mc.omniconfig.api.Format;

import java.lang.annotation.*;

/**
 * Marks a class as the root of the configuration.
 * <p>
 * Exactly one {@link Config} type maps to exactly one config file.
 * <p>
 * Example:
 * <pre>{@code
 * @Config(id = "mymod", name = "config", format = Format.TOML, version = 3)
 * public final class MyModConfig { ... }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {

    //==================================================
    // Properties
    //==================================================

    /**
     * Owning mod ID / namespace.
     * <p>
     * Used for the config subdirectory.
     */
    String id();

    /**
     * Base file name, without extension.
     * <p>
     * Defaults to {@code "config"}.
     */
    String name() default "config";

    /**
     * On-disk format.
     * <p>
     * Defaults to {@link Format#TOML}.
     */
    Format format() default Format.TOML;

    /**
     * Current schema version.
     * <p>
     * When a loaded file declares a lower version, registered migration are
     * applied in order.
     * <p>
     * Defaults to {@code 1}.
     */
    int version() default 1;

}
