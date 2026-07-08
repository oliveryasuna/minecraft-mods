package com.oliveryasuna.mc.ssd.client.render;

import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

/**
 * FRAPI model for the display blocks. A single stateless instance backs every
 * block state; it reads facing/lit/value from the {@code state} and the
 * camo + grid cell from per-block render data, then delegates to
 * {@link CamoRenderer} (the disguise block) and {@link OverlayRenderer} (the
 * glyph).
 */
public final class CamoDisplayModel implements BlockStateModel, BlockStateModel.UnbakedRoot {

    //==================================================
    // Static fields
    //==================================================

    private static final SSDBlockEntity.RenderData DEFAULT_DATA = new SSDBlockEntity.RenderData(SSDBlockEntity.DEFAULT_CAMO, 0, 0, 1, 1);

    //==================================================
    // Static methods
    //==================================================

    private static SSDBlockEntity.RenderData renderData(
            final BlockAndTintGetter blockView,
            final BlockPos pos
    ) {
        final Object data = blockView.getBlockEntityRenderData(pos);

        return (data instanceof final SSDBlockEntity.RenderData rd) ? rd : DEFAULT_DATA;
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
    public BlockStateModel bake(
            final BlockState state,
            final ModelBaker bake
    ) {
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
        final SSDBlockEntity.RenderData data = renderData(blockView, pos);

        CamoRenderer.emit(emitter, data.camo(), random, cullTest);
        OverlayRenderer.emit(emitter, state, data);
    }

    @Override
    public void collectParts(
            final RandomSource random,
            final List<BlockModelPart> parts
    ) {
        // Rendering goes through emitQuads (Indigo); nothing to collect for the
        // fallback path.
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return GlyphSprites.blank();
    }

}
