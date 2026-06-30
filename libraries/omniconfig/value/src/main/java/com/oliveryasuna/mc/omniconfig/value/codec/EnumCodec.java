package com.oliveryasuna.mc.omniconfig.value.codec;

import com.oliveryasuna.mc.omniconfig.value.Scalar;
import com.oliveryasuna.mc.omniconfig.value.ValueCodec;
import com.oliveryasuna.mc.omniconfig.value.ValueNode;

/**
 * Encodes an enum as its constant name; decodes by name.
 */
public final class EnumCodec implements ValueCodec<Object> {

    //==================================================
    // Fields
    //==================================================

    private final Class<?> type;

    //==================================================
    // Constructors
    //==================================================

    public EnumCodec(final Class<?> type) {
        super();

        this.type = type;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValueNode encode(final Object value) {
        return new Scalar(((Enum<?>)value).name());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object decode(final ValueNode node) {
        if(!(node instanceof Scalar(final Object value)) || !(value instanceof final String name)) {
            throw new IllegalArgumentException("Expected enum name for " + type.getSimpleName());
        }

        try {
            return Enum.valueOf((Class)type, name);
        } catch(final IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown constant '" + name + "' for " + type.getSimpleName());
        }
    }

}
