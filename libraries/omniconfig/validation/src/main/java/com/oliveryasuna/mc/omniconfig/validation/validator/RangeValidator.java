package com.oliveryasuna.mc.omniconfig.validation.validator;

import com.oliveryasuna.mc.omniconfig.validation.ValidationIssue;
import com.oliveryasuna.mc.omniconfig.validation.ValidationResult;
import com.oliveryasuna.mc.omniconfig.validation.Validator;

/**
 * Clamps numeric values to an inclusive {@code [min, max]} range.
 */
public record RangeValidator(
        double min,
        double max
) implements Validator<Number> {

    //==================================================
    // Static methods
    //==================================================

    private static Object coerce(
            final double v,
            final Class<?> target
    ) {
        if(target == Integer.class) {
            return (int)Math.round(v);
        } else if(target == Long.class) {
            return Math.round(v);
        } else if(target == Short.class) {
            return (short)Math.round(v);
        } else if(target == Byte.class) {
            return (byte)Math.round(v);
        } else if(target == Float.class) {
            return (float)v;
        } else {
            return v; // Double or unknown Number
        }
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValidationResult validate(final Number value) {
        if(value == null) {
            return ValidationResult.ok();
        }

        final double d = value.doubleValue();

        if(d < min) {
            return ValidationResult.invalid(ValidationIssue.corrected("value " + value + " is below minimum " + min, coerce(min, value.getClass())));
        } else if(d > max) {
            return ValidationResult.invalid(ValidationIssue.corrected("value " + value + " is above maximum " + max, coerce(max, value.getClass())));
        }

        return ValidationResult.ok();
    }

}
