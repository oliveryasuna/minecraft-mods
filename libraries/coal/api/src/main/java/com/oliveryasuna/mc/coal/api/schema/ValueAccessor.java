package com.oliveryasuna.mc.coal.api.schema;

public interface ValueAccessor {

    //==================================================
    // Methods
    //==================================================

    Object read(Object instance);

    void write(Object instance, Object value);

    Class<?> declaredType();

}
