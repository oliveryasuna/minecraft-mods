package com.oliveryasuna.mc.rubric.api.annotation;

import com.oliveryasuna.mc.rubric.api.Format;

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
     * <p>
     * <b>ModMenu auto-discovery</b>: when the Fabric integration's built-in
     * {@code ModMenuIntegration} is on the classpath, it surfaces every config
     * registered via {@code RubricGui.registerScreen(...)} as a screen factory
     * keyed by this {@code id}. ModMenu only renders factories whose key
     * matches a <i>loaded</i> mod's {@code fabric.mod.json#id}, so for
     * auto-discovery to work this value must equal the owning mod's Fabric
     * mod ID. Mods that own more than one config should call the explicit
     * {@code RubricGui.registerScreen(modId, manager)} overload, which groups
     * them under one mod-menu entry and shows a chooser sub-screen.
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
