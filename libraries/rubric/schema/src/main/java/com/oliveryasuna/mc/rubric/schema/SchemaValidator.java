package com.oliveryasuna.mc.rubric.schema;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.api.annotation.Range;
import com.oliveryasuna.mc.rubric.validation.Validator;
import com.oliveryasuna.mc.rubric.validation.validator.LengthValidator;
import com.oliveryasuna.mc.rubric.validation.validator.OneOfValidator;
import com.oliveryasuna.mc.rubric.validation.validator.PatternValidator;
import com.oliveryasuna.mc.rubric.validation.validator.RangeValidator;
import com.oliveryasuna.mc.rubric.value.ValueType;
import com.oliveryasuna.mc.rubric.api.annotation.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Registration-time sanity checks: catches constraint/widget misuse (e.g.,
 * {@link Range} on a
 * {@link String}, {@code @Widget(SLIDER)} without bounds) with a single error
 * listing every problem, instead of failing later or silently.
 */
public final class SchemaValidator {

    //==================================================
    // Static fields
    //==================================================

    private static final Set<Class<?>> NUMERIC = Set.of(
            int.class,
            long.class,
            short.class,
            byte.class,
            float.class,
            double.class,
            Integer.class,
            Long.class,
            Short.class,
            Byte.class,
            Float.class,
            Double.class
    );

    //==================================================
    // Static methods
    //==================================================

    public static void validate(final Schema schema) {
        final List<String> problems = new ArrayList<>();
        walk(schema.root(), "", problems);
        if(!problems.isEmpty()) {
            throw new IllegalStateException("Invalid config schema '" + schema.id() + "':\n  - " + String.join("\n  - ", problems));
        }
    }

    private static void walk(
            final SchemaCategory category,
            final String prefix,
            final List<String> problems
    ) {
        for(final SchemaEntry entry : category.entries()) {
            check(prefix + entry.getKey(), entry, problems);
        }
        for(final SchemaCategory sub : category.categories()) {
            walk(sub, prefix + sub.getName() + ".", problems);
        }
    }


    private static void check(
            final String path,
            final SchemaEntry entry,
            final List<String> problems
    ) {
        final ValueType type = entry.getType();

        boolean hasRange = false;
        boolean hasLength = false;
        boolean hasPattern = false;
        boolean hasOneOf = false;

        for(final Validator<?> v : entry.getMetadata().getValidators()) {
            if(v instanceof RangeValidator) {
                hasRange = true;
            } else if(v instanceof LengthValidator) {
                hasLength = true;
            } else if(v instanceof PatternValidator) {
                hasPattern = true;
            } else if(v instanceof OneOfValidator) {
                hasOneOf = true;
            }
        }

        if(hasRange && !isNumeric(type)) {
            problems.add(path + ": @Range requires a numeric type, but is " + type);
        }
        if(hasLength && !(isString(type) || type.getKind() == ValueType.Kind.LIST || type.getKind() == ValueType.Kind.MAP)) {
            problems.add(path + ": @Length requires String, List, or Map, but is " + type);
        }
        if(hasPattern && !isString(type)) {
            problems.add(path + ": @Pattern requires String, but is " + type);
        }
        if(hasOneOf && !isString(type)) {
            problems.add(path + ": @OneOf requires String, but is " + type);
        }

        final Widget.Type widget = entry.getMetadata().getWidget();
        if(widget == Widget.Type.SLIDER && !hasRange) {
            problems.add(path + ": @Widget(SLIDER) requires a bounded @Range");
        }
        if(widget == Widget.Type.COLOR && !isString(type)) {
            problems.add(path + ": @Widget(COLOR) requires a String (#RRGGBB)");
        }
        if(widget == Widget.Type.DROPDOWN && type.getKind() != ValueType.Kind.ENUM && !hasOneOf) {
            problems.add(path + ": @Widget(DROPDOWN) requires an enum or @OneOf");
        }
    }


    private static boolean isNumeric(final ValueType type) {
        return type.getKind() == ValueType.Kind.SCALAR && NUMERIC.contains(type.getRawType());
    }

    private static boolean isString(final ValueType type) {
        return type.getKind() == ValueType.Kind.SCALAR && type.getRawType() == String.class;
    }

    //==================================================
    // Constructors
    //==================================================

    private SchemaValidator() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
