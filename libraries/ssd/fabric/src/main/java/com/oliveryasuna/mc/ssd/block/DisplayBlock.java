package com.oliveryasuna.mc.ssd.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import com.oliveryasuna.mc.ssd.config.SSDSettings;
import com.oliveryasuna.mc.ssd.display.DisplayType;
import com.oliveryasuna.mc.ssd.display.DisplayTypes;
import com.oliveryasuna.mc.ssd.display.Glyphs;
import com.oliveryasuna.mc.ssd.grid.GridDriver;
import com.oliveryasuna.mc.ssd.grid.GridFormation;
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

import java.util.OptionalInt;

/**
 * A camo-able, joinable seven-segment display block. Behaviour that varies
 * between variants (the redstone-to-glyph mapping) is delegated to a
 * {@link DisplayType}, so a variant is just this block plus a type — no
 * subclassing.
 */
public class DisplayBlock extends HorizontalDirectionalBlock implements EntityBlock {

    //==================================================
    // Static fields
    //==================================================

    public static final MapCodec<DisplayBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DisplayTypes.CODEC.fieldOf("display_type").forGetter(DisplayBlock::displayType),
            propertiesCodec()
    ).apply(instance, DisplayBlock::new));

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /**
     * The {@link Glyphs} index currently displayed (0-15). Only meaningful when
     * {@link #LIT} is {@code true}.
     */
    public static final IntegerProperty VALUE = IntegerProperty.create("value", 0, Glyphs.COUNT - 1);

    /**
     * Whether the display is lit. {@code false} == blank (no incoming redstone
     * signal).
     */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    //==================================================
    // Fields
    //==================================================

    private final DisplayType displayType;

    //==================================================
    // Constructors
    //==================================================

    public DisplayBlock(
            final DisplayType displayType,
            final Properties properties
    ) {
        super(properties);

        this.displayType = displayType;

        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VALUE, 0)
                .setValue(LIT, false));
    }

    //==================================================
    // Methods
    //==================================================

    public DisplayType displayType() {
        return displayType;
    }

    // Redstone mapping
    //--------------------------------------------------

    /**
     * Maps an incoming redstone signal (0-15) to this display's glyph via its
     * {@link DisplayType}.
     */
    public BlockState displayFor(
            final BlockState state,
            final int signal
    ) {
        final OptionalInt index = displayType.mapping().glyphIndex(signal);

        if(index.isEmpty()) {
            return state.setValue(LIT, false);
        }

        return state
                .setValue(LIT, true)
                .setValue(VALUE, index.getAsInt());
    }

    private void refresh(
            final BlockState state,
            final Level level,
            final BlockPos pos
    ) {
        if(level.isClientSide) {
            return;
        }

        // Joined displays are driven as a whole from the strongest signal at
        // any cell.
        if((level.getBlockEntity(pos) instanceof final SSDBlockEntity be) && be.isGrouped()) {
            GridDriver.update(level, pos);

            return;
        }

        final BlockState next = displayFor(state, level.getBestNeighborSignal(pos));

        if(next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    // Block
    //--------------------------------------------------

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
     * Crouch-placing a display links it with its coplanar, same-type neighbors
     * into one joined N&times;N display (when they form a filled square).
     * Placing without crouching leaves it standalone. Uses
     * {@code isShiftKeyDown} (not {@code isCrouching}) so crouch-fly placement
     * works.
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

        if((level instanceof final ServerLevel server) && (placer instanceof final Player player) && player.isShiftKeyDown()) {
            GridFormation.form(server, pos);
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
        GridDriver.dissolve(level, pos);

        return super.playerWillDestroy(level, pos, state, player);
    }

    // Camo
    //--------------------------------------------------

    /**
     * Right-clicking with a block item re-skins the display to look like that
     * block. The held item is not consumed. Another display, or a non-block
     * item, is ignored; non-solid blocks are ignored when
     * {@code solidBlocksOnly} is enabled.
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
        if(!(stack.getItem() instanceof final BlockItem blockItem) || (blockItem.getBlock() instanceof DisplayBlock)) {
            return InteractionResult.PASS;
        }

        final BlockState camo = blockItem.getBlock().defaultBlockState();

        if(SSDSettings.solidBlocksOnly() && !camo.isSolidRender()) {
            return InteractionResult.PASS;
        } else if(level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if(level.getBlockEntity(pos) instanceof final SSDBlockEntity ssd) {
            ssd.setCamo(camo);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

}
