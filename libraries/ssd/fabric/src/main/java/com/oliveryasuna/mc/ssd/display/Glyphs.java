package com.oliveryasuna.mc.ssd.display;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;

/**
 * The fixed table of glyphs a seven-segment display can render, indexed by the
 * {@code VALUE} block-state property. Indices 0-9 are the digits; 10-15 are the
 * hex letters {@code A b C d E F} (lower-case {@code b}/{@code d} because
 * upper-case B/D are indistinguishable from 8/0 on a seven-segment display).
 */
public final class Glyphs {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Total glyph count (also the size of the {@code VALUE} property range,
     * 0..COUNT-1).
     */
    public static final int COUNT = 16;

    private static final char[] SYMBOLS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'b', 'C', 'd', 'E', 'F'};

    private static final Glyph[] TABLE = build();

    //==================================================
    // Static methods
    //==================================================

    public static Glyph get(final int index) {
        if((index < 0) || (index >= COUNT)) {
            throw new IndexOutOfBoundsException("Glyph index out of range [0, " + COUNT + "): " + index);
        }

        return TABLE[index];
    }

    private static Glyph[] build() {
        final Glyph[] table = new Glyph[COUNT];

        for(int i = 0; i < COUNT; i++) {
            table[i] = new Glyph(i, SYMBOLS[i], "digit_" + i);
        }

        return table;
    }

    //==================================================
    // Constructors
    //==================================================

    private Glyphs() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
