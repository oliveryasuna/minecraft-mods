package com.oliveryasuna.mc.ssd.grid;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.block.DisplayBlock;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Forms display groups: floods coplanar, same-type, same-facing neighbors and,
 * if they make a gap-free N&times;N square (N &gt; 1), assigns every member its
 * cell and lights the group.
 */
public final class GridFormation {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Hard cap on a group's side length, bounding the flood fill.
     */
    public static final int MAX_SIZE = 8;

    //==================================================
    // Static methods
    //==================================================

    public static void form(
            final ServerLevel level,
            final BlockPos pos
    ) {
        final BlockState state = level.getBlockState(pos);

        if(!(state.getBlock() instanceof DisplayBlock)) {
            return;
        }

        // A group is one block type only (all digit displays, or all hex displays).
        final Block block = state.getBlock();
        final GridBasis basis = new GridBasis(state.getValue(HorizontalDirectionalBlock.FACING));

        final Set<BlockPos> members = flood(level, pos, basis, block);

        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for(final BlockPos member : members) {
            final int col = basis.col(pos, member);
            final int row = basis.row(pos, member);

            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        final GridSize size = new GridSize((maxCol - minCol) + 1, (maxRow - minRow) + 1);

        // Only a gap-free square larger than a single block forms.
        if(!size.withinBound(MAX_SIZE) || !size.isSquare() || !size.isMultiblock() || (members.size() != size.area())) {
            return;
        }

        final BlockPos origin = basis.cell(pos, minCol, minRow);

        for(final BlockPos member : members) {
            if(level.getBlockEntity(member) instanceof final SSDBlockEntity be) {
                be.setGrid(basis.col(pos, member) - minCol, basis.row(pos, member) - minRow, size.width(), size.height());
            }
        }

        GridDriver.update(level, origin);
    }

    private static Set<BlockPos> flood(
            final ServerLevel level,
            final BlockPos start,
            final GridBasis basis,
            final Block block
    ) {
        final Direction[] dirs = basis.neighbours();
        final Set<BlockPos> members = new HashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();

        members.add(start.immutable());
        queue.add(start.immutable());

        while(!queue.isEmpty() && (members.size() <= (MAX_SIZE * MAX_SIZE))) {
            final BlockPos p = queue.poll();

            for(final Direction d : dirs) {
                final BlockPos n = p.relative(d).immutable();

                if(!members.contains(n) && isMember(level, n, basis.facing(), block)) {
                    members.add(n);
                    queue.add(n);
                }
            }
        }

        return members;
    }

    private static boolean isMember(
            final ServerLevel level,
            final BlockPos pos,
            final Direction facing,
            final Block block
    ) {
        final BlockState state = level.getBlockState(pos);

        return state.is(block)
               && (state.getValue(HorizontalDirectionalBlock.FACING) == facing)
               && (level.getBlockEntity(pos) instanceof SSDBlockEntity);
    }

    //==================================================
    // Constructors
    //==================================================

    private GridFormation() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
