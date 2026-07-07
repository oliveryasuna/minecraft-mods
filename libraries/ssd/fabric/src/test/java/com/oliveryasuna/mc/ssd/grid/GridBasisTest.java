package com.oliveryasuna.mc.ssd.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Invariant tests for the grid coordinate math — deliberately independent of which world direction
 * "right" resolves to, so they hold for any facing.
 */
class GridBasisTest {

    //==================================================
    // Static fields
    //==================================================

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    //==================================================
    // Constructors
    //==================================================

    private GridBasisTest() {
        super();
    }

    //==================================================
    // Tests
    //==================================================

    @Test
    void colRowInvertCell() {
        final BlockPos origin = new BlockPos(10, 64, -7);

        for(final Direction facing : HORIZONTALS) {
            final GridBasis basis = new GridBasis(facing);

            for(int col = 0; col < 5; col++) {
                for(int row = 0; row < 5; row++) {
                    final BlockPos cell = basis.cell(origin, col, row);

                    assertEquals(col, basis.col(origin, cell), facing + " col");
                    assertEquals(row, basis.row(origin, cell), facing + " row");
                }
            }
        }
    }

    @Test
    void originRecoversTopLeftFromAnyCell() {
        final BlockPos origin = new BlockPos(0, 70, 0);

        for(final Direction facing : HORIZONTALS) {
            final GridBasis basis = new GridBasis(facing);

            for(int col = 0; col < 4; col++) {
                for(int row = 0; row < 4; row++) {
                    final BlockPos cell = basis.cell(origin, col, row);

                    assertEquals(origin, basis.origin(cell, new GridCoord(col, row)), facing.toString());
                }
            }
        }
    }

    @Test
    void rightAxisIsPerpendicularToFacing() {
        for(final Direction facing : HORIZONTALS) {
            final GridBasis basis = new GridBasis(facing);

            // Columns run across the display face, never along the facing axis
            // or vertically.
            assertNotEquals(facing.getAxis(), basis.right().getAxis(), facing.toString());
            assertEquals(Direction.Axis.Y, basis.down().getAxis(), facing.toString());
        }
    }

}
