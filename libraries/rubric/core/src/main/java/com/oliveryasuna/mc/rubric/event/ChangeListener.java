package com.oliveryasuna.mc.rubric.event;

/**
 * Notified when a config value changes.
 */
@FunctionalInterface
public interface ChangeListener {

    //==================================================
    // Methods
    //==================================================

    void onChange(ChangeEvent event);

}
