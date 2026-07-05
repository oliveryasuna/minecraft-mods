package com.oliveryasuna.mc.coal.api.schema;

public interface ConfigModel<S> {

    //==================================================
    // Methods
    //==================================================

    Schema schema();

    S newState();

}
