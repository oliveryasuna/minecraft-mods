package com.oliveryasuna.mc.coal.api.migration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility for walking dotted paths through {@code Map<String, Object>} trees.
 * Used by {@link MigrationOp} factories.
 */
final class PathAccessor {

    //==================================================
    // Fields
    //==================================================

    private final Map<String, Object> parent;
    private final String key;

    //==================================================
    // Constructors
    //==================================================

    private PathAccessor(
            final Map<String, Object> parent,
            final String key
    ) {
        super();

        this.parent = parent;
        this.key = key;
    }

    //==================================================
    // Static methods
    //==================================================

    /**
     * Walks {@code tree} along the dotted {@code path}. If {@code create} is
     * {@code true}, missing intermediate segments are created as new
     * {@link LinkedHashMap}s. If {@code create} is {@code false}, missing
     * intermediates cause this to return {@code null}.
     * <p>
     * If a non-map is found at an intermediate segment, always returns
     * {@code null} (treated as "absent").
     */
    @SuppressWarnings("unchecked")
    static PathAccessor resolve(
            final Map<String, Object> tree,
            final String path,
            final boolean create
    ) {
        final String[] segments = path.split("\\.");
        Map<String, Object> cursor = tree;
        for(int i = 0; i < segments.length - 1; i++) {
            final String seg = segments[i];
            final Object child = cursor.get(seg);
            if(child instanceof Map) {
                cursor = (Map<String, Object>)child;
            } else if(child == null && !cursor.containsKey(seg)) {
                if(!create) {
                    return null;
                }

                final Map<String, Object> fresh = new LinkedHashMap<>();
                cursor.put(seg, fresh);
                cursor = fresh;
            } else {
                // Intermediate exists but isn't a map (e.g., a scalar).
                return null;
            }
        }

        return new PathAccessor(cursor, segments[segments.length - 1]);
    }

    //==================================================
    // Getters/setters
    //==================================================

    Map<String, Object> parent() {
        return parent;
    }

    String key() {
        return key;
    }

}
