package com.oliveryasuna.mc.omniconfig.value.codec;

import com.oliveryasuna.mc.omniconfig.value.ListNode;
import com.oliveryasuna.mc.omniconfig.value.ValueCodec;
import com.oliveryasuna.mc.omniconfig.value.ValueNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes/decodes a {@link List} element-wise using the element codec.
 */
public final class ListCodec implements ValueCodec<Object> {

    //==================================================
    // Fields
    //==================================================

    private final ValueCodec<Object> elementCodec;

    //==================================================
    // Constructors
    //==================================================

    public ListCodec(final ValueCodec<Object> elementCodec) {
        super();

        this.elementCodec = elementCodec;
    }

    @Override
    public ValueNode encode(final Object value) {
        final List<ValueNode> items = new ArrayList<>();
        for(final Object obj : (List<?>)value) {
            items.add(elementCodec.encode(obj));
        }

        return new ListNode(items);
    }

    @Override
    public Object decode(final ValueNode node) {
        if(!(node instanceof ListNode(final List<ValueNode> items))) {
            throw new IllegalArgumentException("Expected list, got " + node);
        }

        final List<Object> output = new ArrayList<>();
        for(final ValueNode n : items) {
            output.add(elementCodec.decode(n));
        }

        return output;
    }

}
