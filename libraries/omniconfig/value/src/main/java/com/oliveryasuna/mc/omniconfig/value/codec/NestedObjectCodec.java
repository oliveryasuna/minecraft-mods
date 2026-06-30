package com.oliveryasuna.mc.omniconfig.value.codec;

import com.oliveryasuna.mc.omniconfig.value.*;
import com.oliveryasuna.mc.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class NestedObjectCodec implements ValueCodec<Object> {

    //==================================================
    // Static fields
    //==================================================

    private static List<Field> collectFields(final Class<?> type) {
        final List<Field> out = new ArrayList<>();
        for(final Field f : type.getDeclaredFields()) {
            final int mods = f.getModifiers();
            if(Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
                continue;
            }
            f.setAccessible(true);
            out.add(f);
        }
        return out;
    }

    private static Object instantiate(final Class<?> type) {
        return ReflectionUtil.instantiate(type, e -> new CodecException("Cannot instantiate " + type.getName() + " (needs an accessible no-arg constructor)", e));
    }

    //==================================================
    // Fields
    //==================================================

    private final Class<?> type;
    private final CodecRegistry registry;
    private final List<Field> fields;

    public NestedObjectCodec(
            final Class<?> type,
            final CodecRegistry registry
    ) {
        super();

        this.type = type;
        this.registry = registry;
        this.fields = collectFields(type);
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValueNode encode(final Object value) {
        if(value == null) {
            return new Scalar(null);
        }

        final Section section = new Section();
        for(final Field field : fields) {
            try {
                final Object fieldValue = field.get(value);
                final ValueNode node = fieldValue == null
                        ? new Scalar(null)
                        : registry.codecFor(ValueType.of(field.getGenericType(), registry.getLeafTypes())).encode(fieldValue);

                section.put(field.getName(), node);
            } catch(final IllegalAccessException e) {
                throw new CodecException("cannot read field " + field, e);
            }
        }

        return section;
    }

    @Override
    public Object decode(final ValueNode node) {
        if(node instanceof Scalar(final Object value) && value == null) {
            return null;
        }
        if(!(node instanceof final Section section)) {
            throw new CodecException("expected section for " + type.getSimpleName() + ", got " + node);
        }

        final Object instance = instantiate(type);
        for(final Field field : fields) {
            final ValueNode child = section.get(field.getName());
            if(child == null || (child instanceof Scalar(final Object value) && value == null)) {
                continue;  // absent/null -> keep constructor default
            }

            try {
                field.set(instance, registry.codecFor(ValueType.of(field.getGenericType(), registry.getLeafTypes())).decode(child));
            } catch(final IllegalAccessException e) {
                throw new CodecException("cannot write field " + field, e);
            }
        }

        return instance;
    }

}
