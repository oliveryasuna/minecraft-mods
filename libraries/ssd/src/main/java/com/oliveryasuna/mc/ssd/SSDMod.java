package com.oliveryasuna.mc.ssd;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.ssd.block.SSDBlock;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntities;
import com.oliveryasuna.mc.ssd.config.SSDConfig;
import com.oliveryasuna.mc.ssd.item.SSDBlockItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class SSDMod implements ModInitializer {

    //==================================================
    // Static fields
    //==================================================

    public static final String MOD_ID = "seven-segment-display";

    /**
     * Registration name shared by the block and its item.
     */
    private static final String SSD_NAME = "seven_segment_display";

    public static final ResourceKey<Block> SSD_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id(SSD_NAME));

    public static final SSDBlock SSD_BLOCK = new SSDBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .sound(SoundType.METAL)
            .strength(3.0F)
            .requiresCorrectToolForDrops()
            .setId(SSD_BLOCK_KEY));

    public static final ResourceKey<Item> SSD_ITEM_KEY = ResourceKey.create(Registries.ITEM, id(SSD_NAME));

    public static final BlockItem SSD_ITEM = new SSDBlockItem(
            SSD_BLOCK,
            new Item.Properties()
                    .useBlockDescriptionPrefix()
                    .setId(SSD_ITEM_KEY)
    );

    private static volatile ConfigHandle<SSDConfig> config;

    //==================================================
    // Static methods
    //==================================================

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * The COAL config handle. Non-null after {@link #onInitialize()} has run.
     */
    public static ConfigHandle<SSDConfig> config() {
        return config;
    }

    /**
     * Whether unlit segments should be drawn; defaults to {@code true} before
     * config is ready.
     */
    public static boolean showUnlitSegments() {
        final ConfigHandle<SSDConfig> handle = config;

        return (handle == null) || handle.get().showUnlitSegments;
    }

    /**
     * Whether camo is restricted to full solid blocks; defaults to
     * {@code true} before config is ready.
     */
    public static boolean solidBlocksOnly() {
        final ConfigHandle<SSDConfig> handle = config;

        return (handle == null) || handle.get().solidBlocksOnly;
    }

    /**
     * Glow strength around lit segments (0 none, 1 subtle, 2 full); defaults to
     * 1 before config is ready.
     */
    public static int glowLevel() {
        final ConfigHandle<SSDConfig> handle = config;
        final int level = (handle == null) ? 2 : handle.get().glowLevel;

        return Math.max(0, Math.min(1, level));
    }

    //==================================================
    // Constructors
    //==================================================

    public SSDMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitialize() {
        config = Coal.register(SSDConfig.class);

        Registry.register(BuiltInRegistries.BLOCK, SSD_BLOCK_KEY, SSD_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, SSD_ITEM_KEY, SSD_ITEM);

        SSDBlockEntities.register();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> entries.accept(SSD_ITEM));
    }

}
