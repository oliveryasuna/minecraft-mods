package com.oliveryasuna.mc.rubric.format.nightconfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.schema.SchemaCategory;
import com.oliveryasuna.mc.rubric.schema.SchemaEntry;
import com.oliveryasuna.mc.rubric.validation.Validator;
import com.oliveryasuna.mc.rubric.validation.validator.OneOfValidator;
import com.oliveryasuna.mc.rubric.validation.validator.RangeValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Attaches the comments declared on a {@link Schema} to a NightConfig
 * {@link CommentedConfig}. Walks the schema's category tree, calling
 * {@link CommentedConfig#setComment(List, String)} at each entry and
 * sub-category path.
 * <p>
 * Multi-line comments are joined with {@code \n} after each line is prefixed
 * with {@code " "}; NightConfig's writer prepends {@code "#"} on output, so the
 * emitted lines read {@code "# text"} (canonical TOML style).
 * <p>
 * <strong>Module-internal.</strong> Public only so the {@code toml} package
 * (and future {@code json5} package) can call it.
 * <p>
 * Auto-generated hint lines (range, allowed values, default) are appended to
 * the user-declared comment lines per entry.
 */
public final class CommentApplier {

    //==================================================
    // Static methods
    //==================================================

    public static void apply(
            final CommentedConfig destination,
            final Schema schema
    ) {
        applyCategory(destination, schema.root(), List.of());
    }

    private static void applyCategory(
            final CommentedConfig destination,
            final SchemaCategory category,
            final List<String> path
    ) {
        if(!path.isEmpty() && !category.getComment().isEmpty()) {
            destination.setComment(path, joinLines(category.getComment()));
        }

        for(final SchemaEntry entry : category.entries()) {
            final List<String> lines = buildEntryComment(entry);
            if(lines.isEmpty()) {
                continue;
            }
            destination.setComment(append(path, entry.getKey()), joinLines(lines));
        }

        for(final SchemaCategory sub : category.categories()) {
            applyCategory(destination, sub, append(path, sub.getName()));
        }
    }

    private static List<String> buildEntryComment(final SchemaEntry entry) {
        final List<String> lines = new ArrayList<>(entry.getMetadata().getComment());
        for(final Validator<?> validator : entry.getMetadata().getValidators()) {
            if(validator instanceof RangeValidator(final double min, final double max)) {
                lines.add("Range: [" + formatBound(min) + ", " + formatBound(max) + "].");
            } else if(validator instanceof OneOfValidator(final Set<String> allowed)) {
                lines.add("Allowed: " + String.join(", ", allowed) + ".");
            }
        }
        if(entry.getDefaultValue() != null) {
            lines.add("Default: " + entry.getDefaultValue() + ".");
        }
        return lines;
    }

    private static String formatBound(final double v) {
        return v == Math.floor(v) && !Double.isInfinite(v) ? Long.toString((long)v) : Double.toString(v);
    }

    private static List<String> append(
            final List<String> path,
            final String segment
    ) {
        final List<String> next = new ArrayList<>(path.size() + 1);
        next.addAll(path);
        next.add(segment);
        return next;
    }

    private static String joinLines(final List<String> lines) {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < lines.size(); i++) {
            if(i > 0) sb.append('\n');
            sb.append(' ').append(lines.get(i));
        }
        return sb.toString();
    }

    //==================================================
    // Constructors
    //==================================================

    private CommentApplier() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
