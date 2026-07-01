package com.oliveryasuna.mc.rubric.validation.validator;

import com.oliveryasuna.mc.rubric.validation.ValidationIssue;
import com.oliveryasuna.mc.rubric.validation.ValidationResult;
import com.oliveryasuna.mc.rubric.validation.Validator;

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
        switch(value) {
            case null -> {
                return ValidationResult.ok();
            }
            case final CharSequence s -> {
                final int len = s.length();

                return inBounds(len)
                        ? ValidationResult.ok()
                        : ValidationResult.invalid(ValidationIssue.of("length " + len + " outside [" + min + ", " + max + "]"));
            }
            case final Collection<?> c -> {
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
            }
            case final Map<?, ?> m -> {
                final int size = m.size();

                return inBounds(size)
                        ? ValidationResult.ok()
                        : ValidationResult.invalid(ValidationIssue.of("size " + size + " outside [" + min + ", " + max + "]"));
            }
            default -> {
            }
        }

        return ValidationResult.ok();
    }

    private boolean inBounds(final int n) {
        return n >= min && n <= max;
    }

}
