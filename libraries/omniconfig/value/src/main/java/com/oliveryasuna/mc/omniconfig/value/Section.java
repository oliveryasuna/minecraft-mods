package com.oliveryasuna.mc.omniconfig.value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An ordered, named grouping of child nodes (a sub-table).
 * <p>
 * Mutable while building.
 */
public final class Section implements ValueNode {

    //==================================================
    // Fields
    //==================================================

    private final Map<String, ValueNode> children;

    //==================================================
    // Constructors
    //==================================================

    public Section() {
        super();

        this.children = new LinkedHashMap<>();
    }

    //==================================================
    // Methods
    //==================================================

    public ValueNode get(final String key) {
        return children.get(key);
    }

    public boolean has(final String key) {
        return children.containsKey(key);
    }

    public void put(
            final String key,
            final ValueNode node
    ) {
        children.put(key, node);
    }

    public int size() {
        return children.size();
    }

    public Set<String> keys() {
        return children.keySet();
    }

    public Map<String, ValueNode> asMap() {
        return children;
    }

}
