package com.oliveryasuna.mc.rubric.value;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;

/**
 * Dotted-path navigation and mutation over a {@link Section} tree.
 * <p>
 * Used by migration ops, delta sync, and anywhere else that addresses entries
 * by their {@code "category.subcategory.entry"} path.
 */
public final class TreePaths {

    //==================================================
    // Static methods
    //==================================================

    public static ValueNode get(
            final Section root,
            final String path
    ) {
        final String[] parts = path.split("\\.");
        Section cur = root;
        for(int i = 0; i < parts.length - 1; i++) {
            if(cur.get(parts[i]) instanceof final Section s) {
                cur = s;
            } else {
                return null;
            }
        }
        return cur.get(parts[parts.length - 1]);
    }

    public static boolean has(
            final Section root,
            final String path
    ) {
        return get(root, path) != null;
    }

    public static void put(
            final Section root,
            final String path,
            final ValueNode value
    ) {
        final String[] parts = path.split("\\.");
        Section cur = root;
        for(int i = 0; i < parts.length - 1; i++) {
            if(cur.get(parts[i]) instanceof final Section s) {
                cur = s;
            } else {
                final Section created = new Section();
                cur.put(parts[i], created);
                cur = created;
            }
        }
        cur.put(parts[parts.length - 1], value);
    }

    public static ValueNode remove(
            final Section root,
            final String path
    ) {
        final String[] parts = path.split("\\.");
        Section cur = root;
        for(int i = 0; i < parts.length - 1; i++) {
            if(cur.get(parts[i]) instanceof final Section s) {
                cur = s;
            } else {
                return null;
            }
        }
        return cur.asMap().remove(parts[parts.length - 1]);
    }

    //==================================================
    // Constructors
    //==================================================

    private TreePaths() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
