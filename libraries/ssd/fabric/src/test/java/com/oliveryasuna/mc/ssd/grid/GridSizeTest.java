package com.oliveryasuna.mc.ssd.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridSizeTest {

    //==================================================
    // Constructors
    //==================================================

    private GridSizeTest() {
        super();
    }

    //==================================================
    // Tests
    //==================================================

    @Test
    void area() {
        assertEquals(9, new GridSize(3, 3).area());
        assertEquals(6, new GridSize(2, 3).area());
    }

    @Test
    void isSquare() {
        assertTrue(new GridSize(3, 3).isSquare());
        assertFalse(new GridSize(2, 3).isSquare());
    }

    @Test
    void isMultiblock() {
        assertFalse(new GridSize(1, 1).isMultiblock());
        assertTrue(new GridSize(2, 2).isMultiblock());
    }

    @Test
    void withinBound() {
        assertTrue(new GridSize(8, 8).withinBound(8));
        assertFalse(new GridSize(9, 9).withinBound(8));
    }

    @Test
    void rejectsNonPositiveDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new GridSize(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new GridSize(1, 0));
    }

}
