package com.oliveryasuna.mc.coal.yacl.common;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.config.ConfigManager;
import com.oliveryasuna.mc.coal.api.schema.EntryMetadata;
import com.oliveryasuna.mc.coal.api.validation.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MC-free helpers shared between the Fabric and NeoForge YaclScreenProvider
 * variants. Every method touches only coal-api types + java.lang / java.util;
 * anything that references {@code net.minecraft.*} or
 * {@code dev.isxander.yacl3.*} stays in the per-loader source.
 */
public final class YaclScreenSupport {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Read the current live value at {@code path} from the {@code manager}'s
     * state. Never returns the entry's default — that's the caller's decision.
     * Returns {@code null} when the schema doesn't contain the path.
     */
    public static Object readLive(
            final ConfigManager<?> manager,
            final String path
    ) {
        return manager.schema().find(path).map(entry -> entry.readFrom(manager.get())).orElse(null);
    }

    /**
     * Best-effort scalar coercion for GUI-binding purposes. Returns:
     * <ul>
     *     <li>A type-appropriate zero for {@code null} inputs when {@code target}
     *         is a known scalar wrapper or an enum (first enum constant).</li>
     *     <li>{@code value} unchanged when it's already assignable to
     *         {@code target}.</li>
     *     <li>The narrowed numeric value when {@code value} is a {@code Number}
     *         and {@code target} is a boxed numeric type.</li>
     *     <li>{@code null} otherwise (caller falls back to the entry default).</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(
            final Object value,
            final Class<T> target
    ) {
        if(value == null) {
            if(target == Boolean.class) {
                return (T)Boolean.FALSE;
            } else if(target == Integer.class) {
                return (T)Integer.valueOf(0);
            } else if(target == Long.class) {
                return (T)Long.valueOf(0L);
            } else if(target == Double.class) {
                return (T)Double.valueOf(0d);
            } else if(target == Float.class) {
                return (T)Float.valueOf(0f);
            } else if(target == String.class) {
                return (T)"";
            } else if(target.isEnum()) {
                return target.getEnumConstants()[0];
            }

            return null;
        } else if(target.isInstance(value)) {
            return target.cast(value);
        } else if(value instanceof final Number n) {
            if(target == Integer.class) {
                return (T)Integer.valueOf(n.intValue());
            } else if(target == Long.class) {
                return (T)Long.valueOf(n.longValue());
            } else if(target == Double.class) {
                return (T)Double.valueOf(n.doubleValue());
            } else if(target == Float.class) {
                return (T)Float.valueOf(n.floatValue());
            }
        }

        return null;
    }

    /**
     * Coerce {@code raw} to a {@code List<T>}: {@code null} → empty list;
     * {@code List<?>} → per-element coercion via {@link #coerce}; anything
     * else → empty list. Never returns {@code null}.
     */
    public static <T> List<T> coerceList(
            final Object raw,
            final Class<T> elementType
    ) {
        if(!(raw instanceof final List<?> src)) {
            return new ArrayList<>();
        }

        final List<T> out = new ArrayList<>(src.size());
        for(final Object v : src) {
            final T coerced = coerce(v, elementType);
            if(coerced != null || v == null) {
                out.add(coerced);
            }
        }

        return out;
    }

    /**
     * Find the first validator on {@code meta} whose runtime type is
     * {@code type}. Returns {@code Optional.empty()} if none matches.
     */
    @SuppressWarnings("unchecked")
    public static <V extends Validator<?>> Optional<V> findValidator(
            final EntryMetadata meta,
            final Class<V> type
    ) {
        for(final Validator<?> v : meta.validators()) {
            if(type.isInstance(v)) {
                return Optional.of((V)v);
            }
        }

        return Optional.empty();
    }

    /**
     * Parse a {@code "#RRGGBB"} hex string to a 24-bit RGB int. Accepts an
     * optional leading {@code #}. Falls back to {@code fallback} on null or
     * unparseable input.
     */
    public static int parseColor(
            final String hex,
            final int fallback
    ) {
        if(hex == null) {
            return fallback;
        }

        final String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch(final NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * Format a 24-bit RGB int as {@code "#RRGGBB"}.
     */
    public static String formatColor(final int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    //==================================================
    // Constructors
    //==================================================

    private YaclScreenSupport() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
