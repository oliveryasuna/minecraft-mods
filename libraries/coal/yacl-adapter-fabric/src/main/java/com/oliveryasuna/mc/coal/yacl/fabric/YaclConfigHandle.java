package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.config.ConfigManager;
import com.oliveryasuna.mc.coal.api.config.ConfigSnapshot;
import com.oliveryasuna.mc.coal.api.config.ConfigValue;

import java.io.IOException;

/**
 * Thin façade over {@link YaclConfigManager}. Each method delegates.
 */
final class YaclConfigHandle<S> implements ConfigHandle<S> {

    //==================================================
    // Fields
    //==================================================

    private final YaclConfigManager<S> manager;

    //==================================================
    // Constructors
    //==================================================

    YaclConfigHandle(final YaclConfigManager<S> manager) {
        super();

        this.manager = manager;
    }

    //==================================================
    // ConfigHandle
    //==================================================

    @Override
    public S get() {
        return manager.get();
    }

    @Override
    public void set(
            final String dottedPath,
            final Object value
    ) {
        manager.set(dottedPath, value);
    }

    @Override
    public void reload() throws IOException {
        manager.load();
    }

    @Override
    public void save() throws IOException {
        manager.save();
    }

    @Override
    public ConfigManager<S> manager() {
        return manager;
    }

    @Override
    public ConfigSnapshot snapshot() {
        return manager.snapshot();
    }

    @Override
    public <T> ConfigValue<T> value(
            final String dottedPath,
            final Class<T> type
    ) {
        return new YaclConfigValue<>(manager, dottedPath, type);
    }

}
