package com.oliveryasuna.mc.ssd.block;

import com.mojang.serialization.MapCodec;
import com.oliveryasuna.mc.ssd.SSDMod;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public final class SSDBlock extends HorizontalDirectionalBlock implements EntityBlock {

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
     * Whether the display is lit.
     * <p>
     * {@code false} == blank (no incoming redstone signal).
     */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    /**
     * Highest displayable digit. Signals stronger than {@code MAX_DIGIT + 1}
     * clamp here.
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
     * Signal 0 blanks the display; signals 1-10 show digits 0-9
     * (digit == signal - 1); signals 11-15 clamp to 9.
     */
    public static BlockState displayFor(
            final BlockState state,
            final int signal
    ) {
        if(signal <= 0) {
            return state.setValue(LIT, false);
        }

        final int digit = Math.min(signal - 1, MAX_DIGIT);

        return state
                .setValue(LIT, true)
                .setValue(VALUE, digit);
    }

    private void refresh(
            final BlockState state,
            final Level level,
            final BlockPos pos
    ) {
        if(level.isClientSide) {
            return;
        }

        // Joined displays are driven as a whole from the strongest signal at any cell.
        if((level.getBlockEntity(pos) instanceof final SSDBlockEntity be) && be.isGrouped()) {
            SSDGrid.update(level, pos);

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

    // EntityBlock
    //--------------------------------------------------

    @Override
    public BlockEntity newBlockEntity(
            final BlockPos pos,
            final BlockState state
    ) {
        return new SSDBlockEntity(pos, state);
    }

    // Joining
    //--------------------------------------------------

    /**
     * Crouch-placing an SSD block links it with its coplanar, same-facing
     * neighbours into one joined N&times;N display (when they form a filled
     * rectangle). Placing without crouching leaves it standalone.
     */
    @Override
    public void setPlacedBy(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final LivingEntity placer,
            final ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // isShiftKeyDown (not isCrouching): the crouch pose is not set whil
        // flying, so isCrouching would miss crouch-fly placement.
        if((level instanceof final ServerLevel server) && (placer instanceof final Player player) && player.isShiftKeyDown()) {
            SSDGrid.form(server, pos);
        }
    }

    /**
     * Breaking any member dissolves the whole joined display back into
     * standalone blocks.
     */
    @Override
    public BlockState playerWillDestroy(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final Player player
    ) {
        SSDGrid.dissolve(level, pos);

        return super.playerWillDestroy(level, pos, state, player);
    }

    // Camo
    //--------------------------------------------------

    /**
     * Right-clicking with a block item re-skins the display to look like that
     * block. The held item is not consumed. Uses the SSD block itself or
     * non-block items are ignored.
     */
    @Override
    protected InteractionResult useItemOn(
            final ItemStack stack,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hit
    ) {
        if(!(stack.getItem() instanceof final BlockItem blockItem) || blockItem.getBlock() instanceof SSDBlock) {
            return InteractionResult.PASS;
        }

        final BlockState camo = blockItem.getBlock().defaultBlockState();

        // Restrict to full, solid blocks when configured (skips glass, slabs,
        // stairs, torches, ...).
        if(SSDMod.solidBlocksOnly() && !camo.isSolidRender()) {
            return InteractionResult.PASS;
        } else if(level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if(level.getBlockEntity(pos) instanceof final SSDBlockEntity ssd) {
            ssd.setCamo(camo);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
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
