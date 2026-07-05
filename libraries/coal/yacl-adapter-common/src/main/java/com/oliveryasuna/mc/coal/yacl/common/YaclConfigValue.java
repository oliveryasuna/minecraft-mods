package com.oliveryasuna.mc.coal.yacl.common;

import com.oliveryasuna.mc.coal.api.config.ConfigValue;

import java.util.function.Consumer;

/**
 * {@link ConfigValue} backed by a {@link YaclConfigManager}. Reads/writes
 * delegate to the manager; {@link #onChange(Consumer)} attaches a
 * path-prefixed listener.
 */
final class YaclConfigValue<T> implements ConfigValue<T> {

    //==================================================
    // Fields
    //==================================================

    private final YaclConfigManager<?> manager;
    private final String path;
    private final Class<T> type;

    //==================================================
    // Constructors
    //==================================================

    YaclConfigValue(
            final YaclConfigManager<?> manager,
            final String path,
            final Class<T> type
    ) {
        super();

        this.manager = manager;
        this.path = path;
        this.type = type;
    }

    //==================================================
    // ConfigValue
    //==================================================

    @Override
    public T get() {
        final Object raw = manager.rawAt(path);
        if(raw == null) {
            return null;
        } else if(!type.isInstance(raw)) {
            throw new ClassCastException("value at " + path + " has type " + raw.getClass().getName() + " — not assignable to " + type.getName());
        }

        return type.cast(raw);
    }

    @Override
    public void set(final T value) {
        manager.set(path, value);
    }

    @Override
    public void onChange(final Consumer<T> listener) {
        manager.events().subscribe(path, event -> {
            if(event.path().equals(path)) {
                final Object nv = event.newValue();
                if(nv == null) {
                    listener.accept(null);
                } else if(type.isInstance(nv)) {
                    listener.accept(type.cast(nv));
                }
            }
        });
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Class<T> type() {
        return type;
    }

}
