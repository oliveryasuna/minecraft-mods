package com.oliveryasuna.mc.omniconfig.validation;

/**
 * A single validation failure.
 * <p>
 * {@code hasCorrection} indicates whether {@code correction} holds a usable
 * replacement value; when {@code false}, the corrector falls back to the
 * entry's default.
 */
public record ValidationIssue(
        String message,
        boolean hasCorrection,
        Object correction
) {

    //==================================================
    // Static methods
    //==================================================

    public static ValidationIssue of(final String message) {
        return new ValidationIssue(message, false, null);
    }

    public static ValidationIssue corrected(
            final String message,
            final Object correction
    ) {
        return new ValidationIssue(message, true, correction);
    }

}
