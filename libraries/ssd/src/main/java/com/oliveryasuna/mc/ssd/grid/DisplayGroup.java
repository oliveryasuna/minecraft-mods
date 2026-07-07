package com.oliveryasuna.mc.ssd.grid;

import com.oliveryasuna.mc.ssd.block.DisplayBlock;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An immutable view of a formed display group: its top-left {@code origin},
 * {@code size}, coordinate {@code basis}, and the display {@code block} all its
 * members share.
 *
 * @param origin the world position of cell {@code (0, 0)} (the controller).
 * @param size   grid dimensions.
 * @param basis  coordinate basis (facing-derived axes).
 * @param block  the display block type shared by every member.
 */
public record DisplayGroup(
        BlockPos origin,
        GridSize size,
        GridBasis basis,
        Block block
) {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Builds the group that {@code pos} belongs to from its block entity's
     * stored grid data, or empty if {@code pos} is not a grouped display.
     */
    public static Optional<DisplayGroup> around(
            final Level level,
            final BlockPos pos
    ) {
        if(!(level.getBlockEntity(pos) instanceof final SSDBlockEntity be) || !be.isGrouped()) {
            return Optional.empty();
        }

        final BlockState state = level.getBlockState(pos);

        if(!(state.getBlock() instanceof DisplayBlock)) {
            return Optional.empty();
        }

        final GridBasis basis = new GridBasis(state.getValue(HorizontalDirectionalBlock.FACING));
        final GridSize size = new GridSize(be.gridWidth(), be.gridHeight());
        final BlockPos origin = basis.origin(pos, new GridCoord(be.gridCol(), be.gridRow()));

        return Optional.of(new DisplayGroup(origin, size, basis, state.getBlock()));
    }

    //==================================================
    // Methods
    //==================================================

    public BlockPos cell(
            final int col,
            final int row
    ) {
        return basis.cell(origin, col, row);
    }

    /**
     * Every member cell position, row-major from the top-left.
     */
    public List<BlockPos> cells() {
        final List<BlockPos> cells = new ArrayList<>(size.area());

        for(int row = 0; row < size.height(); row++) {
            for(int col = 0; col < size.width(); col++) {
                cells.add(basis.cell(origin, col, row));
            }
        }

        return cells;
    }

    /**
     * The full-block bounding box of the whole grid, for outlines.
     */
    public AABB bounds() {
        return AABB.encapsulatingFullBlocks(origin, cell(size.width() - 1, size.height() - 1));
    }

}
