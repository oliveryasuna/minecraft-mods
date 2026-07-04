package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.schema.SchemaEntry;
import com.oliveryasuna.mc.coal.api.schema.ValueAccessor;
import com.oliveryasuna.mc.coal.api.schema.ValueType;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Internal value-layer implementations: {@link ValueType} + {@link ValueAccessor}.
 */
final class Values {

    //==================================================
    // Constructors
    //==================================================

    private Values() {
        super();

        throw new UnsupportedInstantiationException();
    }

    //==================================================
    // Nested
    //==================================================

    /**
     * Scalar / enum / list / map / object {@link ValueType} impl. Fields present
     * only when relevant to the kind: {@code element} for LIST, {@code value} for
     * MAP, {@code children} for OBJECT.
     */
    record ValueTypeImpl(
            Kind kind,
            Class<?> rawType,
            ValueType elementTypeOrNull,
            ValueType valueTypeOrNull,
            List<SchemaEntry> childrenOrEmpty
    ) implements ValueType {

        //==================================================
        // Static methods
        //==================================================

        static ValueTypeImpl scalar(final Class<?> raw) {
            return new ValueTypeImpl(Kind.SCALAR, raw, null, null, Collections.emptyList());
        }

        static ValueTypeImpl enumType(final Class<?> raw) {
            return new ValueTypeImpl(Kind.ENUM, raw, null, null, Collections.emptyList());
        }

        static ValueTypeImpl list(
                final Class<?> raw,
                final ValueType element
        ) {
            return new ValueTypeImpl(Kind.LIST, raw, element, null, Collections.emptyList());
        }

        static ValueTypeImpl map(
                final Class<?> raw,
                final ValueType value
        ) {
            return new ValueTypeImpl(Kind.MAP, raw, null, value, Collections.emptyList());
        }

        static ValueTypeImpl object(
                final Class<?> raw,
                final List<SchemaEntry> children
        ) {
            return new ValueTypeImpl(Kind.OBJECT, raw, null, null, List.copyOf(children));
        }

        //==================================================
        // Methods
        //==================================================

        // ValueType
        //--------------------------------------------------

        @Override
        public Optional<ValueType> elementType() {
            return Optional.ofNullable(elementTypeOrNull);
        }

        @Override
        public Optional<ValueType> valueType() {
            return Optional.ofNullable(valueTypeOrNull);
        }

        @Override
        public List<SchemaEntry> children() {
            return childrenOrEmpty;
        }

    }

    /**
     * Reflection-backed {@link ValueAccessor} over a single {@link Field}. The
     * field is made accessible on construction.
     */
    static final class FieldAccessor implements ValueAccessor {

        //==================================================
        // Static methods
        //==================================================

        /**
         * Best-effort numeric coercion for values coming off the wire as
         * {@code Number} but the field type is a specific primitive.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object coerce(final Object value, final Class<?> target) {
            if(value == null) return null;
            if(target.isInstance(value)) return value;

            // Primitive fields: Class.isInstance always returns false for a
            // boxed wrapper (boolean.class.isInstance(Boolean.TRUE) == false),
            // so match the boxed types explicitly. Field.set handles the
            // unboxing itself.
            if(target.isPrimitive() && isWrapperFor(target, value)) {
                return value;
            }

            if(value instanceof final Number n) {
                if(target == int.class || target == Integer.class) {
                    return n.intValue();
                } else if(target == long.class || target == Long.class) {
                    return n.longValue();
                } else if(target == double.class || target == Double.class) {
                    return n.doubleValue();
                } else if(target == float.class || target == Float.class) {
                    return n.floatValue();
                } else if(target == short.class || target == Short.class) {
                    return n.shortValue();
                } else if(target == byte.class || target == Byte.class) {
                    return n.byteValue();
                }
            }

            if(target.isEnum() && value instanceof final String s) {
                try {
                    return Enum.valueOf((Class<Enum>)target, s);
                } catch(final IllegalArgumentException ignored) {
                    // Fall through to a ClassCastException at the assignment site.
                }
            }

            throw new ClassCastException("Cannot assign value of type " + value.getClass().getName() + " to field type " + target.getName());
        }

        private static boolean isWrapperFor(final Class<?> primitive, final Object value) {
            return (primitive == boolean.class && value instanceof Boolean)
                   || (primitive == int.class && value instanceof Integer)
                   || (primitive == long.class && value instanceof Long)
                   || (primitive == double.class && value instanceof Double)
                   || (primitive == float.class && value instanceof Float)
                   || (primitive == short.class && value instanceof Short)
                   || (primitive == byte.class && value instanceof Byte)
                   || (primitive == char.class && value instanceof Character);
        }

        //==================================================
        // Fields
        //==================================================

        private final Field field;

        //==================================================
        // Constructors
        //==================================================

        FieldAccessor(final Field field) {
            super();

            this.field = field;
            this.field.setAccessible(true);
        }

        //==================================================
        // Methods
        //==================================================

        // ValueAccessor
        //--------------------------------------------------

        @Override
        public Object read(final Object instance) {
            try {
                return field.get(instance);
            } catch(final IllegalAccessException e) {
                throw new IllegalStateException("Field access failed: " + field, e);
            }
        }

        @Override
        public void write(
                final Object instance,
                final Object value
        ) {
            try {
                field.set(instance, coerce(value, field.getType()));
            } catch(final IllegalAccessException e) {
                throw new IllegalStateException("Field write failed: " + field, e);
            }
        }

        @Override
        public Class<?> declaredType() {
            return field.getType();
        }

    }

    /**
     * Reflection-backed {@link ValueAccessor} that walks a chain of fields
     * (parent -> nested -> ... -> leaf). Used by {@code AnnotationSchemaReader}
     * when it recurses into non-annotated POJO fields to flatten them as
     * sub-category entries.
     * <p>
     * Read walks up to the leaf's parent and reads the leaf; write does the
     * same and writes with primitive-aware coercion. Intermediate objects
     * MUST be non-null on read — the outermost {@code @Config} POJO's
     * constructor is expected to initialize them.
     */
    static final class ChainedFieldAccessor implements ValueAccessor {

        //==================================================
        // Fields
        //==================================================

        private final Field[] chain;

        //==================================================
        // Constructors
        //==================================================

        ChainedFieldAccessor(final Field[] chain) {
            super();

            if(chain.length == 0) {
                throw new IllegalArgumentException("chain must have at least one field");
            }
            this.chain = chain;
            for(final Field f : this.chain) {
                f.setAccessible(true);
            }
        }

        //==================================================
        // Methods
        //==================================================

        // ValueAccessor
        //--------------------------------------------------

        @Override
        public Object read(final Object instance) {
            try {
                Object cur = instance;
                for(final Field f : chain) {
                    if(cur == null) {
                        return null;
                    }

                    cur = f.get(cur);
                }

                return cur;
            } catch(final IllegalAccessException e) {
                throw new IllegalStateException("Field access failed: " + describe(), e);
            }
        }

        @Override
        public void write(
                final Object instance,
                final Object value
        ) {
            try {
                Object cur = instance;
                for(int i = 0; i < chain.length - 1; i++) {
                    if(cur == null) {
                        return;
                    }

                    cur = chain[i].get(cur);
                }
                if(cur == null) {
                    return;
                }

                final Field leaf = chain[chain.length - 1];
                leaf.set(cur, FieldAccessor.coerce(value, leaf.getType()));
            } catch(final IllegalAccessException e) {
                throw new IllegalStateException("Field write failed: " + describe(), e);
            }
        }

        @Override
        public Class<?> declaredType() {
            return chain[chain.length - 1].getType();
        }

        private String describe() {
            final StringBuilder sb = new StringBuilder();
            for(int i = 0; i < chain.length; i++) {
                if(i > 0) {
                    sb.append(".");
                }
                sb.append(chain[i].getName());
            }

            return sb.toString();
        }

    }

}
