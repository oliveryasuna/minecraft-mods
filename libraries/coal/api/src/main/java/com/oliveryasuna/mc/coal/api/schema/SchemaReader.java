package com.oliveryasuna.mc.coal.api.schema;

import com.oliveryasuna.mc.coal.api.config.ConfigSpec;

import java.util.Map;

public interface SchemaReader {

    //==================================================
    // Methods
    //==================================================

    <S> ConfigModel<S> read(Class<S> type);

    ConfigModel<Map<String, Object>> read(ConfigSpec spec);

}
