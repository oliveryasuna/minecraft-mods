package com.oliveryasuna.mc.ssd.display;

/**
 * A single displayable symbol on a seven-segment display.
 *
 * @param index  the {@code VALUE} block-state index (0-15).
 * @param symbol the character it represents ({@code 0}-{@code 9},
 *               {@code A b C d E F}); for tooltips/debugging.
 * @param sprite the texture base name under {@code textures/block/} (without
 *               variant suffix).
 */
public record Glyph(
        int index,
        char symbol,
        String sprite
) {

    //==================================================
    // Constructors
    //==================================================

    public Glyph {
        if(index < 0) {
            throw new IllegalArgumentException("Glyph index must be >= 0: " + index);
        }
    }

}
