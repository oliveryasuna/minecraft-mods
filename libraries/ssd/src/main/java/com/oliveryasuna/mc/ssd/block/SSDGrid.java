package com.oliveryasuna.mc.ssd.block;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Formation and driving of joined N&times;N seven-segment displays.
 * <p>
 * Members are coplanar {@link SSDBlock}s that share a
 * {@link HorizontalDirectionalBlock#FACING} — they tile the plane perpendicular
 * to that facing, indexed by {@code (col, row)} with {@code col} nincreasing to
 * the viewer's right ({@code facing.getCounterClockWise()}) and {@code row}
 * increasing downward. The top-left cell {@code (0, 0)} is the controller.
 * Every member shows the same digit; a member's {@code (col, row)} within the
 * {@code width x height} grid selects the sub-rectangle of the digit it draws,
 * so together they render one giant digit.
 */
public final class SSDGrid {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Hard cap on a group's side length, bounding the flood fill and cell
     * iteration.
     */
    public static final int MAX_SIZE = 8;

    //==================================================
    // Static methods
    //==================================================

    /**
     * World direction of increasing {@code col} for a display with the given
     * facing.
     */
    public static Direction right(final Direction facing) {
        return facing.getCounterClockWise();
    }

    /**
     * World position of cell {@code (col, row)} given the group's top-left
     * {@code origin}.
     */
    public static BlockPos cell(
            final BlockPos origin,
            final Direction facing,
            final int col,
            final int row
    ) {
        return origin.relative(right(facing), col).relative(Direction.DOWN, row);
    }

    /**
     * Attempt to (re)form a filled rectangular group containing {@code pos}.
     * Floods coplanar, same-facing SSD neighbours; if they form a complete
     * rectangle larger than 1&times;1, assigns every member its cell and lights
     * the group. Otherwise leaves blocks untouched (standalone).
     */
    public static void form(
            final ServerLevel level,
            final BlockPos pos
    ) {
        final BlockState state = level.getBlockState(pos);

        if(!(state.getBlock() instanceof SSDBlock)) {
            return;
        }

        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        final Direction right = right(facing);
        final Direction[] dirs = {right, right.getOpposite(), Direction.UP, Direction.DOWN};

        final Set<BlockPos> members = new HashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();
        members.add(pos.immutable());
        queue.add(pos.immutable());

        while(!queue.isEmpty() && members.size() <= (MAX_SIZE * MAX_SIZE)) {
            final BlockPos p = queue.poll();

            for(final Direction d : dirs) {
                final BlockPos n = p.relative(d).immutable();

                if(!members.contains(n) && isMember(level, n, facing)) {
                    members.add(n);
                    queue.add(n);
                }
            }
        }

        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for(final BlockPos p : members) {
            final int col = colOf(p, pos, right);
            final int row = pos.getY() - p.getY();

            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        final int width = (maxCol - minCol) + 1;
        final int height = (maxRow - minRow) + 1;

        // Only form a square (N×N, N > 1), gap-free grid. Same-facing is
        // enforced by isMember.
        if((width < 1) || (height < 1) || (width > MAX_SIZE) || (height > MAX_SIZE)) {
            return;
        }
        if((width != height) || (members.size() != (width * height)) || (width == 1)) {
            return;
        }

        final BlockPos origin = pos.relative(right, minCol).relative(Direction.DOWN, minRow);

        for(final BlockPos p : members) {
            if(level.getBlockEntity(p) instanceof final SSDBlockEntity be) {
                be.setGrid(colOf(p, pos, right) - minCol, (pos.getY() - p.getY()) - minRow, width, height);
            }
        }

        update(level, origin);
    }

    /**
     * Recompute a group's digit from the strongest redstone signal at any cell
     * and push it to all cells.
     */
    public static void update(
            final Level level,
            final BlockPos anyMember
    ) {
        if(level.isClientSide) {
            return;
        }
        if(!(level.getBlockEntity(anyMember) instanceof final SSDBlockEntity be) || !be.isGrouped()) {
            return;
        }

        final Direction facing = level.getBlockState(anyMember).getValue(HorizontalDirectionalBlock.FACING);
        final int width = be.gridWidth();
        final int height = be.gridHeight();
        final BlockPos origin = anyMember.relative(right(facing), -be.gridCol()).relative(Direction.DOWN, -be.gridRow());

        int signal = 0;
        for(int row = 0; row < height; row++) {
            for(int col = 0; col < width; col++) {
                signal = Math.max(signal, level.getBestNeighborSignal(cell(origin, facing, col, row)));
            }
        }

        for(int row = 0; row < height; row++) {
            for(int col = 0; col < width; col++) {
                final BlockPos cp = cell(origin, facing, col, row);
                final BlockState cs = level.getBlockState(cp);

                if(cs.getBlock() instanceof SSDBlock) {
                    final BlockState next = SSDBlock.displayFor(cs, signal);

                    if(next != cs) {
                        level.setBlock(cp, next, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /**
     * Reset every member of the group containing {@code pos} back to a
     * standalone 1&times;1 display.
     */
    public static void dissolve(
            final Level level,
            final BlockPos pos
    ) {
        if(level.isClientSide) {
            return;
        }
        if(!(level.getBlockEntity(pos) instanceof final SSDBlockEntity be) || !be.isGrouped()) {
            return;
        }

        final Direction facing = level.getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING);
        final int width = be.gridWidth();
        final int height = be.gridHeight();
        final BlockPos origin = pos.relative(right(facing), -be.gridCol()).relative(Direction.DOWN, -be.gridRow());

        for(int row = 0; row < height; row++) {
            for(int col = 0; col < width; col++) {
                final BlockPos cp = cell(origin, facing, col, row);

                if(level.getBlockEntity(cp) instanceof final SSDBlockEntity member) {
                    member.setGrid(0, 0, 1, 1);

                    // Re-derive each block's display from its OWN redstone;
                    // otherwise cells keep the group digit the controller
                    // pushed onto them until some later neighbour update.
                    final BlockState cs = level.getBlockState(cp);

                    if(cs.getBlock() instanceof SSDBlock) {
                        final BlockState next = SSDBlock.displayFor(cs, level.getBestNeighborSignal(cp));

                        if(next != cs) {
                            level.setBlock(cp, next, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }

    private static boolean isMember(
            final Level level,
            final BlockPos pos,
            final Direction facing
    ) {
        final BlockState state = level.getBlockState(pos);

        return (state.getBlock() instanceof SSDBlock)
               && (state.getValue(HorizontalDirectionalBlock.FACING) == facing)
               && (level.getBlockEntity(pos) instanceof SSDBlockEntity);
    }

    /**
     * Signed column offset of {@code p} from {@code ref} along the display's
     * right axis.
     */
    private static int colOf(
            final BlockPos p,
            final BlockPos ref,
            final Direction right
    ) {
        return ((p.getX() - ref.getX()) * right.getStepX()) + ((p.getZ() - ref.getZ()) * right.getStepZ());
    }

    //==================================================
    // Constructors
    //==================================================

    private SSDGrid() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
