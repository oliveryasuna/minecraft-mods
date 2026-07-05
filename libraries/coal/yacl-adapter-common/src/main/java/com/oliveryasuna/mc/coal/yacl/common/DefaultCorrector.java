package com.oliveryasuna.mc.coal.yacl.common;

import com.oliveryasuna.mc.coal.api.schema.Schema;
import com.oliveryasuna.mc.coal.api.schema.SchemaEntry;
import com.oliveryasuna.mc.coal.api.validation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Walks a loaded instance, invokes every validator on every entry, and replaces
 * invalid values with the first validator's {@code suggestion} — falling back
 * to the entry's declared default when no suggestion is offered.
 */
final class DefaultCorrector implements Corrector {

    //==================================================
    // Static methods
    //==================================================

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ValidationResult runValidators(
            final SchemaEntry entry,
            final String path,
            final Object value
    ) {
        final List<ValidationIssue> issues = new ArrayList<>();
        for(final Validator v : entry.metadata().validators()) {
            final ValidationResult r;
            try {
                r = v.validate(value, new ValidatorContext(entry, path));
            } catch(final RuntimeException e) {
                continue;  // A misbehaving validator MUST NOT block correction.
            }

            if(r.isInvalid()) {
                issues.addAll(r.issues());
            }
        }

        return issues.isEmpty() ? ValidationResult.ok() : ValidationResult.invalid(issues);
    }

    private static Object pickReplacement(
            final SchemaEntry entry,
            final ValidationResult result
    ) {
        for(final ValidationIssue issue : result.issues()) {
            if(issue.suggestion().isPresent()) {
                return issue.suggestion().get();
            }
        }

        return entry.defaultValue();
    }

    private static boolean sameValue(
            final Object a,
            final Object b
    ) {
        if(a == b) {
            return true;
        } else if(a == null || b == null) {
            return false;
        }

        return a.equals(b);
    }

    private static String joinMessages(final List<ValidationIssue> issues) {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < issues.size(); i++) {
            if(i > 0) {
                sb.append("; ");
            }
            sb.append(issues.get(i).message());
        }

        return sb.toString();
    }

    //==================================================
    // Constructors
    //==================================================

    DefaultCorrector() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    // Corrector
    //--------------------------------------------------

    @Override
    public List<Correction> correct(
            final Schema schema,
            final Object instance
    ) {
        final List<Correction> corrections = new ArrayList<>();
        for(final String path : schema.paths()) {
            final Optional<SchemaEntry> entryOpt = schema.find(path);
            if(entryOpt.isEmpty()) {
                continue;
            }

            final SchemaEntry entry = entryOpt.get();

            final Object before = entry.readFrom(instance);
            final ValidationResult result = runValidators(entry, path, before);
            if(!result.isInvalid()) {
                continue;
            }

            final Object after = pickReplacement(entry, result);
            if(sameValue(before, after)) {
                continue;
            }

            entry.writeTo(instance, after);
            corrections.add(new Correction(path, before, after, joinMessages(result.issues())));
        }

        return corrections;
    }

    //==================================================
    // Nested
    //==================================================

    private record ValidatorContext(
            SchemaEntry entry,
            String path
    ) implements Validator.ValidationContext {

    }

}
