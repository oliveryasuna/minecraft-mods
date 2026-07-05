package com.oliveryasuna.mc.coal.api.schema;

import com.oliveryasuna.mc.coal.api.Format;

import java.util.Optional;
import java.util.Set;

public interface Schema {

    //==================================================
    // Methods
    //==================================================

    Class<?> type();

    String id();

    String name();

    Format format();

    int version();

    SchemaCategory root();

    Optional<SchemaEntry> find(String dottedPath);

    Set<String> paths();

}
