package com.oliveryasuna.mc.coal.api.migration;

import java.util.Map;
import java.util.function.Function;

/**
 * A single in-place mutation on the parsed config tree.
 * <p>
 * <b>Tree shape.</b> The tree is a mutable {@code Map<String, Object>}.
 * Nested tables are also {@code Map<String, Object>}. Lists are
 * {@code List<Object>}. Scalars are {@link String} / {@link Number} /
 * {@link Boolean} / {@code null}.
 * <p>
 * <b>Paths.</b> Dotted paths. Write ops auto-create intermediate map segments;
 * read ops treat missing intermediates as absent (no throw).
 */
@FunctionalInterface
public interface MigrationOp {

    //==================================================
    // Static methods
    //==================================================

    // Factories
    //--------------------------------------------------

    /**
     * Move the value at {@code from} to {@code to}. Auto-creates intermediate
     * segments on the destination path.
     * <ul>
     *   <li>Source absent -> no-op.</li>
     *   <li>Source present + destination absent -> move.</li>
     *   <li>
     *       Source present + destination present ->
     *       {@link IllegalStateException} (rename would clobber existing data).
     *   </li>
     * </ul>
     */
    static MigrationOp renameKey(
            final String from,
            final String to
    ) {
        return tree -> {
            final PathAccessor source = PathAccessor.resolve(tree, from, false);
            if(source == null || !source.parent().containsKey(source.key())) {
                return; // source absent
            }

            final Object value = source.parent().get(source.key());

            final PathAccessor dest = PathAccessor.resolve(tree, to, true);
            if(dest.parent().containsKey(dest.key())) {
                throw new IllegalStateException("renameKey: destination '" + to + "' already exists");
            }

            source.parent().remove(source.key());
            dest.parent().put(dest.key(), value);
        };
    }

    /**
     * Remove the leaf at {@code path}.
     * <ul>
     *   <li>Absent -> no-op.</li>
     *   <li>
     *       Present -> remove; parent maps are NOT pruned even if left empty.
     *   </li>
     * </ul>
     */
    static MigrationOp removeKey(final String path) {
        return tree -> {
            final PathAccessor leaf = PathAccessor.resolve(tree, path, false);
            if(leaf != null) {
                leaf.parent().remove(leaf.key());
            }
        };
    }

    /**
     * Set {@code path} to {@code value} only when the leaf key is entirely
     * absent from its parent map. A present-but-null value counts as
     * present and is left untouched. Auto-creates intermediate segments.
     */
    static MigrationOp setDefault(
            final String path,
            final Object value
    ) {
        return tree -> {
            final PathAccessor leaf = PathAccessor.resolve(tree, path, true);
            if(!leaf.parent().containsKey(leaf.key())) {
                leaf.parent().put(leaf.key(), value);
            }
        };
    }

    /**
     * Set {@code path} to {@code value}, overwriting any existing entry.
     * Auto-creates intermediate segments.
     */
    static MigrationOp setValue(
            final String path,
            final Object value
    ) {
        return tree -> {
            final PathAccessor leaf = PathAccessor.resolve(tree, path, true);
            leaf.parent().put(leaf.key(), value);
        };
    }

    /**
     * Apply {@code fn} to the current value at {@code path} and store the
     * result.
     * <ul>
     *   <li>Path absent -> no-op ({@code fn} is not invoked).</li>
     *   <li>
     *       Path present -> {@code parent.put(key, fn.apply(current))}. A
     *       returned {@code null} sets the value to {@code null}; it does not
     *       remove the entry.
     *   </li>
     * </ul>
     */
    static MigrationOp transform(
            final String path,
            final Function<Object, Object> fn
    ) {
        return tree -> {
            final PathAccessor leaf = PathAccessor.resolve(tree, path, false);
            if(leaf == null || !leaf.parent().containsKey(leaf.key())) {
                return;
            }

            final Object before = leaf.parent().get(leaf.key());
            leaf.parent().put(leaf.key(), fn.apply(before));
        };
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Apply this op to the tree. See class-level javadoc for the expected
     * tree shape.
     */
    void apply(Map<String, Object> tree);

}
