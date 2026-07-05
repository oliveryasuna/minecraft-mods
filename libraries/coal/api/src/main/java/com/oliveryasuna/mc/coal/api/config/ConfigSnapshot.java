package com.oliveryasuna.mc.coal.api.config;

import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface ConfigSnapshot {

    //==================================================
    // Methods
    //==================================================

    Instant capturedAt();

    Schema schema();

    <T> Optional<T> get(
            String dottedPath,
            Class<T> type
    );

    Object getRaw(String dottedPath);

    Set<String> paths();

    boolean isPresent(String dottedPath);

}
