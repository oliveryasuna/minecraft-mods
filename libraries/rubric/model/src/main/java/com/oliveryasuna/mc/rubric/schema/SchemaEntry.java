package com.oliveryasuna.mc.rubric.schema;

import com.oliveryasuna.mc.rubric.value.ValueType;

import java.util.Objects;

/**
 * A single leaf config value: key, type, default, metadata, and how to
 * read/write it.
 */
public final class SchemaEntry {

    //==================================================
    // Fields
    //==================================================

    private final String key;
    private final ValueType type;
    private final Object defaultValue;
    private final EntryMetadata metadata;
    private final ValueAccessor accessor;

    //==================================================
    // Constructors
    //==================================================

    public SchemaEntry(
            final String key,
            final ValueType type,
            final Object defaultValue,
            final EntryMetadata metadata,
            final ValueAccessor accessor
    ) {
        super();

        this.key = Objects.requireNonNull(key, "key");
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = defaultValue;
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.accessor = Objects.requireNonNull(accessor, "accessor");
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Reads this entry's value from a config state (POJO or map).
     *
     * @param state The config state.
     * @return The value.
     */
    public Object readFrom(final Object state) {
        return accessor.read(state);
    }

    /**
     * Writes this entry's value onto a config state (POJO or map).
     *
     * @param state The config state.
     * @param value The value to write.
     */
    public void writeTo(
            final Object state,
            final Object value
    ) {
        accessor.write(state, value);
    }

    //==================================================
    // Getters/setters
    //==================================================

    public String getKey() {
        return key;
    }

    public ValueType getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public EntryMetadata getMetadata() {
        return metadata;
    }

    public ValueAccessor getAccessor() {
        return accessor;
    }

}
