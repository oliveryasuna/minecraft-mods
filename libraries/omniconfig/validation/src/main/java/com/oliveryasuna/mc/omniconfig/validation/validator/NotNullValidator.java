package com.oliveryasuna.mc.omniconfig.validation.validator;

import com.oliveryasuna.mc.omniconfig.validation.ValidationIssue;
import com.oliveryasuna.mc.omniconfig.validation.ValidationResult;
import com.oliveryasuna.mc.omniconfig.validation.Validator;

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
