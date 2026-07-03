package com.oliveryasuna.mc.coal.api.validation;

import com.oliveryasuna.mc.coal.api.schema.SchemaEntry;

@FunctionalInterface
public interface Validator<T> {

    //==================================================
    // Methods
    //==================================================

    ValidationResult validate(
            T value,
            ValidationContext ctx
    );

    //==================================================
    // Nested
    //==================================================

    interface ValidationContext {

        //==================================================
        // Methods
        //==================================================

        SchemaEntry entry();

        String path();

    }

}
