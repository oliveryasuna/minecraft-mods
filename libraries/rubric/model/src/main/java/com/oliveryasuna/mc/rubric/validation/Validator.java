package com.oliveryasuna.mc.rubric.validation;

/**
 * Validates a single config value of type {@code T}, optionally suggesting a
 * corrected value.
 *
 * @param <T> The type of value to validate.
 */
@FunctionalInterface
public interface Validator<T> {

    //==================================================
    // Methods
    //==================================================

    /**
     * Returns {@link ValidationResult#ok()} if the value is acceptable.
     *
     * @param value The value to validate.
     * @return The validation result.
     */
    ValidationResult validate(T value);

    /**
     * Framework entry point: applies this validator to an untyped runtime
     * value.
     *
     * @param value The value to validate.
     * @return The validation result.
     */
    @SuppressWarnings("unchecked")
    default ValidationResult validateRaw(final Object value) {
        return validate((T)value);
    }

}
