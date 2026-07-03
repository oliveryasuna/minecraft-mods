package com.oliveryasuna.mc.coal.api;

import java.util.Optional;
import java.util.Set;

public interface Format {

    //==================================================
    // Static fields
    //==================================================

    Format TOML = SimpleFormat.of("toml", "toml", true);
    Format JSON = SimpleFormat.of("json", "json", false);
    Format JSON5 = SimpleFormat.of("json5", "json5", true);

    //==================================================
    // Static methods
    //==================================================

    /**
     * Lookup by ID.
     * <p>
     * Returns a built-in when {@code id} matches; otherwise returns a synthetic
     * {@link Format} wrapping the given ID. Providers that support custom
     * formats override this via a service-loader registry.
     */
    static Format of(final String id) {
        // TODO: Implement.
    }

    static Format of(
            final String id,
            final String extension,
            final boolean comments
    ) {
        // TODO: Implement.
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Stable string identifier (e.g., "toml", "json", "json5").
     * <p>
     * Case-insensitive.
     */
    String id();

    /**
     * Default file extension, no leading dot.
     */
    String defaultExtension();

    /**
     * Whether this format preserves per-entry comments across load/save.
     */
    boolean supportsComments();

    //==================================================
    // Nested
    //==================================================

    /**
     * Registry so providers can pin exotic formats (e.g., HOCON) to a canonical
     * instance.
     */
    interface Registry {

        //==================================================
        // Methods
        //==================================================

        void register(Format format);

        Optional<Format> lookup(String id);

        Set<Format> all();

    }

}
