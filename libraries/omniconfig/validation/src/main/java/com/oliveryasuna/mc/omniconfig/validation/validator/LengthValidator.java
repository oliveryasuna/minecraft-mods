package com.oliveryasuna.mc.omniconfig.validation.validator;

import com.oliveryasuna.mc.omniconfig.validation.ValidationIssue;
import com.oliveryasuna.mc.omniconfig.validation.ValidationResult;
import com.oliveryasuna.mc.omniconfig.validation.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Constraints the length of a {@link CharSequence} or the size of a
 * {@link Collection}/{@link Map}.
 * <p>
 * Over-long collections are truncated (suggested correction); strings/maps
 * out-of-bounds report without correction, leaving the corrector to reset to
 * default.
 */
public record LengthValidator(
        int min,
        int max
) implements Validator<Object> {

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValidationResult validate(final Object value) {
        if(value == null) {
            return ValidationResult.ok();
        }

        if(value instanceof final CharSequence s) {
            final int len = s.length();

            return inBounds(len)
                    ? ValidationResult.ok()
                    : ValidationResult.invalid(ValidationIssue.of("length " + len + " outside [" + min + ", " + max + "]"));
        } else if(value instanceof final Collection<?> c) {
            final int size = c.size();

            if(size > max) {
                final List<Object> truncated = c.stream()
                        .limit(max)
                        .map(o -> (Object)o)
                        .toList();

                return ValidationResult.invalid(ValidationIssue.corrected("size " + size + " exceeds maximum " + max, truncated));
            } else if(size < min) {
                return ValidationResult.invalid(ValidationIssue.of("size " + size + " below minimum " + min));
            }

            return ValidationResult.ok();
        } else if(value instanceof final Map<?, ?> m) {
            final int size = m.size();

            return inBounds(size)
                    ? ValidationResult.ok()
                    : ValidationResult.invalid(ValidationIssue.of("size " + size + " outside [" + min + ", " + max + "]"));
        }

        return ValidationResult.ok();
    }

    private boolean inBounds(final int n) {
        return n >= min && n <= max;
    }

}
