package com.oliveryasuna.mc.coal.api.event;

public interface EventBus {

    //==================================================
    // Methods
    //==================================================

    Registration subscribe(ChangeListener listener);

    Registration subscribe(
            String pathPrefix,
            ChangeListener listener
    );

    /**
     * Dispatches a {@link ChangeEvent} to all registered listeners.
     * <p>
     * Provider-internal.
     */
    void dispatch(ChangeEvent event);

    //==================================================
    // Nested
    //==================================================

    interface Registration extends AutoCloseable {

        //==================================================
        // Methods
        //==================================================

        @Override
        void close();

    }

}
