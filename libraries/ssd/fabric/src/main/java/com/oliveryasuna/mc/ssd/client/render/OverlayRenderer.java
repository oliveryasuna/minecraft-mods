package com.oliveryasuna.mc.ssd.client.render;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.block.DisplayBlock;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import com.oliveryasuna.mc.ssd.config.SSDSettings;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Emits the seven-segment overlay quad on a display's facing side: picks the
 * glyph sprite, maps it (full face, or a sub-rectangle when part of a joined
 * group), and sets the render layer/emissivity.
 */
public final class OverlayRenderer {

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

    public static void emit(
            final QuadEmitter emitter,
            final BlockState state,
            final SSDBlockEntity.RenderData data
    ) {
        final boolean lit = state.getValue(DisplayBlock.LIT);
        final int glowLevel = lit ? SSDSettings.glowLevel() : 0;

        final TextureAtlasSprite sprite = GlyphSprites.overlay(state, glowLevel);

        if(sprite == null) {
            return;
        }

        final boolean grouped = (data.width() * data.height()) > 1;
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);

        emitter.square(facing, 0.0F, 0.0F, 1.0F, 1.0F, OVERLAY_DEPTH);

        if(grouped) {
            // Explicit per-cell UVs, baked as already-normalized (0-1) so this cell samples its slice
            // of the shared glyph texture. No uv-lock, which would re-derive full-face UVs.
            subRectUv(emitter, data);
            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED);
        } else {
            emitter.uvUnitSquare();
            emitter.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
        }

        for(int vertex = 0; vertex < 4; vertex++) {
            emitter.color(vertex, WHITE);
        }

        emitter.nominalFace(facing);
        emitter.cullFace(null);
        // Glow needs a soft alpha gradient -> translucent; otherwise cutout
        // (crisp, cheaper, lets the transparent texture background show the
        // camo through).
        emitter.renderLayer(glowLevel > 0 ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT);
        emitter.emissive(lit);
        emitter.emit();
    }

    /**
     * Maps this cell's overlay quad to its slice of the shared glyph texture.
     * Vertex order matches {@code uvUnitSquare} so a full grid tiles the glyph
     * upright across its faces.
     */
    private static void subRectUv(
            final QuadEmitter emitter,
            final SSDBlockEntity.RenderData data
    ) {
        final float u0 = (float)data.col() / data.width();
        final float u1 = (float)(data.col() + 1) / data.width();
        final float v0 = (float)data.row() / data.height();
        final float v1 = (float)(data.row() + 1) / data.height();

        emitter.uv(0, u0, v0);
        emitter.uv(1, u0, v1);
        emitter.uv(2, u1, v1);
        emitter.uv(3, u1, v0);
    }

    //==================================================
    // Constructors
    //==================================================

    private OverlayRenderer() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
