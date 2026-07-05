package com.oliveryasuna.mc.coal.api.validation;

import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.util.List;

@FunctionalInterface
public interface Corrector {

    //==================================================
    // Methods
    //==================================================

    List<Correction> correct(
            Schema schema,
            Object instance
    );

}
