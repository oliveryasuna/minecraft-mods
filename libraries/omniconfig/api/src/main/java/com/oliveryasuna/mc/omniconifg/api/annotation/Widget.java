package com.oliveryasuna.mc.omniconifg.api.annotation;

import java.lang.annotation.*;

/**
 * Overrides the control used to edit an entry in the generated GUI.
 * <p>
 * When absent (or {@link Type#AUTO}), the control is inferred from the entry's
 * type and its other constraints (e.g., a {@link Range} numeric becomes a
 * slider).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Widget {

    //==================================================
    // Properties
    //==================================================

    /**
     * Desired control.
     * <p>
     * Defaults to {@link Type#AUTO}.
     */
    Type value() default Type.AUTO;

    /**
     * Whether the GUI is allowed to commit invalid edits to disk on save.
     * <p>
     * When {@code false} (default) and the widget is in an invalid state (e.g..
     * a {@code TextField}'s text fails the entry's {@link Pattern @Pattern} or
     * codec decode), the GUI's "Save & exit" is blocked: a
     * {@code ConfirmScreen} lists the offending entry paths and the screen
     * stays open. Set {@code true} to allow the user to save over invalid input
     * (the underlying field keeps its last valid value; load-time correction
     * handles cleanup on reload).
     */
    boolean allowInvalid() default false;

    //==================================================
    // Nested
    //==================================================

    /**
     * Built-in control types.
     */
    enum Type {

        //==================================================
        // Values
        //==================================================

        /**
         * Infer from the entry's data type and constraints.
         */
        AUTO,

        /**
         * Boolean on/off button.
         */
        TOGGLE,

        /**
         * Slider; requires a bounded {@link Range}.
         */
        SLIDER,

        /**
         * Free-form numeric input.
         */
        NUMBER_FIELD,

        /**
         * Free-form text input.
         */
        TEXT_FIELD,

        /**
         * Dropdown; used for enums and {@link OneOf}.
         */
        DROPDOWN,

        /**
         * Color picker; backed by a {@code "#RRGGBB"} string.
         */
        COLOR

    }

}
