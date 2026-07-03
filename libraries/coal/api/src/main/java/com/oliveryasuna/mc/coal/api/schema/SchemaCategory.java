package com.oliveryasuna.mc.coal.api.schema;

import java.util.List;
import java.util.Optional;

public interface SchemaCategory {

    //==================================================
    // Methods
    //==================================================

    String name();

    List<String> comment();

    List<SchemaEntry> entries();

    List<SchemaCategory> categories();

    Optional<SchemaCategory> category(String name);

    Optional<SchemaEntry> entry(String key);

}
