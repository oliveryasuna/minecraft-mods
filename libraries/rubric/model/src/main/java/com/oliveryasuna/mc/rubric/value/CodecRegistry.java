package com.oliveryasuna.mc.rubric.value;

import com.oliveryasuna.mc.rubric.value.codec.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Resolves a {@link ValueCodec} for a {@link ValueType}.
 * <p>
 * Scalars, enums, lists, and maps are handled built-in; custom value objects
 * are supported by registering a codec for their class.
 */
public final class CodecRegistry {

    //==================================================
    // Static methods
    //==================================================

    private static <T> ValueCodec<Object> scalar(
            final Function<T, Object> encode,
            final Function<Object, T> decode
    ) {
        return cast(new ScalarCodec<>(encode, decode));
    }

    private static boolean asBool(final Object o) {
        if(o instanceof final Boolean b) return b;

        return Boolean.parseBoolean(String.valueOf(o));
    }

    private static Number asNumber(final Object o) {
        if(o instanceof final Number n) return n;
        return Double.valueOf(String.valueOf(o));  // throws NumberFormatException -> CodecException
    }

    private static char asChar(final Object o) {
        final String s = String.valueOf(o);
        if(s.length() != 1) {
            throw new IllegalArgumentException("expected single character, got '" + s + "'");
        }
        return s.charAt(0);
    }

    @SuppressWarnings("unchecked")
    private static ValueCodec<Object> cast(final ValueCodec<?> c) {
        return (ValueCodec<Object>)c;
    }

    //==================================================
    // Fields
    //==================================================

    private final Map<Class<?>, ValueCodec<Object>> scalars;
    private final Map<Class<?>, ValueCodec<Object>> custom;
    /**
     * Types registered via {@link #registerLeaf(Class, ValueCodec)} — these are
     * treated as scalar leaves by
     * {@link ValueType#of(java.lang.reflect.Type, Set)} instead of being
     * recursed into as nested categories. Built-in scalars (primitives, String,
     * UUID, Instant, Duration) are NOT in this set; they are recognized
     * directly by {@link ValueType#isScalar(Class)}.
     */
    private final Set<Class<?>> extraLeaves;

    //==================================================
    // Constructors
    //==================================================

    public CodecRegistry() {
        super();

        this.scalars = new HashMap<>();
        this.custom = new HashMap<>();
        this.extraLeaves = new HashSet<>();

        registerBuiltinScalars();
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Registers a custom codec for a type. Does NOT mark the type as a leaf
     * for schema-construction purposes — use
     * {@link #registerLeaf(Class, ValueCodec)} when the type should be treated
     * as a single scalar entry rather than recursed into as a nested category.
     *
     * @param type  Target type.
     * @param codec Codec for the type.
     */
    public void registerCustom(
            final Class<?> type,
            final ValueCodec<?> codec
    ) {
        custom.put(type, cast(codec));
    }

    /**
     * Registers a type as a scalar leaf with its codec. The class is added to
     * {@link #getLeafTypes()} so {@code AnnotationSchemaReader} (via
     * {@link ValueType#of(java.lang.reflect.Type, Set)}) treats it as a single
     * entry rather than recursing into it as a nested category.
     * <p>
     * Use this for custom value types that look like primitives — Minecraft's
     * {@code ResourceLocation}, a Mojang {@code Codec<T>}-bridged type, an
     * {@code IPAddress}-style domain wrapper, etc.
     *
     * @param type  Leaf type.
     * @param codec Codec for the leaf type.
     */
    public void registerLeaf(
            final Class<?> type,
            final ValueCodec<?> codec
    ) {
        extraLeaves.add(type);
        custom.put(type, cast(codec));
    }

    /**
     * @return Immutable snapshot of types registered via
     * {@link #registerLeaf(Class, ValueCodec)}. Schema construction reads this
     * to know which non-built-in classes should be classified
     * {@link ValueType.Kind#SCALAR} rather than {@link ValueType.Kind#OBJECT}.
     */
    public Set<Class<?>> getLeafTypes() {
        return Set.copyOf(extraLeaves);
    }

    /**
     * Resolves (or builds) a codec for the given type.
     *
     * @param type The type.
     * @return The codec.
     */
    public ValueCodec<Object> codecFor(final ValueType type) {
        return switch(type.getKind()) {
            case SCALAR -> {
                ValueCodec<Object> codec = scalars.get(type.getRawType());
                if(codec == null) {
                    // Custom leaf registered via registerLeaf(...) lands in
                    // `custom`; SCALAR-kind types come through both for
                    // built-ins (in `scalars`) and registered leaves.
                    codec = custom.get(type.getRawType());
                }
                if(codec == null) {
                    throw new CodecException("no scalar codec for " + type.getRawType());
                }
                yield codec;
            }
            case ENUM -> new EnumCodec(type.getRawType());
            case LIST -> new ListCodec(codecFor(type.getElementType()));
            case MAP -> new MapCodec(codecFor(type.getElementType()));
            case OBJECT -> {
                final ValueCodec<Object> codec = custom.get(type.getRawType());
                yield (codec != null)
                        ? codec
                        : new NestedObjectCodec(type.getRawType(), this);
            }
        };
    }

    private void registerBuiltinScalars() {
        put(scalar(Function.identity(), CodecRegistry::asBool), boolean.class, Boolean.class);
        put(scalar(Function.identity(), o -> asNumber(o).intValue()), int.class, Integer.class);
        put(scalar(Function.identity(), o -> asNumber(o).longValue()), long.class, Long.class);
        put(scalar(Function.identity(), o -> asNumber(o).shortValue()), short.class, Short.class);
        put(scalar(Function.identity(), o -> asNumber(o).byteValue()), byte.class, Byte.class);
        put(scalar(Function.identity(), o -> asNumber(o).floatValue()), float.class, Float.class);
        put(scalar(Function.identity(), o -> asNumber(o).doubleValue()), double.class, Double.class);
        put(scalar(String::valueOf, CodecRegistry::asChar), char.class, Character.class);
        put(scalar(Function.identity(), String::valueOf), String.class);
        put(scalar(UUID::toString, o -> (o instanceof final UUID v) ? v : UUID.fromString(String.valueOf(o))), UUID.class);
        put(scalar(Instant::toString, o -> (o instanceof final Instant v) ? v : Instant.parse(String.valueOf(o))), Instant.class);
        put(scalar(Duration::toString, o -> (o instanceof final Duration v) ? v : Duration.parse(String.valueOf(o))), Duration.class);
    }

    private void put(
            final ValueCodec<Object> codec,
            final Class<?>... types
    ) {
        for(final Class<?> type : types) {
            scalars.put(type, codec);
        }
    }

}
