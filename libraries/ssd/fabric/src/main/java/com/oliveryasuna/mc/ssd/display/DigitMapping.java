package com.oliveryasuna.mc.ssd.display;

import java.util.OptionalInt;

/**
 * Decimal mapping: signal 0 blanks; signals 1-10 show digits 0-9
 * ({@code digit == signal - 1}); signals 11-15 clamp to 9.
 */
public final class DigitMapping implements SignalMapping {

    //==================================================
    // Static fields
    //==================================================

    public static final DigitMapping INSTANCE = new DigitMapping();

    private static final int MAX_DIGIT = 9;

    //==================================================
    // Constructors
    //==================================================

    private DigitMapping() {
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

        return OptionalInt.of(Math.min(signal - 1, MAX_DIGIT));
    }

}
