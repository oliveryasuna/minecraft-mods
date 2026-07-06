package com.oliveryasuna.mc.ssd.block.entity;

import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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

    private static final String CAMO_KEY = "Camo";

    //==================================================
    // Fields
    //==================================================

    private BlockState camo = DEFAULT_CAMO;

    //==================================================
    // Constructors
    //==================================================

    public SSDBlockEntity(final BlockPos pos, final BlockState state) {
        super(SSDBlockEntities.SSD, pos, state);
    }

    //==================================================
    // Methods
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

        setChanged();

        if(level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // RenderDataBlockEntity
    //--------------------------------------------------

    @Override
    public Object getRenderData() {
        return camo;
    }

    // BlockEntity
    //--------------------------------------------------

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        camo = input.read(CAMO_KEY, BlockState.CODEC).orElse(DEFAULT_CAMO);

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
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
