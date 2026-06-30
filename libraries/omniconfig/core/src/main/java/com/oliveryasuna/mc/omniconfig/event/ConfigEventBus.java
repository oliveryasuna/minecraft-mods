package com.oliveryasuna.mc.omniconfig.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatches {@link ChangeEvent}s to global and per-path listeners.
 * <p>
 * Thread-safe.
 */
public final class ConfigEventBus {

    //==================================================
    // Fields
    //==================================================

    private final List<ChangeListener> global;
    private final Map<String, List<ChangeListener>> byPath;

    //==================================================
    // Constructors
    //==================================================

    public ConfigEventBus() {
        super();

        this.global = new CopyOnWriteArrayList<>();
        this.byPath = new ConcurrentHashMap<>();
    }

    //==================================================
    // Methods
    //==================================================

    public void subscribe(final ChangeListener listener) {
        global.add(listener);
    }

    public void subscribe(
            final String path,
            final ChangeListener listener
    ) {
        byPath.computeIfAbsent(path, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void fire(final ChangeEvent event) {
        for(final ChangeListener listener : global) {
            listener.onChange(event);
        }

        final List<ChangeListener> scoped = byPath.get(event.path());
        if(scoped != null) {
            for(final ChangeListener listener : scoped) {
                listener.onChange(event);
            }
        }
    }

}
