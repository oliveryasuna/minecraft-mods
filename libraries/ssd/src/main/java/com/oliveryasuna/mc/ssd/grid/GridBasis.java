package com.oliveryasuna.mc.ssd.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The coordinate basis of a display grid: the world directions its {@code col}
 * and {@code row} axes run along, for a given facing.
 * <p>
 * This is the single place that encodes the "displays sit on a vertical plane"
 * assumption — {@code col} runs to the viewer's right
 * ({@code facing.getCounterClockWise()}) and {@code row} runs downward.
 * Supporting up/down-facing displays later means adding alternative bases here,
 * not touching the formation/driving logic.
 */
public record GridBasis(
        Direction facing
) {

    //==================================================
    // Methods
    //==================================================

    /**
     * World direction of increasing {@code col}.
     */
    public Direction right() {
        return facing.getCounterClockWise();
    }

    /**
     * World direction of increasing {@code row}.
     */
    public Direction down() {
        return Direction.DOWN;
    }

    /**
     * The four in-plane neighbor directions, for flood-fill.
     */
    public Direction[] neighbours() {
        return new Direction[] {right(), right().getOpposite(), Direction.UP, Direction.DOWN};
    }

    /**
     * World position of cell {@code (col, row)} relative to the grid's top-left
     * {@code origin}.
     */
    public BlockPos cell(
            final BlockPos origin,
            final int col,
            final int row
    ) {
        return origin.relative(right(), col).relative(down(), row);
    }

    public BlockPos cell(
            final BlockPos origin,
            final GridCoord coord
    ) {
        return cell(origin, coord.col(), coord.row());
    }

    /**
     * The grid origin (top-left) implied by a member at {@code coord}.
     */
    public BlockPos origin(
            final BlockPos member,
            final GridCoord coord
    ) {
        return member.relative(right(), -coord.col()).relative(down(), -coord.row());
    }

    /**
     * Column offset of {@code to} relative to {@code from} along the right
     * axis.
     */
    public int col(
            final BlockPos from,
            final BlockPos to
    ) {
        final Direction right = right();

        return ((to.getX() - from.getX()) * right.getStepX()) + ((to.getZ() - from.getZ()) * right.getStepZ());
    }

    /**
     * Row offset of {@code to} relative to {@code from} (downward positive).
     */
    public int row(
            final BlockPos from,
            final BlockPos to
    ) {
        return from.getY() - to.getY();
    }

}
