package com.oliveryasuna.mc.ssd.client.render;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.SSDMod;
import com.oliveryasuna.mc.ssd.block.DisplayBlock;
import com.oliveryasuna.mc.ssd.config.SSDSettings;
import com.oliveryasuna.mc.ssd.display.Glyph;
import com.oliveryasuna.mc.ssd.display.Glyphs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resolves the block-atlas sprite to overlay on a display, from its state and
 * the display config. Owns all sprite naming (the {@code _lit} /
 * {@code _glow{n}} variant suffixes) in one place.
 */
public final class GlyphSprites {

    //==================================================
    // Static methods
    //==================================================

    /**
     * A sprite by path under {@code textures/}, in the mod's namespace.
     */
    public static TextureAtlasSprite atlas(final String path) {
        return Minecraft.getInstance()
                .getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ResourceLocation.fromNamespaceAndPath(SSDMod.MOD_ID, path));
    }

    /**
     * The blank (all-segments-off) display face.
     */
    public static TextureAtlasSprite blank() {
        return atlas("block/template");
    }

    /**
     * The overlay sprite for a display state, or {@code null} when nothing
     * should be drawn.
     * <p>
     * When {@code showUnlitSegments} is on, a lit display shows all segments
     * (lit + faint unlit) and an off display shows the blank frame. When off,
     * only lit segments are drawn and an off display renders nothing. A
     * positive {@code glowLevel} selects the pre-baked glow variant.
     */
    public static TextureAtlasSprite overlay(final BlockState state, final int glowLevel) {
        final boolean showUnlit = SSDSettings.showUnlitSegments();

        if(state.getValue(DisplayBlock.LIT)) {
            final Glyph glyph = Glyphs.get(state.getValue(DisplayBlock.VALUE));
            final String suffix = (showUnlit ? "" : "_lit") + (glowLevel > 0 ? "_glow" + glowLevel : "");

            return atlas("block/" + glyph.sprite() + suffix);
        }

        return showUnlit ? blank() : null;
    }

    //==================================================
    // Constructors
    //==================================================

    private GlyphSprites() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
