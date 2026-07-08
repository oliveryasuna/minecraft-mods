package com.oliveryasuna.mc.ssd.content;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.SSDMod;
import com.oliveryasuna.mc.ssd.block.DisplayBlock;
import com.oliveryasuna.mc.ssd.display.DisplayTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The mod's blocks and their registry keys. {@link #register()} runs during common init.
 */
public final class SSDBlocks {

    //==================================================
    // Static fields
    //==================================================

    public static final ResourceKey<Block> DIGIT_KEY = key("digit_display");
    public static final DisplayBlock DIGIT = new DisplayBlock(DisplayTypes.DIGIT, properties(DIGIT_KEY));

    public static final ResourceKey<Block> HEX_KEY = key("hex_display");
    public static final DisplayBlock HEX = new DisplayBlock(DisplayTypes.HEX, properties(HEX_KEY));

    //==================================================
    // Static methods
    //==================================================

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, DIGIT_KEY, DIGIT);
        Registry.register(BuiltInRegistries.BLOCK, HEX_KEY, HEX);
    }

    private static ResourceKey<Block> key(final String name) {
        return ResourceKey.create(Registries.BLOCK, SSDMod.id(name));
    }

    private static BlockBehaviour.Properties properties(final ResourceKey<Block> key) {
        // Behaves like an iron block: same hardness/resistance, metal sound,
        // and requires a stone+ pickaxe to drop (via the mineable/pickaxe +
        // needs_stone_tool block tags).
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .setId(key);
    }

    //==================================================
    // Constructors
    //==================================================

    private SSDBlocks() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
