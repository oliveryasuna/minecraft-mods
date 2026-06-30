package com.oliveryasuna.mc.util;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;

import java.lang.reflect.Constructor;
import java.util.function.Function;

public final class ReflectionUtil {

    //==================================================
    // Static methods
    //==================================================

    public static Object instantiate(
            final Class<?> type,
            final Function<Throwable, RuntimeException> exceptionFactory
    ) {
        try {
            final Constructor<?> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch(final ReflectiveOperationException e) {
            throw exceptionFactory.apply(e);
        }
    }

    //==================================================
    // Constructors
    //==================================================

    private ReflectionUtil() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
