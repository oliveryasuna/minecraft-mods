package com.oliveryasuna.mc.rubric.value;

/**
 * The root of a parsed config: a single top-level {@link Section}.
 */
public record ValueTree(
        Section root
) {

    //==================================================
    // Static methods
    //==================================================

    public static ValueTree empty() {
        return new ValueTree(new Section());
    }

}
