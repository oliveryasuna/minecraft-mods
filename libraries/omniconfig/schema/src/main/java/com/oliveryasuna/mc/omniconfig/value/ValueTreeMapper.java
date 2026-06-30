package com.oliveryasuna.mc.omniconfig.value;

import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.schema.SchemaCategory;
import com.oliveryasuna.mc.omniconfig.schema.SchemaEntry;
import com.oliveryasuna.mc.omniconfig.validation.Correction;
import com.oliveryasuna.mc.omniconfig.validation.Corrector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Maps between a populated config instance and a format-neutral
 * {@link ValueTree}.
 * <p>
 * {@link #toTree} encodes every entry via the {@link CodecRegistry}.
 * {@link #fromTree} decodes present entries onto a target instance, leaving
 * defaults in place for absent/null nodes and resetting to default (with a
 * recorded {@link Correction}) when a node cannot be decoded. Constraint
 * checking is a separate stage ({@link Corrector}).
 */
public final class ValueTreeMapper {

    //==================================================
    // Fields
    //==================================================

    private final CodecRegistry codecs;

    //==================================================
    // Constructors
    //==================================================

    public ValueTreeMapper(final CodecRegistry codecs) {
        super();

        this.codecs = codecs;
    }

    //==================================================
    // Methods
    //==================================================

    public ValueTree toTree(
            final Schema schema,
            final Object instance
    ) {
        return toTree(schema, instance, path -> true);
    }

    /**
     * Encodes only entries whose dotted path satisfies {@code includePath}.
     * Used by {@code ConfigManager.save()} to filter out values whose origin
     * is non-local (e.g. server-pushed snapshot values on a remote-connected
     * client). Categories with no included entries are still emitted as
     * empty sections so structure is preserved.
     */
    public ValueTree toTree(
            final Schema schema,
            final Object instance,
            final Predicate<String> includePath
    ) {
        final Section root = new Section();
        writeCategory(schema.root(), instance, root, "", includePath);
        return new ValueTree(root);
    }

    private void writeCategory(
            final SchemaCategory category,
            final Object root,
            final Section section,
            final String prefix,
            final Predicate<String> includePath
    ) {
        for(final SchemaEntry entry : category.entries()) {
            final String path = prefix + entry.getKey();
            if(!includePath.test(path)) {
                continue;
            }
            final Object value = entry.readFrom(root);
            final ValueNode node = value == null
                    ? new Scalar(null)
                    : codecs.codecFor(entry.getType()).encode(value);
            section.put(entry.getKey(), node);
        }
        for(final SchemaCategory sub : category.categories()) {
            final Section child = new Section();
            writeCategory(sub, root, child, prefix + sub.getName() + ".", includePath);
            section.put(sub.getName(), child);
        }
    }

    public List<Correction> fromTree(
            final Schema schema,
            final ValueTree tree,
            final Object instance
    ) {
        return fromTree(schema, tree, instance, path -> {
        });
    }

    /**
     * Decodes present entries onto {@code instance} and invokes
     * {@code onApplied} once per entry whose value was actually written
     * (including the decode-failure-to-default path). Absent and
     * explicit-null entries do not fire. Caller uses this to mark per-path
     * origin (e.g. LOCAL_EDIT) without re-walking the schema.
     */
    public List<Correction> fromTree(
            final Schema schema,
            final ValueTree tree,
            final Object instance,
            final Consumer<String> onApplied
    ) {
        final List<Correction> corrections = new ArrayList<>();
        readCategory(schema.root(), tree.root(), instance, "", corrections, onApplied);
        return corrections;
    }

    private void readCategory(
            final SchemaCategory category,
            final Section section,
            final Object root,
            final String prefix,
            final List<Correction> out,
            final Consumer<String> onApplied
    ) {
        for(final SchemaEntry entry : category.entries()) {
            final String path = prefix + entry.getKey();
            final ValueNode node = section == null ? null : section.get(entry.getKey());

            // Null semantics (v1, by design): an absent key AND an explicit
            // null both keep the field's default. A config file therefore
            // cannot force a field to null. Rationale: TOML has no clean null,
            // and non-null defaults keep consumer code NPE-safe. (Documentation
            // note: state this explicitly in user docs; model
            // genuinely-optional values with a sentinel or an "enable"
            // companion rather than relying on null).
            if(node == null || (node instanceof Scalar(final Object value) && value == null)) {
                continue;  // absent or explicit null -> keep default
            }

            try {
                final Object decoded = codecs.codecFor(entry.getType()).decode(node);
                entry.writeTo(root, decoded);
            } catch(final CodecException e) {
                entry.writeTo(root, entry.getDefaultValue());
                out.add(new Correction(path, node, entry.getDefaultValue(), "could not decode (" + e.getMessage() + "); reset to default"));
            }
            onApplied.accept(path);
        }
        for(final SchemaCategory sub : category.categories()) {
            final ValueNode childNode = section == null ? null : section.get(sub.getName());
            final Section childSection = childNode instanceof final Section cs ? cs : null;
            readCategory(sub, childSection, root, prefix + sub.getName() + ".", out, onApplied);
        }
    }

}
