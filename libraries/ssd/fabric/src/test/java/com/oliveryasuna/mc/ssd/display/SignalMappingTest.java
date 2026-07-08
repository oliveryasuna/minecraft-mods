package com.oliveryasuna.mc.ssd.display;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalMappingTest {

    //==================================================
    // Methods
    //==================================================

    private SignalMappingTest() {
        super();
    }

    //==================================================
    // Tests
    //==================================================

    @Test
    void digitBlanksAtZero() {
        assertTrue(DigitMapping.INSTANCE.glyphIndex(0).isEmpty());
    }

    @Test
    void digitMapsOneThroughTenToZeroThroughNine() {
        for(int signal = 1; signal <= 10; signal++) {
            assertEquals(OptionalInt.of(signal - 1), DigitMapping.INSTANCE.glyphIndex(signal), "signal " + signal);
        }
    }

    @Test
    void digitClampsAboveTen() {
        for(int signal = 11; signal <= 15; signal++) {
            assertEquals(OptionalInt.of(9), DigitMapping.INSTANCE.glyphIndex(signal), "signal " + signal);
        }
    }

    @Test
    void hexBlanksAtZero() {
        assertTrue(HexLetterMapping.INSTANCE.glyphIndex(0).isEmpty());
    }

    @Test
    void hexMapsOneThroughSixToLettersAThroughF() {
        // Glyph indices 10..15 are A b C d E F.
        for(int signal = 1; signal <= 6; signal++) {
            assertEquals(OptionalInt.of(9 + signal), HexLetterMapping.INSTANCE.glyphIndex(signal), "signal " + signal);
        }
    }

    @Test
    void hexClampsAboveSix() {
        for(int signal = 7; signal <= 15; signal++) {
            assertEquals(OptionalInt.of(15), HexLetterMapping.INSTANCE.glyphIndex(signal), "signal " + signal);
        }
    }

    @Test
    void everyMappedIndexIsAValidGlyph() {
        for(int signal = 0; signal <= 15; signal++) {
            DigitMapping.INSTANCE.glyphIndex(signal).ifPresent(Glyphs::get);
            HexLetterMapping.INSTANCE.glyphIndex(signal).ifPresent(Glyphs::get);
        }
    }

}
