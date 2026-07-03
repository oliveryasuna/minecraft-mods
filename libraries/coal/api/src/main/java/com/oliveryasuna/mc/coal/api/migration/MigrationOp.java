package com.oliveryasuna.mc.coal.api.migration;

import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface MigrationOp {

    //==================================================
    // Static methods
    //==================================================

    // Factories
    //--------------------------------------------------

    static MigrationOp renameKey(
            final String from,
            final String to
    ) {
        // TODO: Implement.
    }

    static MigrationOp removeKey(final String path) {
        // TODO: Implement.
    }

    static MigrationOp setDefault(
            final String path,
            final Object value
    ) {
        // TODO: Implement.
    }

    static MigrationOp setValue(
            final String path,
            final Object value
    ) {
        // TODO: Implement.
    }

    static MigrationOp transform(
            final String path,
            final Function<Object, Object> fn
    ) {
        // TODO: Implement.
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * {@code tree} is a mutable {@code Map<String, Object>} — the parsed
     * representation of the config file. Nested tables are also
     * {@code Map<String, Object>}. Lists are {@code List<Object>}. Scalars are
     * {@link String}/{@link Number}/{@link Boolean}.
     */
    void apply(Map<String, Object> tree);

}
