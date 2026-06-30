package com.oliveryasuna.mc.omniconfig.schema;

import com.oliveryasuna.mc.omniconfig.api.Format;

import java.util.Objects;
import java.util.Optional;

/**
 * The complete in-memory model of one config: identity, format, version, and
 * entry tree.
 */
public record Schema(
        Class<?> type,
        String id,
        String name,
        Format format,
        int version,
        SchemaCategory root
) {

    //==================================================
    // Constructors
    //==================================================

    public Schema(
            final Class<?> type,
            final String id,
            final String name,
            final Format format,
            final int version,
            final SchemaCategory root
    ) {
        this.type = Objects.requireNonNull(type);
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.format = Objects.requireNonNull(format);
        this.version = version;
        this.root = Objects.requireNonNull(root);
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Looks up an entry by dotted path, e.g., {@code "display.opacity"}.
     *
     * @param path The dotted path.
     * @return The entry, if found.
     */
    public Optional<SchemaEntry> find(final String path) {
        final String[] parts = path.split("\\.");

        SchemaCategory cat = root;
        for(int i = 0; i < parts.length - 1; i++) {
            final Optional<SchemaCategory> next = cat.category(parts[i]);
            if(next.isEmpty()) {
                return Optional.empty();
            }

            cat = next.get();
        }

        return cat.entry(parts[parts.length - 1]);
    }

}
