package com.oliveryasuna.mc.rubric.schema;

/**
 * Couples a {@link Schema} with a factory for fresh default state.
 *
 * @param <S> The type of the state.
 */
public interface ConfigModel<S> {

    //==================================================
    // Methods
    //==================================================

    Schema schema();

    S newState();

}
