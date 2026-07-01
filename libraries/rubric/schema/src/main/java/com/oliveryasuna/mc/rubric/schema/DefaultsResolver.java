package com.oliveryasuna.mc.rubric.schema;

import com.oliveryasuna.mc.util.ReflectionUtil;

import java.lang.reflect.Field;

/**
 * Instantiates config POJOs via their no-arg constructor and reads field
 * defaults.
 */
public final class DefaultsResolver {

    //==================================================
    // Constructors
    //==================================================

    public DefaultsResolver() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Creates a default instance of {@code type} via its (possibly non-public)
     * no-arg constructor.
     *
     * @param type The type to instantiate.
     * @return The default instance of {@code type}.
     */
    public Object instantiate(final Class<?> type) {
        return ReflectionUtil.instantiate(type, e -> new IllegalArgumentException("Config type " + type.getName() + " must  have a no-arg constructor", e));
    }

    /**
     * Reads the value of {@code field} from {@code owner}.
     *
     * @param field The field to read.
     * @param owner The owner of the field.
     * @return The value of the field.
     */
    public Object read(
            final Field field,
            final Object owner
    ) {
        try {
            field.setAccessible(true);

            return field.get(owner);
        } catch(final IllegalAccessException e) {
            throw new IllegalStateException("Cannot read defaults for " + field, e);
        }
    }

}
