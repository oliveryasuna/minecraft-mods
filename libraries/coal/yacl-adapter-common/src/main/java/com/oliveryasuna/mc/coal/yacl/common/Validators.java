package com.oliveryasuna.mc.coal.yacl.common;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.annotation.*;
import com.oliveryasuna.mc.coal.api.validation.ValidationResult;
import com.oliveryasuna.mc.coal.api.validation.Validator;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Built-in {@link Validator} implementations for the constraint annotations in
 * {@code coal-api} plus a factory that materializes them from a {@link Field}.
 */
public final class Validators {

    //==================================================
    // Static methods
    //==================================================

    // Factory
    //--------------------------------------------------

    /**
     * Read every constraint annotation on {@code field} and return the
     * corresponding validators. Order:
     * {@code @NotNull}, {@code @Range}, {@code @Length}, {@code @OneOf}, {@code @Pattern}.
     */
    static List<Validator<?>> forField(final Field field) {
        final List<Validator<?>> vs = new ArrayList<>();

        if(field.isAnnotationPresent(NotNull.class)) {
            vs.add(new NotNullValidator());
        }

        final Range range = field.getAnnotation(Range.class);
        if(range != null) {
            vs.add(new RangeValidator(range.min(), range.max()));
        }

        final Length length = field.getAnnotation(Length.class);
        if(length != null) {
            vs.add(new LengthValidator(length.min(), length.max()));
        }

        final OneOf oneOf = field.getAnnotation(OneOf.class);
        if(oneOf != null) {
            vs.add(new OneOfValidator(oneOf.value()));
        }

        final Pattern pattern = field.getAnnotation(Pattern.class);
        if(pattern != null) {
            vs.add(new PatternValidator(pattern.value()));
        }

        return vs;
    }

    //==================================================
    // Constructors
    //==================================================

    private Validators() {
        super();

        throw new UnsupportedInstantiationException();
    }

    //==================================================
    // Nested
    //==================================================

    static final class NotNullValidator implements Validator<Object> {

        //==================================================
        // Constructors
        //==================================================

        NotNullValidator() {
            super();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public ValidationResult validate(
                final Object value,
                final ValidationContext ctx
        ) {
            if(value == null) {
                return ValidationResult.invalid("value must not be null", ctx.entry().defaultValue());
            }

            return ValidationResult.ok();
        }

    }

    public static final class RangeValidator implements Validator<Object> {

        //==================================================
        // Fields
        //==================================================

        private final double min;
        private final double max;

        //==================================================
        // Constructors
        //==================================================

        RangeValidator(
                final double min,
                final double max
        ) {
            super();

            this.min = min;
            this.max = max;
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public ValidationResult validate(
                final Object value,
                final ValidationContext ctx
        ) {
            if(!(value instanceof final Number n)) {
                return ValidationResult.ok();  // Non-numeric -> let another validator handle it.
            }

            final double d = n.doubleValue();
            if(d < min) {
                return ValidationResult.invalid("value " + d + " below min " + min, min);
            } else if(d > max) {
                return ValidationResult.invalid("value " + d + " above max " + max, max);
            }

            return ValidationResult.ok();
        }

        //==================================================
        // Getters/setters
        //==================================================

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

    }

    public static final class LengthValidator implements Validator<Object> {

        //==================================================
        // Fields
        //==================================================

        private final int min;
        private final int max;

        //==================================================
        // Constructors
        //==================================================

        LengthValidator(
                final int min,
                final int max
        ) {
            super();

            this.min = min;
            this.max = max;
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public ValidationResult validate(
                final Object value,
                final ValidationContext ctx
        ) {
            if(value == null) {
                return ValidationResult.ok();
            }

            final int len;
            if(value instanceof final String s) {
                len = s.length();
            } else if(value instanceof final Collection<?> c) {
                len = c.size();
            } else if(value instanceof final Map<?, ?> m) {
                len = m.size();
            } else {
                return ValidationResult.ok();  // Not sizable — validator doesn't apply.
            }

            if(len < min) {
                return ValidationResult.invalid("length " + len + " below min " + min, null);
            } else if(len > this.max) {
                return ValidationResult.invalid("length " + len + " above max " + max, null);
            }

            return ValidationResult.ok();
        }

        //==================================================
        // Getters/setters
        //==================================================

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

    }

    public static final class OneOfValidator implements Validator<Object> {

        //==================================================
        // Fields
        //==================================================

        private final Set<String> allowed;

        //==================================================
        // Constructors
        //==================================================

        OneOfValidator(final String[] values) {
            super();

            this.allowed = Set.of(values);
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public ValidationResult validate(
                final Object value,
                final ValidationContext ctx
        ) {
            if(!(value instanceof final String s)) {
                return ValidationResult.ok();  // Non-string -> skip.
            } else if(!allowed.contains(s)) {
                final String first = allowed.iterator().next();
                return ValidationResult.invalid("value '" + s + "' not in allowed set " + allowed, first);
            }

            return ValidationResult.ok();
        }

        //==================================================
        // Getters/setters
        //==================================================

        public Set<String> getAllowed() {
            return allowed;
        }

    }

    static final class PatternValidator implements Validator<Object> {

        //==================================================
        // Fields
        //==================================================

        private final java.util.regex.Pattern pattern;

        //==================================================
        // Constructors
        //==================================================

        PatternValidator(final String regex) {
            super();

            this.pattern = java.util.regex.Pattern.compile(regex);
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public ValidationResult validate(
                final Object value,
                final ValidationContext ctx
        ) {
            if(!(value instanceof final String s)) {
                return ValidationResult.ok();
            } else if(!pattern.matcher(s).matches()) {
                return ValidationResult.invalid("value '" + s + "' does not match pattern " + pattern.pattern(), ctx.entry().defaultValue());
            }

            return ValidationResult.ok();
        }

    }

}
