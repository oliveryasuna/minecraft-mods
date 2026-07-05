package com.oliveryasuna.mc.coal.api.config;

import java.util.function.Consumer;

public interface ConfigValue<T> {

    //==================================================
    // Methods
    //==================================================

    T get();

    void set(T value);

    void onChange(Consumer<T> listener);

    String path();

    Class<T> type();

}
