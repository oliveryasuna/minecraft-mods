package com.oliveryasuna.mc.coal.api.annotation;


import java.lang.annotation.*;

/**
 * Marks a class as the root of the configuration.
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
     */
    String id();

    /**
     * Base file name, without extension.
     */
    String name();

    /**
     * On-disk format.
     */
    String format() default "toml";

    /**
     * Current schema version.
     */
    int version() default 1;

}
