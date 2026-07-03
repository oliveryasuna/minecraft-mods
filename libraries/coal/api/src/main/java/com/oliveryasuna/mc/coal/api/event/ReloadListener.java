package com.oliveryasuna.mc.coal.api.event;

@FunctionalInterface
public interface ReloadListener<S> {

    //==================================================
    // Methods
    //==================================================

    void onReload(
            S previous,
            S current
    );

}
