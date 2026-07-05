package com.oliveryasuna.mc.coal.api.validation;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public sealed interface ValidationResult permits ValidationResult.Ok, ValidationResult.Invalid {

    //==================================================
    // Static methods
    //==================================================

    static Ok ok() {
        return new Ok();
    }

    static Invalid invalid(final List<ValidationIssue> issues) {
        return new Invalid(issues);
    }

    static Invalid invalid(
            final String message,
            final Object suggestion
    ) {
        return invalid(Collections.singletonList(new ValidationIssue(message, Optional.of(suggestion))));
    }

    //==================================================
    // Methods
    //==================================================

    boolean isOk();

    boolean isInvalid();

    List<ValidationIssue> issues();

    //==================================================
    // Nested
    //==================================================

    // Purposefully a class so we can return the same instance from `issues()`
    // every time.
    final class Ok implements ValidationResult {

        //==================================================
        // Fields
        //==================================================

        private final List<ValidationIssue> issues;

        //==================================================
        // Constructors
        //==================================================

        public Ok() {
            super();

            this.issues = Collections.emptyList();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public List<ValidationIssue> issues() {
            return issues;
        }

        //==================================================
        // Object methods
        //==================================================

        @Override
        public boolean equals(final Object other) {
            if(this == other) return true;
            if(other == null || getClass() != other.getClass()) return false;

            final Ok otherCasted = (Ok)other;

            return new EqualsBuilder()
                    .append(issues, otherCasted.issues)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(issues)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("issues", issues)
                    .toString();
        }

    }

    record Invalid(List<ValidationIssue> issues) implements ValidationResult {

        //==================================================
        // Constructors
        //==================================================

        public Invalid {
            if(issues.isEmpty()) {
                throw new IllegalArgumentException("Invalid ValidationResult must have at least one issue.");
            }

            issues = List.copyOf(issues);
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public boolean isInvalid() {
            return true;
        }

    }

}
