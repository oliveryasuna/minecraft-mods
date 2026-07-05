package com.oliveryasuna.mc.coal.api.spi;

import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.io.ConfigIO;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.schema.SchemaReader;
import com.oliveryasuna.mc.coal.api.validation.Corrector;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ConfigProvider {

    //==================================================
    // Methods
    //==================================================

    // Identity
    //--------------------------------------------------

    String name();

    // Registration
    //--------------------------------------------------

    <S> ConfigHandle<S> register(
            Class<S> type,
            MigrationSpec migrations
    );

    ConfigHandle<Map<String, Object>> register(
            ConfigSpec spec,
            MigrationSpec migrations
    );

    // SPIs
    //--------------------------------------------------

    Platform platform();

    SchemaReader schemaReader();

    Corrector corrector();

    ConfigIO defaultIO();

    // Accessors
    //--------------------------------------------------

    Set<String> registeredConfigIds();

    Optional<ConfigHandle<?>> getById(String id);

    // Support
    //--------------------------------------------------

    boolean supports(Capability capability);

}
