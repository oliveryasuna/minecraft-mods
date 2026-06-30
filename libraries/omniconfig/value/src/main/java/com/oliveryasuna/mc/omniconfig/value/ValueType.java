package com.oliveryasuna.mc.omniconfig.value;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Describes the type of a config entry: a raw class plus, for collections, the
 * captured element type. {@link Kind#SCALAR} covers single-codec leaf values
 * (primitives, {@code String}, and a small set of built-in value types such as
 * {@link UUID}/{@link Instant}/{@link Duration}).
 */
public final class ValueType {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Object types treated as single-codec leaves rather than nested
     * categories.
     */
    private static final Set<Class<?>> LEAF_VALUE_TYPES = Set.of(UUID.class, Instant.class, Duration.class);

    //==================================================
    // Static methods
    //==================================================

    // Factories (used by the builder API; the reader uses of(Type))

    public static ValueType scalar(final Class<?> raw) {
        if(!isScalar(raw)) {
            throw new IllegalArgumentException("not a scalar/leaf value type: " + raw);
        }
        return new ValueType(raw, Kind.SCALAR, null);
    }

    public static ValueType enumType(final Class<?> raw) {
        if(!raw.isEnum()) {
            throw new IllegalArgumentException("not an enum: " + raw);
        }
        return new ValueType(raw, Kind.ENUM, null);
    }

    public static ValueType listOf(final ValueType element) {
        return new ValueType(List.class, Kind.LIST, element);
    }

    public static ValueType mapOf(final ValueType value) {
        return new ValueType(Map.class, Kind.MAP, value);
    }

    /**
     * Resolves a {@link ValueType} from a reflective generic type using
     * built-in scalar classification only. Equivalent to
     * {@link #of(Type, Set) of(genericType, Set.of())}.
     *
     * @param genericType The generic type.
     * @return The value type.
     */
    public static ValueType of(final Type genericType) {
        return of(genericType, Set.of());
    }

    /**
     * Resolves a {@link ValueType} from a reflective generic type, treating
     * {@code extraLeaves} as scalar leaf types in addition to the built-ins.
     * Callers thread their {@link CodecRegistry}'s {@code getLeafTypes()} here
     * so registered leaves (e.g. {@code ResourceLocation}) get
     * {@link Kind#SCALAR} classification instead of falling through to
     * {@link Kind#OBJECT} (which would make {@code AnnotationSchemaReader}
     * recurse into them as nested categories).
     *
     * @param genericType The generic type.
     * @param extraLeaves Additional classes to treat as scalar leaves.
     * @return The value type.
     */
    public static ValueType of(final Type genericType, final Set<Class<?>> extraLeaves) {
        if(genericType instanceof final Class<?> cls) {
            if(isScalar(cls, extraLeaves)) {
                return new ValueType(cls, Kind.SCALAR, null);
            } else if(cls.isEnum()) {
                return new ValueType(cls, Kind.ENUM, null);
            } else if(List.class.isAssignableFrom(cls)) {
                return new ValueType(cls, Kind.LIST, of(Object.class, extraLeaves));
            } else if(Map.class.isAssignableFrom(cls)) {
                return new ValueType(cls, Kind.MAP, of(Object.class, extraLeaves));
            }
            return new ValueType(cls, Kind.OBJECT, null);
        }
        if(genericType instanceof final ParameterizedType pt) {
            final Class<?> raw = (Class<?>)pt.getRawType();
            final Type[] args = pt.getActualTypeArguments();
            if(List.class.isAssignableFrom(raw)) {
                return new ValueType(raw, Kind.LIST, of(args[0], extraLeaves));
            } else if(Map.class.isAssignableFrom(raw)) {
                if(!(args[0] instanceof final Class<?> keyClass) || keyClass != String.class) {
                    throw new IllegalArgumentException("Map keys must be String, got: " + args[0]);
                }
                return new ValueType(raw, Kind.MAP, of(args[1], extraLeaves));
            }
            return new ValueType(raw, Kind.OBJECT, null);
        }

        throw new IllegalArgumentException("Unsupported config field type: " + genericType);
    }

    /**
     * True for primitives, their boxed forms, {@code String}, and built-in
     * leaf value types. Equivalent to
     * {@link #isScalar(Class, Set) isScalar(cls, Set.of())}.
     *
     * @param cls The class to check.
     * @return True if the class is a scalar/leaf value type.
     */
    public static boolean isScalar(final Class<?> cls) {
        return isScalar(cls, Set.of());
    }

    /**
     * True if {@code cls} is a built-in scalar OR an entry in
     * {@code extraLeaves}.
     *
     * @param cls         The class to check.
     * @param extraLeaves Additional classes treated as scalar leaves.
     * @return True if scalar.
     */
    public static boolean isScalar(final Class<?> cls, final Set<Class<?>> extraLeaves) {
        return cls.isPrimitive()
               || cls == Boolean.class
               || cls == Byte.class
               || cls == Short.class
               || cls == Integer.class
               || cls == Long.class
               || cls == Float.class
               || cls == Double.class
               || cls == Character.class
               || cls == String.class
               || LEAF_VALUE_TYPES.contains(cls)
               || extraLeaves.contains(cls);
    }

    //==================================================
    // Fields
    //==================================================

    private final Class<?> rawType;
    private final Kind kind;
    private final ValueType elementType;

    //==================================================
    // Constructors
    //==================================================

    private ValueType(
            final Class<?> rawType,
            final Kind kind,
            final ValueType elementType
    ) {
        super();

        this.rawType = Objects.requireNonNull(rawType, "rawType");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.elementType = elementType;
    }

    //==================================================
    // Getters/setters
    //==================================================

    public Class<?> getRawType() {
        return rawType;
    }

    public Kind getKind() {
        return kind;
    }

    public ValueType getElementType() {
        return elementType;
    }

    //==================================================
    // Object methods
    //==================================================

    @Override
    public String toString() {
        return switch(kind) {
            case LIST -> "List<" + elementType + ">";
            case MAP -> "Map<String, " + elementType + ">";
            default -> rawType.getSimpleName();
        };
    }

    //==================================================
    // Nested
    //==================================================

    public enum Kind {

        //==================================================
        // Values
        //==================================================

        SCALAR,

        ENUM,

        LIST,

        MAP,

        OBJECT
    }

}
