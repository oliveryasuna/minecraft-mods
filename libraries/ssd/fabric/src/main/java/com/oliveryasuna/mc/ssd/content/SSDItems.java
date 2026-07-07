package com.oliveryasuna.mc.ssd.content;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.SSDMod;
import com.oliveryasuna.mc.ssd.item.SSDBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/**
 * The block items for the mod's blocks. {@link #register()} runs during common
 * init, after {@link SSDBlocks}.
 */
public final class SSDItems {

    //==================================================
    // Static fields
    //==================================================

    public static final ResourceKey<Item> DIGIT_KEY = key("digit_display");
    public static final BlockItem DIGIT = new SSDBlockItem(SSDBlocks.DIGIT, properties(DIGIT_KEY));

    public static final ResourceKey<Item> HEX_KEY = key("hex_display");
    public static final BlockItem HEX = new SSDBlockItem(SSDBlocks.HEX, properties(HEX_KEY));

    //==================================================
    // Static methods
    //==================================================

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, DIGIT_KEY, DIGIT);
        Registry.register(BuiltInRegistries.ITEM, HEX_KEY, HEX);
    }

    private static ResourceKey<Item> key(final String name) {
        return ResourceKey.create(Registries.ITEM, SSDMod.id(name));
    }

    private static Item.Properties properties(final ResourceKey<Item> key) {
        return new Item.Properties()
                .useBlockDescriptionPrefix()
                .setId(key);
    }

    //==================================================
    // Constructors
    //==================================================

    private SSDItems() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
