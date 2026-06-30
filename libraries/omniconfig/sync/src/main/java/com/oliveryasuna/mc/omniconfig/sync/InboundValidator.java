package com.oliveryasuna.mc.omniconfig.sync;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.schema.SchemaCategory;
import com.oliveryasuna.mc.omniconfig.schema.SchemaEntry;
import com.oliveryasuna.mc.omniconfig.validation.ValidationIssue;
import com.oliveryasuna.mc.omniconfig.validation.ValidationResult;
import com.oliveryasuna.mc.omniconfig.validation.Validator;
import com.oliveryasuna.mc.omniconfig.validation.validator.LengthValidator;
import com.oliveryasuna.mc.omniconfig.value.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-validates an incoming {@link ValueTree} against a {@link Schema} before
 * the client applies it.
 * <p>
 * Policy: <strong>per-entry reject + structured report</strong>. Each entry
 * that fails to decode or fails any validator is dropped (its
 * {@link Rejection} recorded); every entry that survives is carried into the
 * returned {@link InboundResult#accepted()} tree, ready to hand to
 * {@link ScopeEnforcer#applyAuthoritative}. Whole-payload rejection is left to
 * the caller — typically, a caller may inspect
 * {@link InboundResult#rejections()} and decide to log loudly, alert ops, or
 * drop the connection entirely.
 * <p>
 * Only {@code com.oliveryasuna.mc.omniconfig.api.annotation.Sync.Scope#SERVER}
 * and {@code com.oliveryasuna.mc.omniconfig.api.annotation.Sync.Scope#COMMON}
 * entries are inspected; {@code CLIENT} entries in the incoming tree are
 * ignored entirely (the server has no business sending them).
 *
 * <h2>Pre-decode caps (defense against crafted payloads)</h2>
 * Before invoking the codec, every entry is checked against a size cap so a
 * malicious server cannot ask the client to allocate an arbitrarily large
 * value. Three kinds of caps apply:
 * <ul>
 *   <li><b>String length</b> — default {@link #DEFAULT_STRING_CAP}
 *   ({@value #DEFAULT_STRING_CAP}) characters.</li>
 *   <li><b>List size</b> — default {@link #DEFAULT_COLLECTION_CAP}
 *   ({@value #DEFAULT_COLLECTION_CAP}) elements.</li>
 *   <li><b>Map size</b> — default {@link #DEFAULT_COLLECTION_CAP}
 *   ({@value #DEFAULT_COLLECTION_CAP}) entries.</li>
 * </ul>
 * Per-entry overrides ride the existing
 * {@code com.oliveryasuna.mc.omniconfig.api.annotation.Length @Length}
 * annotation: if the entry declares one, its {@code max} is the cap; else
 * the default applies. A value exceeding the cap is rejected without ever
 * being decoded — the {@link Rejection} carries an "exceeds cap" reason.
 */
public final class InboundValidator {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Default maximum {@code String} length accepted from the wire.
     */
    public static final int DEFAULT_STRING_CAP = 4096;

    /**
     * Default maximum collection (list/map) size accepted from the wire.
     */
    public static final int DEFAULT_COLLECTION_CAP = 1024;

    //==================================================
    // Static methods
    //==================================================

    public static InboundResult validate(
            final Schema schema,
            final ValueTree incoming,
            final CodecRegistry codecs
    ) {
        final List<Rejection> rejections = new ArrayList<>();
        final Section acceptedRoot = new Section();
        walk(schema.root(), incoming.root(), acceptedRoot, "", codecs, rejections);
        return new InboundResult(new ValueTree(acceptedRoot), List.copyOf(rejections));
    }

    // Walk
    //--------------------------------------------------

    private static void walk(
            final SchemaCategory category,
            final Section incoming,
            final Section accepted,
            final String prefix,
            final CodecRegistry codecs,
            final List<Rejection> rejections
    ) {
        for(final SchemaEntry entry : category.entries()) {
            if(!ScopeEnforcer.isAuthoritative(entry.getMetadata().getSyncScope())) {
                continue;
            }
            final ValueNode node = incoming == null ? null : incoming.get(entry.getKey());
            if(node == null || (node instanceof Scalar(final Object value) && value == null)) {
                continue;
            }
            final String path = prefix + entry.getKey();
            final String capRejection = capExceeded(entry, node);
            if(capRejection != null) {
                rejections.add(new Rejection(path, node, capRejection));
                continue;
            }
            final Object decoded;
            try {
                decoded = codecs.codecFor(entry.getType()).decode(node);
            } catch(final CodecException e) {
                rejections.add(new Rejection(path, node, "decode failed: " + e.getMessage()));
                continue;
            }
            final String validatorMessage = firstFailingValidator(entry.getMetadata().getValidators(), decoded);
            if(validatorMessage != null) {
                rejections.add(new Rejection(path, node, validatorMessage));
                continue;
            }
            accepted.put(entry.getKey(), node);
        }
        for(final SchemaCategory sub : category.categories()) {
            final ValueNode child = incoming == null ? null : incoming.get(sub.getName());
            final Section childSection = child instanceof final Section cs ? cs : null;
            final Section acceptedChild = new Section();
            walk(sub, childSection, acceptedChild, prefix + sub.getName() + ".", codecs, rejections);
            if(acceptedChild.size() > 0) {
                accepted.put(sub.getName(), acceptedChild);
            }
        }
    }

    /**
     * @return A rejection reason string when {@code node}'s pre-decode size
     * exceeds the {@code entry}'s effective cap, or {@code null} when the
     * value is within the cap (or the entry kind has no applicable cap).
     */
    private static String capExceeded(
            final SchemaEntry entry,
            final ValueNode node
    ) {
        final ValueType.Kind kind = entry.getType().getKind();
        if(kind == ValueType.Kind.SCALAR
           && entry.getType().getRawType() == String.class
           && node instanceof Scalar(final Object inner)
           && inner instanceof final CharSequence cs) {
            final int cap = effectiveCap(entry, DEFAULT_STRING_CAP);
            return cs.length() > cap
                    ? "string length " + cs.length() + " exceeds cap " + cap
                    : null;
        }
        if(kind == ValueType.Kind.LIST && node instanceof ListNode(final List<ValueNode> items)) {
            final int cap = effectiveCap(entry, DEFAULT_COLLECTION_CAP);
            return items.size() > cap
                    ? "collection size " + items.size() + " exceeds cap " + cap
                    : null;
        }
        if(kind == ValueType.Kind.MAP && node instanceof final Section section) {
            final int cap = effectiveCap(entry, DEFAULT_COLLECTION_CAP);
            return section.size() > cap
                    ? "map size " + section.size() + " exceeds cap " + cap
                    : null;
        }
        return null;
    }

    /**
     * @return The effective cap for {@code entry} — its
     * {@link LengthValidator}{@code .max()} if declared via {@code @Length},
     * else {@code defaultCap}.
     */
    private static int effectiveCap(
            final SchemaEntry entry,
            final int defaultCap
    ) {
        for(final Validator<?> validator : entry.getMetadata().getValidators()) {
            if(validator instanceof final LengthValidator length) {
                return length.max();
            }
        }
        return defaultCap;
    }

    private static String firstFailingValidator(
            final List<Validator<?>> validators,
            final Object value
    ) {
        for(final Validator<?> validator : validators) {
            final ValidationResult result = validator.validateRaw(value);
            if(!result.isValid()) {
                final List<ValidationIssue> issues = result.getIssues();
                return issues.isEmpty() ? "rejected by " + validator.getClass().getSimpleName() : issues.getFirst().message();
            }
        }
        return null;
    }

    //==================================================
    // Constructors
    //==================================================

    private InboundValidator() {
        super();

        throw new UnsupportedInstantiationException();
    }

    //==================================================
    // Nested
    //==================================================

    public record InboundResult(
            ValueTree accepted,
            List<Rejection> rejections
    ) {

        //==================================================
        // Constructors
        //==================================================

        public InboundResult {
            rejections = List.copyOf(rejections);
        }

    }

    public record Rejection(
            String path,
            ValueNode received,
            String reason
    ) {

    }

}
