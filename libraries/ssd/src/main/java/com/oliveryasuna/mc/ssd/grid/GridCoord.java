package com.oliveryasuna.mc.ssd.grid;

/**
 * A cell position within a display grid: {@code col} increases to the viewer's
 * right, {@code row} increases downward. {@code (0, 0)} is the top-left
 * (controller) cell.
 */
public record GridCoord(int col, int row) {

    public static final GridCoord ORIGIN = new GridCoord(0, 0);

}
