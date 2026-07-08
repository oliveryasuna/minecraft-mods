package com.oliveryasuna.mc.ssd.grid;

/**
 * The dimensions of a display grid, in cells.
 */
public record GridSize(
        int width,
        int height
) {

    //==================================================
    // Constructors
    //==================================================

    public GridSize {
        if((width < 1) || (height < 1)) {
            throw new IllegalArgumentException("GridSize must be positive: " + width + "x" + height);
        }
    }

    //==================================================
    // Methods
    //==================================================

    public int area() {
        return width * height;
    }

    public boolean isSquare() {
        return width == height;
    }

    public boolean isMultiblock() {
        return area() > 1;
    }

    public boolean withinBound(final int max) {
        return (width <= max) && (height <= max);
    }

}
