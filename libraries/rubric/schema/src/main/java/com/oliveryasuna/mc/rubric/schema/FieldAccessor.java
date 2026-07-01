package com.oliveryasuna.mc.rubric.schema;

import com.oliveryasuna.mc.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Reflective accessor: walks an owner path from the root POJO, then
 * reads/writes a field.
 */
public final class FieldAccessor implements ValueAccessor {

    //==================================================
    // Static methods
    //==================================================

    private static Object instantiate(final Class<?> type) {
        return ReflectionUtil.instantiate(type, e -> new IllegalStateException("Cannot instantiate owner " + type.getName(), e));
    }


    //==================================================
    // Fields
    //==================================================

    private final List<Field> ownerPath;
    private final Field field;

    //==================================================
    // Constructors
    //==================================================

    public FieldAccessor(
            final List<Field> ownerPath,
            final Field field
    ) {
        super();

        this.ownerPath = List.copyOf(ownerPath);
        this.field = field;

        this.field.setAccessible(true);
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Object read(final Object state) {
        final Object owner = resolveOwner(state, false);
        if(owner == null) {
            return null;
        }

        try {
            return field.get(owner);
        } catch(final IllegalAccessException e) {
            throw new IllegalStateException("Cannot read field " + field, e);
        }

    }

    @Override
    public void write(
            final Object state,
            final Object value
    ) {
        try {
            field.set(resolveOwner(state, true), value);
        } catch(final IllegalAccessException e) {
            throw new IllegalStateException("Cannot write field " + field, e);
        }

    }

    private Object resolveOwner(
            final Object root,
            final boolean createMissing
    ) {
        Object curr = root;
        for(final Field link : ownerPath) {
            try {
                link.setAccessible(true);
                Object next = link.get(curr);
                if(next == null) {
                    if(!createMissing) {
                        return null;
                    }
                    next = instantiate(link.getType());
                    link.set(curr, next);
                }
                curr = next;
            } catch(final IllegalAccessException e) {
                throw new IllegalStateException("Cannot traverse owner field " + link, e);
            }
        }

        return curr;
    }

    //==================================================
    // Getters/setters
    //==================================================

    public List<Field> getOwnerPath() {
        return ownerPath;
    }

    public Field getField() {
        return field;
    }

}
