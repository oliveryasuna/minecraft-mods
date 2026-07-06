package com.oliveryasuna.mc.ssd.client;

import com.oliveryasuna.mc.ssd.SSDMod;
import com.oliveryasuna.mc.ssd.block.SSDBlock;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * FRAPI model for the SSD block. Renders the
 * {@linkplain SSDBlockEntity#getCamo() camo} block's geometry on all faces,
 * then overlays the seven-segment display (an off/blank frame or a lit,
 * emissive digit) on the block's {@link HorizontalDirectionalBlock#FACING}
 * side.
 * <p>
 * A single stateless instance serves every block state: facing/lit/value are
 * read from the {@code state} passed to {@link #emitQuads}, and the camo from
 * per-block render data.
 */
public final class CamoDisplayModel implements BlockStateModel, BlockStateModel.UnbakedRoot {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Distance the overlay sits proud of the face, to win the depth test
     * against the camo face.
     */
    private static final float OVERLAY_DEPTH = -0.001F;

    private static final int WHITE = 0xFFFFFFFF;

    //==================================================
    // Static methods
    //==================================================

    private static TextureAtlasSprite sprite(final String path) {
        return Minecraft.getInstance()
                .getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ResourceLocation.fromNamespaceAndPath(SSDMod.MOD_ID, path));
    }

    /**
     * The overlay sprite for this state, or {@code null} when nothing should be
     * drawn.
     * <p>
     * When {@code showUnlitSegments} is enabled the full display is drawn
     * (lit + faint unlit segments, or the blank frame when off). When disabled,
     * only the lit segments are drawn and an off display renders nothing.
     */
    private static TextureAtlasSprite displaySprite(final BlockState state) {
        final boolean showUnlit = SSDMod.showUnlitSegments();

        if(state.getValue(SSDBlock.LIT)) {
            final int value = state.getValue(SSDBlock.VALUE);

            return sprite(showUnlit ? "block/digit_" + value : "block/digit_" + value + "_lit");
        }

        return showUnlit ? sprite("block/template") : null;
    }

    //==================================================
    // Constructors
    //==================================================

    public CamoDisplayModel() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    // BlockStateModel.UnbakedRoot
    //--------------------------------------------------

    @Override
    public BlockStateModel bake(final BlockState state, final ModelBaker baker) {
        return this;
    }

    @Override
    public Object visualEqualityGroup(final BlockState state) {
        // One dynamic model backs every state; treat them as a single bake
        // group.
        return this;
    }

    @Override
    public void resolveDependencies(final ResolvableModel.Resolver resolver) {
        // No model dependencies — geometry is generated at render time.
    }

    // BlockStateModel
    //--------------------------------------------------

    @Override
    public void emitQuads(
            final QuadEmitter emitter,
            final BlockAndTintGetter blockView,
            final BlockPos pos,
            final BlockState state,
            final RandomSource random,
            final Predicate<Direction> cullTest
    ) {
        emitCamo(emitter, blockView, pos, random, cullTest);
        emitOverlay(emitter, state);
    }

    @Override
    public void collectParts(
            final RandomSource random,
            final List<BlockModelPart> parts
    ) {
        // Rendering goes through emitQuads (Indigo); nothing to collect for the fallback path.
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return sprite("block/template");
    }

    private void emitCamo(
            final QuadEmitter emitter,
            final BlockAndTintGetter blockView,
            final BlockPos pos,
            final RandomSource random,
            final Predicate<Direction> cullTest
    ) {
        final Object data = blockView.getBlockEntityRenderData(pos);
        final BlockState camo = (data instanceof final BlockState state) ? state : SSDBlockEntity.DEFAULT_CAMO;
        if(camo.isAir()) {
            return;
        }

        final BlockStateModel camoModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);

        for(final BlockModelPart part : camoModel.collectParts(random)) {
            part.emitQuads(emitter, cullTest);
        }
    }

    private void emitOverlay(
            final QuadEmitter emitter,
            final BlockState state
    ) {
        final TextureAtlasSprite sprite = displaySprite(state);
        if(sprite == null) {
            return;
        }

        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        final boolean lit = state.getValue(SSDBlock.LIT);

        emitter.square(facing, 0.0F, 0.0F, 1.0F, 1.0F, OVERLAY_DEPTH);
        emitter.uvUnitSquare();
        emitter.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);

        for(int vertex = 0; vertex < 4; vertex++) {
            emitter.color(vertex, WHITE);
        }

        emitter.nominalFace(facing);
        emitter.cullFace(null);
        // Cutout so the transparent texture background lets the camo show
        // through; solid would not.
        emitter.renderLayer(ChunkSectionLayer.CUTOUT);
        emitter.emissive(lit);
        emitter.emit();
    }

}
