package com.oliveryasuna.mc.omniconfig.value;

import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.schema.SchemaCategory;
import com.oliveryasuna.mc.omniconfig.schema.SchemaEntry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable, dotted-path view of a config's values captured at a point in
 * time.
 * <p>
 * {@code ConfigHandle#get()} reads from the current snapshot; the reload
 * controller swaps in a new one atomically.
 */
public final class ConfigSnapshot {

    //==================================================
    // Static methods
    //==================================================

    public static ConfigSnapshot capture(
            final Schema schema,
            final Object instance
    ) {
        final Map<String, Object> values = new LinkedHashMap<>();
        walk(schema.root(), instance, "", values);
        return new ConfigSnapshot(schema, values);
    }

    private static void walk(
            final SchemaCategory category,
            final Object root,
            final String prefix,
            final Map<String, Object> out
    ) {
        for(final SchemaEntry entry : category.entries()) {
            out.put(prefix + entry.getKey(), entry.readFrom(root));
        }
        for(final SchemaCategory sub : category.categories()) {
            walk(sub, root, prefix + sub.getName() + ".", out);
        }
    }

    //==================================================
    // Fields
    //==================================================

    private final Schema schema;
    private final Map<String, Object> values;

    //==================================================
    // Constructors
    //==================================================

    private ConfigSnapshot(
            final Schema schema,
            final Map<String, Object> values
    ) {
        this.schema = schema;
        // TODO: Map.copyOf?
        this.values = Collections.unmodifiableMap(values);
    }

    //==================================================
    // Methods
    //==================================================

    public Object get(final String path) {
        return values.get(path);
    }

    public boolean contains(final String path) {
        return values.containsKey(path);
    }

    public Map<String, Object> asMap() {
        // TODO: Unmodifiable?
        return values;
    }

    //==================================================
    // Getters/setters
    //==================================================

    public Schema getSchema() {
        return schema;
    }

}
