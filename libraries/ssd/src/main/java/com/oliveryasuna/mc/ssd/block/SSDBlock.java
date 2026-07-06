package com.oliveryasuna.mc.ssd.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;

public final class SSDBlock extends HorizontalDirectionalBlock {

    //==================================================
    // Static fields
    //==================================================

    private static final MapCodec<SSDBlock> CODEC = simpleCodec(SSDBlock::new);

    private static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /**
     * The digit currently displayed (0-9).
     * <p>
     * Only meaningful when {@link #LIT} is {@code true}.
     */
    public static final IntegerProperty VALUE = IntegerProperty.create("value", 0, 9);

    /**
     * Whether the display is lit. {@code false} == blank (no incoming redstone signal).
     */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    /**
     * Highest displayable digit. Signals stronger than {@code MAX_DIGIT + 1} clamp here.
     */
    private static final int MAX_DIGIT = 9;

    //==================================================
    // Static methods
    //==================================================

    // Redstone mapping
    //--------------------------------------------------

    /**
     * Maps an incoming redstone signal (0-15) to a display state.
     * <p>
     * Signal 0 blanks the display; signals 1-10 show digits 0-9 (digit == signal - 1);
     * signals 11-15 clamp to 9.
     */
    private static BlockState displayFor(final BlockState state, final int signal) {
        if(signal <= 0) {
            return state.setValue(LIT, false);
        }

        final int digit = Math.min(signal - 1, MAX_DIGIT);

        return state
                .setValue(LIT, true)
                .setValue(VALUE, digit);
    }

    private void refresh(final BlockState state, final Level level, final BlockPos pos) {
        if(level.isClientSide) {
            return;
        }

        final BlockState next = displayFor(state, level.getBestNeighborSignal(pos));

        if(next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    //==================================================
    // Constructors
    //==================================================

    public SSDBlock(final Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VALUE, 0)
                .setValue(LIT, false));
    }

    //==================================================
    // Methods
    //==================================================

    // Block
    //

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VALUE, LIT);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final BlockState oldState,
            final boolean movedByPiston
    ) {
        refresh(state, level, pos);
    }

    @Override
    protected void neighborChanged(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Block neighborBlock,
            final Orientation orientation,
            final boolean movedByPiston
    ) {
        refresh(state, level, pos);
    }

    @Override
    protected BlockState rotate(
            final BlockState state,
            final Rotation rotation
    ) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(
            final BlockState state,
            final Mirror mirror
    ) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

}
