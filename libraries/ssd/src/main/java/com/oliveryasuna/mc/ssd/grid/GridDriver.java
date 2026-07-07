package com.oliveryasuna.mc.ssd.grid;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.block.DisplayBlock;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives a formed display: pushing the current glyph to every cell, and tearing
 * a group back down to
 * standalone blocks.
 */
public final class GridDriver {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Recompute a group's glyph from the strongest redstone signal at any cell
     * and push it to all cells.
     */
    public static void update(
            final Level level,
            final BlockPos anyMember
    ) {
        if(level.isClientSide) {
            return;
        }

        DisplayGroup.around(level, anyMember).ifPresent(group -> {
            int signal = 0;

            for(final BlockPos cell : group.cells()) {
                signal = Math.max(signal, level.getBestNeighborSignal(cell));
            }

            for(final BlockPos cell : group.cells()) {
                setDisplay(level, cell, signal);
            }
        });
    }

    /**
     * Reset every member of the group containing {@code pos} back to a
     * standalone 1&times;1 display, re-deriving each block's glyph from its own
     * redstone.
     */
    public static void dissolve(
            final Level level,
            final BlockPos pos
    ) {
        if(level.isClientSide) {
            return;
        }

        DisplayGroup.around(level, pos).ifPresent(group -> {
            for(final BlockPos cell : group.cells()) {
                if(level.getBlockEntity(cell) instanceof final SSDBlockEntity member) {
                    member.setGrid(0, 0, 1, 1);

                    setDisplay(level, cell, level.getBestNeighborSignal(cell));
                }
            }
        });
    }

    private static void setDisplay(
            final Level level,
            final BlockPos cell,
            final int signal
    ) {
        final BlockState state = level.getBlockState(cell);

        if(state.getBlock() instanceof final DisplayBlock display) {
            final BlockState next = display.displayFor(state, signal);

            if(next != state) {
                level.setBlock(cell, next, Block.UPDATE_CLIENTS);
            }
        }
    }

    //==================================================
    // Constructors
    //==================================================

    private GridDriver() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
