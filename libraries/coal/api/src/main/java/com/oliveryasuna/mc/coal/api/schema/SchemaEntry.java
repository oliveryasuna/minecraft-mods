package com.oliveryasuna.mc.coal.api.schema;

public interface SchemaEntry {

    //==================================================
    // Methods
    //==================================================

    String key();

    ValueType type();

    Object defaultValue();

    EntryMetadata metadata();

    ValueAccessor accessor();

    Object readFrom(Object instance);

    void writeTo(
            Object instance,
            Object value
    );

}
