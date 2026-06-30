package com.oliveryasuna.mc.omniconfig.schema;

import java.util.Map;

/**
 * Map-backed accessor for builder-defined configs: the state is a flat map
 * keyed by dotted path.
 */
public final class MapAccessor implements ValueAccessor {

    //==================================================
    // Fields
    //==================================================

    private final String path;

    //==================================================
    // Constructors
    //==================================================

    public MapAccessor(final String path) {
        super();

        this.path = path;
    }

    //==================================================
    // Methods
    //==================================================

    @SuppressWarnings("unchecked")
    @Override
    public Object read(final Object state) {
        return ((Map<String, Object>)state).get(path);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(
            final Object state,
            final Object value
    ) {
        ((Map<String, Object>)state).put(path, value);
    }

    //==================================================
    // Getters/setters
    //==================================================

    public String getPath() {
        return path;
    }

}
