package com.oliveryasuna.mc.coal.api.config;

import java.io.IOException;

public interface ConfigHandle<S> {

    //==================================================
    // Methods
    //==================================================

    S get();

    void set(
            String dottedPath,
            Object value
    );

    void reload() throws IOException;

    void save() throws IOException;

    ConfigManager<S> manager();

    ConfigSnapshot snapshot();

    <T> ConfigValue<T> value(
            String dottedPath,
            Class<T> type
    );

}
