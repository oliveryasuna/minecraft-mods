package com.oliveryasuna.mc.omniconfig.validation.validator;

import com.oliveryasuna.mc.omniconfig.validation.ValidationIssue;
import com.oliveryasuna.mc.omniconfig.validation.ValidationResult;
import com.oliveryasuna.mc.omniconfig.validation.Validator;

import java.util.regex.Pattern;

/**
 * Requires a string value to fully match a regular expression
 */
public record PatternValidator(
        Pattern pattern
) implements Validator<String> {

    //==================================================
    // Constructors
    //==================================================

    public PatternValidator(final String regex) {
        this(Pattern.compile(regex));
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValidationResult validate(final String value) {
        if(value == null) {
            return ValidationResult.ok();
        }

        return pattern.matcher(value).matches()
                ? ValidationResult.ok()
                : ValidationResult.invalid(ValidationIssue.of("value \"" + value + "\" does not match /" + pattern.pattern() + "/"));
    }

    public String getPattern() {
        return pattern.pattern();
    }

}
