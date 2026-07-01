package com.oliveryasuna.mc.rubric.validation;

import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.schema.SchemaCategory;
import com.oliveryasuna.mc.rubric.schema.SchemaEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs each entry's validators against a populated config instance, applying
 * the first failing validator's suggested correction (or the entry default when
 * none is suggested) and recording what changed.
 * <p>
 * Never throws on bad data.
 */
public final class Corrector {

    //==================================================
    // Constructors
    //==================================================

    public Corrector() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    public List<Correction> correct(
            final Schema schema,
            final Object instance
    ) {
        final List<Correction> corrections = new ArrayList<>();
        walk(schema.root(), instance, "", corrections);

        return corrections;
    }

    private void walk(
            final SchemaCategory category,
            final Object root,
            final String prefix,
            final List<Correction> out
    ) {
        for(final SchemaEntry entry : category.entries()) {
            final String path = prefix + entry.getKey();
            Object value = entry.readFrom(root);

            for(final Validator<?> validator : entry.getMetadata().getValidators()) {
                final ValidationResult result = validator.validateRaw(value);
                if(result.isValid()) {
                    continue;
                }

                final ValidationIssue issue = result.getIssues().getFirst();
                final Object replacement = issue.hasCorrection()
                        ? issue.correction()
                        : entry.getDefaultValue();

                entry.writeTo(root, replacement);
                out.add(new Correction(path, value, replacement, issue.message()));

                value = replacement;
            }
        }

        for(final SchemaCategory sub : category.categories()) {
            walk(sub, root, prefix + sub.getName() + ".", out);
        }
    }

}
