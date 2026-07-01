package com.oliveryasuna.mc.rubric.validation;

import java.util.List;

/**
 * Outcome of validation a value: either valid, or a list of issues.
 */
public final class ValidationResult {

    //==================================================
    // Static fields
    //==================================================

    private static final ValidationResult OK = new ValidationResult(List.of());

    //==================================================
    // Static methods
    //==================================================

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult invalid(final ValidationIssue issue) {
        return new ValidationResult(List.of(issue));
    }

    public static ValidationResult of(final List<ValidationIssue> issues) {
        return issues.isEmpty() ? OK : new ValidationResult(issues);
    }

    //==================================================
    // Fields
    //==================================================

    private final List<ValidationIssue> issues;

    //==================================================
    // Constructors
    //==================================================

    private ValidationResult(final List<ValidationIssue> issues) {
        super();

        this.issues = List.copyOf(issues);
    }

    //==================================================
    // Methods
    //==================================================

    public boolean isValid() {
        return issues.isEmpty();
    }

    //==================================================
    // Getters/setters
    //==================================================

    public List<ValidationIssue> getIssues() {
        return issues;
    }

}
