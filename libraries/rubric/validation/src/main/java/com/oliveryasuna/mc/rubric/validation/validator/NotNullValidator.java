package com.oliveryasuna.mc.rubric.validation.validator;

import com.oliveryasuna.mc.rubric.validation.ValidationIssue;
import com.oliveryasuna.mc.rubric.validation.ValidationResult;
import com.oliveryasuna.mc.rubric.validation.Validator;

/**
 * Rejects a null value (the corrector then resets it to the entry default).
 */
public record NotNullValidator() implements Validator<Object> {

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValidationResult validate(final Object value) {
        return value != null
                ? ValidationResult.ok()
                : ValidationResult.invalid(ValidationIssue.of("value must not be null"));
    }

}
