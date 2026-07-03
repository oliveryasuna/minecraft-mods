package com.oliveryasuna.mc.coal.api.config;

import com.oliveryasuna.mc.coal.api.event.EventBus;
import com.oliveryasuna.mc.coal.api.event.ReloadListener;
import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public interface ConfigManager<S> {

    //==================================================
    // Methods
    //==================================================

    Schema schema();

    S get();

    void set(
            String dottedPath,
            Object value
    );

    LoadResult load() throws IOException;

    void save() throws IOException;

    Path file();

    EventBus events();

    void addReloadListener(ReloadListener<S> listener);

    Origin originOf(String dottedPath);

    void markOrigins(
            Collection<String> paths,
            Origin origin
    );

    ConfigSnapshot snapshot();

}
