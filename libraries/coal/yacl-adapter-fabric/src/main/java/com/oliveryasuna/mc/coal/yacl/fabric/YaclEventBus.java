package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.mc.coal.api.event.ChangeEvent;
import com.oliveryasuna.mc.coal.api.event.ChangeListener;
import com.oliveryasuna.mc.coal.api.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link EventBus} backed by a {@link CopyOnWriteArrayList} of listeners.
 * Path-prefix subscribers get a synthetic wrapper listener that filters events
 * to matches — segment-boundary aware per spec §10.1 (subscribing to
 * {@code "gui"} matches {@code "gui.foo"} but NOT {@code "guildhall"}).
 */
final class YaclEventBus implements EventBus {

    //==================================================
    // Fields
    //==================================================

    private final List<ChangeListener> listeners;

    //==================================================
    // Constructors
    //==================================================

    YaclEventBus() {
        super();

        this.listeners = new CopyOnWriteArrayList<>();
    }

    //==================================================
    // Methods
    //==================================================

    // EventBus
    //--------------------------------------------------

    @Override
    public Registration subscribe(final ChangeListener listener) {
        listeners.add(listener);

        return new RegistrationImpl(listeners, listener);
    }

    @Override
    public Registration subscribe(
            final String pathPrefix,
            final ChangeListener listener
    ) {
        final ChangeListener filtered = event -> {
            final String p = event.path();
            if(p.equals(pathPrefix) || p.startsWith(pathPrefix + ".")) {
                listener.onChange(event);
            }
        };
        listeners.add(filtered);

        return new RegistrationImpl(listeners, filtered);
    }

    @Override
    public void dispatch(final ChangeEvent event) {
        final List<ChangeListener> snapshot = new ArrayList<>(listeners);
        for(final ChangeListener l : snapshot) {
            try {
                l.onChange(event);
            } catch(final RuntimeException ignored) {
                // Per spec §10.3: providers MAY catch and log unchecked
                // exceptions from listeners.
            }
        }
    }

    //==================================================
    // Nested
    //==================================================

    private static final class RegistrationImpl implements Registration {

        //==================================================
        // Fields
        //==================================================

        private final List<ChangeListener> listeners;
        private final ChangeListener listener;
        private final AtomicBoolean closed;

        //==================================================
        // Constructors
        //==================================================

        RegistrationImpl(
                final List<ChangeListener> listeners,
                final ChangeListener listener
        ) {
            super();

            this.listeners = listeners;
            this.listener = listener;
            this.closed = new AtomicBoolean(false);
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public void close() {
            if(closed.compareAndSet(false, true)) {
                listeners.remove(listener);
            }
        }

    }

}
