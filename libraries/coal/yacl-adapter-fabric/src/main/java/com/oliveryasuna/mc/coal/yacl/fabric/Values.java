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

}
