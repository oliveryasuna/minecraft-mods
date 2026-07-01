package com.oliveryasuna.mc.rubric.migration;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.value.TreePaths;
import com.oliveryasuna.mc.rubric.value.ValueNode;

import java.util.function.UnaryOperator;

/**
 * Factory methods for common migration operations.
 * <p>
 * Paths are dotted ({@code "display.opacity"}); a rename across sections is a
 * move. Ops are best-effort: renaming/transforming an absent key is a no-op, so
 * re-running a migration is largely idempotent.
 */
public final class MigrationOps {

    //==================================================
    // Static methods
    //==================================================

    public static MigrationOp renameKey(
            final String fromPath,
            final String toPath
    ) {
        return root -> {
            final ValueNode value = TreePaths.remove(root, fromPath);
            if(value != null) {
                TreePaths.put(root, toPath, value);
            }
        };
    }

    public static MigrationOp removeKey(final String path) {
        return root -> TreePaths.remove(root, path);
    }

    /**
     * Sets a value only if the key is currently absent.
     *
     * @param path  Path to the value.
     * @param value Value to set.
     * @return The migration operation.
     */
    public static MigrationOp setDefault(
            final String path,
            final ValueNode value
    ) {
        return root -> {
            if(!TreePaths.has(root, path)) {
                TreePaths.put(root, path, value);
            }
        };
    }

    /**
     * Sets a value unconditionally.
     *
     * @param path  Path to the value.
     * @param value Value to set.
     * @return The migration operation.
     */
    public static MigrationOp setValue(
            final String path,
            final ValueNode value
    ) {
        return root -> {
            TreePaths.put(root, path, value);
        };
    }

    /**
     * Transforms an existing value in palce (no-op if absent).
     *
     * @param path Path to the value.
     * @param fn   Transformation function.
     * @return The migration operation.
     */
    public static MigrationOp transform(
            final String path,
            final UnaryOperator<ValueNode> fn
    ) {
        return root -> {
            final ValueNode value = TreePaths.get(root, path);
            if(value != null) {
                TreePaths.put(root, path, fn.apply(value));
            }
        };
    }

    //==================================================
    // Constructors
    //==================================================

    private MigrationOps() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
