package com.oliveryasuna.mc.rubric.core;

import com.oliveryasuna.mc.rubric.event.ChangeListener;
import com.oliveryasuna.mc.rubric.lifecycle.LoadResult;
import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.value.ConfigSnapshot;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Public handle to a registered config: read values, reload, save, observe
 * changes.
 *
 * @param <T> The config type.
 */
public final class ConfigHandle<T> {

    //==================================================
    // Fields
    //==================================================

    private final ConfigManager<T> manager;

    //==================================================
    // Constructors
    //==================================================

    public ConfigHandle(final ConfigManager<T> manager) {
        super();

        this.manager = manager;
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * The live config instance (POJO).
     *
     * @return The config instance.
     */
    public T get() {
        return manager.get();
    }

    /**
     * Sets a value (validated), then persists.
     * <p>
     * Throws on unknown path or constraint violation.
     *
     * @param path  The value path, e.g. {@code "database.host"}.
     * @param value The value.
     */
    public void set(
            final String path,
            final Object value
    ) {
        manager.set(path, value);
        save();
    }

    public ConfigSnapshot snapshot() {
        return manager.getSnapshot();
    }

    public Schema schema() {
        return manager.getSchema();
    }

    public LoadResult reload() {
        try {
            return manager.reload();
        } catch(final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void save() {
        try {
            manager.save();
        } catch(final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void onChange(final ChangeListener listener) {
        manager.getEvents().subscribe(listener);
    }

    public <V> ConfigValue<V> value(
            final String path,
            final Class<V> type
    ) {
        return new ConfigValue<>(manager, path, type);
    }

}
