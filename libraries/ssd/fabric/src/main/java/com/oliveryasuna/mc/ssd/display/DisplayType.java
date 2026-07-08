package com.oliveryasuna.mc.ssd.display;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * The behaviour of a display variant: an identity plus the
 * {@link SignalMapping} that turns redstone into a glyph. A block variant is
 * just a {@code DisplayType} plugged into the generic display block, so new
 * variants (binary, custom glyph sets, ...) are added as data, not new classes.
 *
 * @param id      unique identity, also used to (de)serialize the block's codec.
 * @param mapping redstone-to-glyph strategy.
 */
public record DisplayType(
        ResourceLocation id,
        SignalMapping mapping
) {

    //==================================================
    // Constructors
    //==================================================

    public DisplayType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(mapping, "mapping");
    }

}
