package com.oliveryasuna.mc.omniconfig.sync;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.schema.SchemaCategory;
import com.oliveryasuna.mc.omniconfig.schema.SchemaEntry;
import com.oliveryasuna.mc.omniconfig.value.*;
import com.oliveryasuna.mc.omniconfig.api.annotation.Sync;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Splits a config along {@link Sync.Scope} boundaries:
 * <ul>
 *     <li>
 *         {@link #extractAuthoritative(Schema, Object, CodecRegistry)} builds a
 *         {@link ValueTree} containing only entries whose effective scope is
 *         {@link Sync.Scope#SERVER} or {@link Sync.Scope#COMMON}. Used by the
 *         server to construct the snapshot it pushes to clients.
 *     </li>
 *     <li>
 *         {@link #applyAuthoritative(Schema, ValueTree, Object, CodecRegistry)}
 *         walks an incoming tree and applies <strong>only</strong> the
 *         {@code SERVER}/{@code COMMON} entries onto a client-side instance —
 *         {@code CLIENT} entries on the client are never touched, regardless
 *         of whether the incoming tree contains them. This is the
 *         server-authoritative contract.
 *     </li>
 * </ul>
 * Categories with no in-scope entries are omitted from the extracted tree to
 * keep wire size honest.
 */
public final class ScopeEnforcer {

    //==================================================
    // Static methods
    //==================================================

    public static boolean isAuthoritative(final Sync.Scope scope) {
        return scope == Sync.Scope.SERVER || scope == Sync.Scope.COMMON;
    }

    public static ValueTree extractAuthoritative(
            final Schema schema,
            final Object instance,
            final CodecRegistry codecs
    ) {
        final Section root = new Section();
        writeCategory(schema.root(), instance, root, codecs);
        return new ValueTree(root);
    }

    /**
     * Applies authoritative entries from {@code incoming} onto
     * {@code instance} and returns the set of dotted paths that were
     * actually written. Caller uses the path set to drive per-path
     * bookkeeping (e.g. marking origin = FROM_REMOTE on a client). Paths
     * are insertion-ordered ({@link LinkedHashSet}) so listeners can rely
     * on schema-declaration order.
     */
    public static Set<String> applyAuthoritative(
            final Schema schema,
            final ValueTree incoming,
            final Object instance,
            final CodecRegistry codecs
    ) {
        final Set<String> applied = new LinkedHashSet<>();
        readCategory(schema.root(), incoming.root(), instance, codecs, "", applied);
        return applied;
    }

    // Write (instance -> ValueTree, scope-filtered)
    //--------------------------------------------------

    private static void writeCategory(
            final SchemaCategory category,
            final Object root,
            final Section section,
            final CodecRegistry codecs
    ) {
        for(final SchemaEntry entry : category.entries()) {
            if(!isAuthoritative(entry.getMetadata().getSyncScope())) {
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
            writeCategory(sub, root, child, codecs);
            if(child.size() > 0) {
                section.put(sub.getName(), child);
            }
        }
    }

    // Read (ValueTree -> instance, scope-filtered)
    //--------------------------------------------------

    private static void readCategory(
            final SchemaCategory category,
            final Section section,
            final Object root,
            final CodecRegistry codecs,
            final String prefix,
            final Set<String> applied
    ) {
        for(final SchemaEntry entry : category.entries()) {
            if(!isAuthoritative(entry.getMetadata().getSyncScope())) {
                continue;
            }
            final ValueNode node = section == null ? null : section.get(entry.getKey());
            if(node == null || (node instanceof Scalar(final Object value) && value == null)) {
                continue;
            }
            try {
                final Object decoded = codecs.codecFor(entry.getType()).decode(node);
                entry.writeTo(root, decoded);
                applied.add(prefix + entry.getKey());
            } catch(final CodecException ignored) {
                // Inbound decode failure is silent at this layer; InboundValidator
                // catches and rejects upstream before we ever get here in the
                // happy path. If it slips through (e.g. direct test use),
                // leave the field untouched rather than corrupt it.
            }
        }
        for(final SchemaCategory sub : category.categories()) {
            final ValueNode child = section == null ? null : section.get(sub.getName());
            final Section childSection = child instanceof final Section cs ? cs : null;
            readCategory(sub, childSection, root, codecs, prefix + sub.getName() + ".", applied);
        }
    }

    //==================================================
    // Constructors
    //==================================================

    private ScopeEnforcer() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
