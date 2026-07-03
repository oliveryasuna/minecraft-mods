package com.oliveryasuna.mc.rubric.format.json5;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
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
 * Attaches schema-declared comments to a Jankson {@link JsonObject}.
 * <p>
 * Walks the schema's category tree, calling
 * {@link JsonObject#setComment(String, String)} for each entry and
 * {@link JsonObject#setComment(String, String)} on the parent for each
 * sub-category (Jankson attaches comments by parent-key lookup, so recursion
 * descends into the child object as it goes).
 * <p>
 * Auto-generated hint lines (range, allowed values, default) are appended to
 * the user-declared comment lines per entry, mirroring the TOML
 * {@code CommentApplier}.
 */
final class Json5CommentApplier {

    //==================================================
    // Static methods
    //==================================================

    static void apply(
            final JsonObject root,
            final Schema schema
    ) {
        applyCategory(root, schema.root());
    }

    private static void applyCategory(
            final JsonObject destination,
            final SchemaCategory category
    ) {
        for(final SchemaEntry entry : category.entries()) {
            final List<String> lines = buildEntryComment(entry);
            if(!lines.isEmpty() && destination.containsKey(entry.getKey())) {
                destination.setComment(entry.getKey(), joinLines(lines));
            }
        }

        for(final SchemaCategory sub : category.categories()) {
            final JsonElement child = destination.get(sub.getName());
            if(!(child instanceof final JsonObject childObject)) {
                continue;
            }
            if(!sub.getComment().isEmpty()) {
                destination.setComment(sub.getName(), joinLines(sub.getComment()));
            }
            applyCategory(childObject, sub);
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

    private static String joinLines(final List<String> lines) {
        return String.join("\n", lines);
    }

    //==================================================
    // Constructors
    //==================================================

    private Json5CommentApplier() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
