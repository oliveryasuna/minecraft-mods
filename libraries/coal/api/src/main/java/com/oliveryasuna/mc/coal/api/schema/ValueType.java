package com.oliveryasuna.mc.coal.api.schema;

import java.util.List;
import java.util.Optional;

public interface ValueType {

    //==================================================
    // Methods
    //==================================================

    Kind kind();

    Class<?> rawType();

    Optional<ValueType> elementType();

    Optional<ValueType> valueType();

    List<SchemaEntry> children();

    //==================================================
    // Nested
    //==================================================

    enum Kind {

        //==================================================
        // Values
        //==================================================

        SCALAR,

        ENUM,

        LIST,

        MAP,

        OBJECT

    }

}
