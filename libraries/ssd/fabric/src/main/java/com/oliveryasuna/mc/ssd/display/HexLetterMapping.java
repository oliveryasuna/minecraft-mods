package com.oliveryasuna.mc.ssd.display;

import java.util.OptionalInt;

/**
 * Hex-letter mapping: signal 0 blanks; signals 1-6 show letters A-F; signals
 * 7-15 clamp to F.
 */
public final class HexLetterMapping implements SignalMapping {

    //==================================================
    // Static fields
    //==================================================

    public static final HexLetterMapping INSTANCE = new HexLetterMapping();

    /**
     * {@link Glyphs} index of the first letter ('A').
     */
    private static final int FIRST_LETTER = 10;

    /**
     * Number of letters (A-F).
     */
    private static final int LETTER_COUNT = 6;

    //==================================================
    // Constructors
    //==================================================

    private HexLetterMapping() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public OptionalInt glyphIndex(final int signal) {
        if(signal <= 0) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(FIRST_LETTER + Math.min(signal - 1, LETTER_COUNT - 1));
    }

}
