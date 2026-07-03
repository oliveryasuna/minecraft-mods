package com.oliveryasuna.mc.rubric.loader;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.loader.config.RubricConfig;

import java.util.function.Supplier;

/**
 * Static access point for Rubric's own self-{@link RubricConfig}. Loader entry
 * points call {@link #configSupplier(Supplier)} once the self-config manager
 * has loaded, then shared screen code reads live state via {@link #config()}
 * without referring to any loader-specific mod class.
 * <p>
 * Defaults to a fresh {@link RubricConfig} so callers that fire before
 * initialization still see usable defaults.
 */
public final class RubricSelf {

    //==================================================
    // Static fields
    //==================================================

    private static volatile Supplier<RubricConfig> supplier = RubricConfig::new;

    //==================================================
    // Static methods
    //==================================================

    public static void configSupplier(final Supplier<RubricConfig> newSupplier) {
        if(newSupplier == null) {
            supplier = RubricConfig::new;
        } else {
            supplier = newSupplier;
        }
    }

    public static RubricConfig config() {
        return supplier.get();
    }

    //==================================================
    // Constructors
    //==================================================

    private RubricSelf() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
