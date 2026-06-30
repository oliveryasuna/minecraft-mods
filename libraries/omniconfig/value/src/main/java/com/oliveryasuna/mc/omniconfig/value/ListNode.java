package com.oliveryasuna.mc.omniconfig.value;

import java.util.List;

/**
 * An ordered sequence of nodes.
 */
public record ListNode(
        List<ValueNode> items
) implements ValueNode {

    //==================================================
    // Constructors
    //==================================================

    public ListNode {
        items = List.copyOf(items);
    }

}
