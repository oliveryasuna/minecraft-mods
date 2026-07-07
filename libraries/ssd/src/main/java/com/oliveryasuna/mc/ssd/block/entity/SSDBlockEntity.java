package com.oliveryasuna.mc.ssd.block.entity;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the block this display is disguised (camouflaged) as. The camo
 * {@link BlockState} is exposed to the client renderer as
 * {@linkplain #getRenderData() render data} so the FRAPI model can emit the
 * camo's quads with the digit overlaid on the facing side.
 */
public final class SSDBlockEntity extends BlockEntity implements RenderDataBlockEntity {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Camo used before the player applies one — a neutral cased look.
     */
    public static final BlockState DEFAULT_CAMO = Blocks.GRAY_CONCRETE.defaultBlockState();

    /**
     * Client-side live SSD block entities, for debug rendering (grid outlines).
     */
    public static final Set<SSDBlockEntity> CLIENT_INSTANCES = ConcurrentHashMap.newKeySet();

    private static final String CAMO_KEY = "Camo";
    private static final String GRID_COL_KEY = "GridCol";
    private static final String GRID_ROW_KEY = "GridRow";
    private static final String GRID_WIDTH_KEY = "GridWidth";
    private static final String GRID_HEIGHT_KEY = "GridHeight";

    //==================================================
    // Inner types
    //==================================================

    /**
     * Client render payload: the camo block plus this display's cell within a
     * joined N&times;N group. A standalone display is a 1&times;1 group at cell
     * (0, 0).
     */
    public record RenderData(
            BlockState camo,
            int col,
            int row,
            int width,
            int height
    ) {

    }

    //==================================================
    // Fields
    //==================================================

    private BlockState camo;

    private int gridCol;
    private int gridRow;
    private int gridWidth;
    private int gridHeight;

    //==================================================
    // Constructors
    //==================================================

    public SSDBlockEntity(
            final BlockPos pos,
            final BlockState state
    ) {
        super(SSDBlockEntities.SSD, pos, state);

        this.camo = DEFAULT_CAMO;

        this.gridCol = 0;
        this.gridRow = 0;
        this.gridWidth = 1;
        this.gridHeight = 1;
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * {@code true} when this display is part of a joined group (larger than 1&times;1).
     */
    public boolean isGrouped() {
        return (gridWidth * gridHeight) > 1;
    }

    /**
     * The controller cell — the group's top-left block — which reads redstone for the whole group.
     */
    public boolean isController() {
        return (gridCol == 0) && (gridRow == 0);
    }

    /**
     * Server-side: assign this display's cell within a group and re-render.
     */
    public void setGrid(final int col, final int row, final int width, final int height) {
        gridCol = col;
        gridRow = row;
        gridWidth = width;
        gridHeight = height;

        sync();
    }

    private void sync() {
        setChanged();

        if(level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // RenderDataBlockEntity
    //--------------------------------------------------

    @Override
    public Object getRenderData() {
        return new RenderData(camo, gridCol, gridRow, gridWidth, gridHeight);
    }

    // BlockEntity
    //--------------------------------------------------

    @Override
    public void setLevel(final Level level) {
        super.setLevel(level);

        if(level.isClientSide) {
            CLIENT_INSTANCES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        CLIENT_INSTANCES.remove(this);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        camo = input.read(CAMO_KEY, BlockState.CODEC).orElse(DEFAULT_CAMO);
        gridCol = input.getIntOr(GRID_COL_KEY, 0);
        gridRow = input.getIntOr(GRID_ROW_KEY, 0);
        gridWidth = input.getIntOr(GRID_WIDTH_KEY, 1);
        gridHeight = input.getIntOr(GRID_HEIGHT_KEY, 1);

        // On the client this runs when an update packet arrives; mark the
        // section for re-render.
        if(level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        output.store(CAMO_KEY, BlockState.CODEC, camo);
        output.putInt(GRID_COL_KEY, gridCol);
        output.putInt(GRID_ROW_KEY, gridRow);
        output.putInt(GRID_WIDTH_KEY, gridWidth);
        output.putInt(GRID_HEIGHT_KEY, gridHeight);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //==================================================
    // Getters/setters
    //==================================================

    public BlockState getCamo() {
        return camo;
    }

    /**
     * Server-side: change the camo, persist it, and push a re-render to
     * tracking clients.
     */
    public void setCamo(final BlockState camo) {
        this.camo = camo;

        sync();
    }

    public int gridCol() {
        return gridCol;
    }

    public int gridRow() {
        return gridRow;
    }

    public int gridWidth() {
        return gridWidth;
    }

    public int gridHeight() {
        return gridHeight;
    }

}
