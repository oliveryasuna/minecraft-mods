package com.oliveryasuna.mc.coal.api;

import com.oliveryasuna.commons.language.condition.Arguments;

import java.util.Locale;
import java.util.Objects;

/**
 * On-disk format for a configuration file.
 * <p>
 * <b>Open set.</b> {@link #TOML}, {@link #JSON}, and {@link #JSON5} are the
 * built-in singletons. Providers may honor arbitrary custom formats obtained
 * via {@link #of(String)} — those advertise
 * {@link com.oliveryasuna.mc.coal.api.spi.Capability#CUSTOM_FORMATS}.
 */
public interface Format {

    //==================================================
    // Static fields
    //==================================================

    Format TOML = new SimpleFormat("toml", "toml", true);
    Format JSON = new SimpleFormat("json", "json", false);
    Format JSON5 = new SimpleFormat("json5", "json5", true);

    //==================================================
    // Static methods
    //==================================================

    /**
     * Lookup by id.
     * <p>
     * Matches built-in ids case-insensitively: {@code of("Toml") == TOML}.
     * For any other id, returns a synthetic {@link Format} (with the id as
     * both {@link #id()} and {@link #defaultExtension()}, and
     * {@link #supportsComments()} defaulting to {@code false}).
     * <p>
     * Providers that don't recognize a synthetic Format's id are free to
     * reject it at load time — synthetic formats have no built-in adapter.
     */
    static Format of(final String id) {
        Objects.requireNonNull(id, "id");

        final String normalized = id.toLowerCase(Locale.ROOT);
        return switch(normalized) {
            case "toml" -> TOML;
            case "json" -> JSON;
            case "json5" -> JSON5;
            default -> new SimpleFormat(normalized, normalized, false);
        };
    }

    /**
     * Constructs a synthetic {@link Format} with an explicit extension and
     * comment-support flag. Does not consult the built-in singletons.
     */
    static Format of(
            final String id,
            final String extension,
            final boolean comments
    ) {
        Arguments.requireNotNull(id, "id");
        Arguments.requireNotNull(extension, "extension");

        return new SimpleFormat(id.toLowerCase(Locale.ROOT), extension, comments);
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Stable string identifier (e.g., "toml", "json", "json5").
     * <p>
     * Always lowercase.
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
     * Concrete {@link Format}. Record equality means two {@code SimpleFormat}
     * instances with the same {@code id} are equal, regardless of extension
     * or comment support — so {@code Format.of("toml").equals(Format.TOML)}
     * holds even if reconstructed independently.
     */
    record SimpleFormat(
            String id,
            String defaultExtension,
            boolean supportsComments
    ) implements Format {

        //==================================================
        // Constructors
        //==================================================

        public SimpleFormat {
            Arguments.requireNotNull(id, "id");
            Arguments.requireNotNull(defaultExtension, "defaultExtension");
        }

        //==================================================
        // Object methods
        //==================================================

        @Override
        public boolean equals(final Object other) {
            if(this == other) return true;
            if(!(other instanceof final SimpleFormat that)) return false;

            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

    }

}
