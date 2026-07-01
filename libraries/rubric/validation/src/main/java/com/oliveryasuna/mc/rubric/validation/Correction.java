package com.oliveryasuna.mc.rubric.validation;

/**
 * A record of one applied correction during load: which path, what was
 * rejected, what replaces it, and why.
 * <p>
 * Surfaced in the load report and logged.
 */
public record Correction(
        String path,
        Object rejected,
        Object replacement,
        String message
) {

}
