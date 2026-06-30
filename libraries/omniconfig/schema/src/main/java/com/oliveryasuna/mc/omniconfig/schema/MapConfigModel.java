package com.oliveryasuna.mc.omniconfig.schema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link ConfigModel} backed by a flat map keyed by dotted path (builder
 * configs).
 */
public final class MapConfigModel implements ConfigModel<Map<String, Object>> {

    //==================================================
    // Fields
    //==================================================

    private final Schema schema;

    //==================================================
    // Constructors
    //==================================================

    public MapConfigModel(final Schema schema) {
        super();

        this.schema = schema;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public Map<String, Object> newState() {
        final Map<String, Object> state = new LinkedHashMap<>();
        seed(schema.root(), state);

        return state;
    }

    private void seed(
            final SchemaCategory category,
            final Map<String, Object> state
    ) {
        for(final SchemaEntry entry : category.entries()) {
            entry.writeTo(state, entry.getDefaultValue());
        }
        for(final SchemaCategory sub : category.categories()) {
            seed(sub, state);
        }
    }

}
