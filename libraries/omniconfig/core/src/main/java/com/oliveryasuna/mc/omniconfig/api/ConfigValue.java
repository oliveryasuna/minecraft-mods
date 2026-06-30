package com.oliveryasuna.mc.omniconfig.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Typed view of a single config entry by dotted path.
 * <p>
 * Reads from the manager's current snapshot; subscribes to changes for that
 * path. Use boxes types ({@code Integer.class}, not {@code int.class}).
 *
 * @param <V> The type of value.
 */
public class ConfigValue<V> {

    //==================================================
    // Fields
    //==================================================

    private final ConfigManager<?> manager;
    private final String path;
    private final Class<V> type;

    //==================================================
    // Constructors
    //==================================================

    public ConfigValue(
            final ConfigManager<?> manager,
            final String path,
            final Class<V> type
    ) {
        super();

        this.manager = manager;
        this.path = path;
        this.type = type;
    }

    //==================================================
    // Methods
    //==================================================

    public V get() {
        return type.cast(manager.getSnapshot().get(path));
    }

    /**
     * Sets this value (validation), then persists the config.
     *
     * @param value The value.
     */
    public void set(final V value) {
        manager.set(path, value);
        try {
            manager.save();
        } catch(final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void onChange(final Consumer<V> listener) {
        manager.getEvents().subscribe(path, event -> listener.accept(type.cast(event.newValue())));
    }

    //==================================================
    // Getters/setters
    //==================================================

    public String getPath() {
        return path;
    }

}
