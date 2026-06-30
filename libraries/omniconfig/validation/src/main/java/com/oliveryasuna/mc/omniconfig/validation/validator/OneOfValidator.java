package com.oliveryasuna.mc.omniconfig.validation.validator;

import com.oliveryasuna.mc.omniconfig.validation.ValidationIssue;
import com.oliveryasuna.mc.omniconfig.validation.ValidationResult;
import com.oliveryasuna.mc.omniconfig.validation.Validator;

import java.util.Set;

/**
 * Restricts a value's string form to an explicit allow-list.
 */
public record OneOfValidator(
        Set<String> allowed
) implements Validator<String> {

    //==================================================
    // Constructors
    //==================================================

    public OneOfValidator(final Set<String> allowed) {
        this.allowed = Set.copyOf(allowed);
    }

    public OneOfValidator(final String[] allowed) {
        this(Set.of(allowed));
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValidationResult validate(final String value) {
        if(value == null) {
            return ValidationResult.ok();
        }

        return allowed.contains(value)
                ? ValidationResult.ok()
                : ValidationResult.invalid(ValidationIssue.of("value \"" + value + "\" is not one of " + allowed));
    }

}
