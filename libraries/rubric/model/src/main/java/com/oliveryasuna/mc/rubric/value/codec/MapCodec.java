package com.oliveryasuna.mc.rubric.value.codec;

import com.oliveryasuna.mc.rubric.value.Section;
import com.oliveryasuna.mc.rubric.value.ValueCodec;
import com.oliveryasuna.mc.rubric.value.ValueNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encodes/decodes a {@code Map<String, ?>} as a section using the value codec.
 */
public final class MapCodec implements ValueCodec<Object> {

    //==================================================
    // Fields
    //==================================================

    private final ValueCodec<Object> valueCodec;

    //==================================================
    // Constructors
    //==================================================

    public MapCodec(final ValueCodec<Object> valueCodec) {
        super();

        this.valueCodec = valueCodec;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ValueNode encode(final Object value) {
        final Section section = new Section();
        for(final Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
            section.put(String.valueOf(entry.getKey()), valueCodec.encode(entry.getValue()));
        }

        return section;
    }

    @Override
    public Object decode(final ValueNode node) {
        if(!(node instanceof final Section section)) {
            throw new IllegalArgumentException("Expected section, got " + node);
        }

        final Map<String, Object> output = new LinkedHashMap<>();
        for(final String key : section.keys()) {
            output.put(key, valueCodec.decode(section.get(key)));
        }

        return output;
    }

}
