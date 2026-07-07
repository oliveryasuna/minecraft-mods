package com.oliveryasuna.mc.ssd;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.ssd.block.HexDisplayBlock;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SSDMod implements ModInitializer {

    //==================================================
    // Static fields
    //==================================================

    public static final String MOD_ID = "seven-segment-display";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Digit display: 0-9.
    public static final ResourceKey<Block> DIGIT_BLOCK_KEY = blockKey("digit_display");
    public static final SSDBlock DIGIT_BLOCK = new SSDBlock(displayProperties(DIGIT_BLOCK_KEY));
    public static final ResourceKey<Item> DIGIT_ITEM_KEY = itemKey("digit_display");
    public static final BlockItem DIGIT_ITEM = new SSDBlockItem(DIGIT_BLOCK, itemProperties(DIGIT_ITEM_KEY));

    // Hex display: A-F.
    public static final ResourceKey<Block> HEX_BLOCK_KEY = blockKey("hex_display");
    public static final HexDisplayBlock HEX_BLOCK = new HexDisplayBlock(displayProperties(HEX_BLOCK_KEY));
    public static final ResourceKey<Item> HEX_ITEM_KEY = itemKey("hex_display");
    public static final BlockItem HEX_ITEM = new SSDBlockItem(HEX_BLOCK, itemProperties(HEX_ITEM_KEY));

    private static volatile ConfigHandle<SSDConfig> config;

    //==================================================
    // Static methods
    //==================================================

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static ResourceKey<Block> blockKey(final String name) {
        return ResourceKey.create(Registries.BLOCK, id(name));
    }

    private static ResourceKey<Item> itemKey(final String name) {
        return ResourceKey.create(Registries.ITEM, id(name));
    }

    private static BlockBehaviour.Properties displayProperties(final ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .sound(SoundType.METAL)
                .strength(3.0F)
                .requiresCorrectToolForDrops()
                .setId(key);
    }

    private static Item.Properties itemProperties(final ResourceKey<Item> key) {
        return new Item.Properties()
                .useBlockDescriptionPrefix()
                .setId(key);
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

        return (handle == null) || handle.get().general.showUnlitSegments;
    }

    /**
     * Whether camo is restricted to full solid blocks; defaults to
     * {@code true} before config is ready.
     */
    public static boolean solidBlocksOnly() {
        final ConfigHandle<SSDConfig> handle = config;

        return (handle == null) || handle.get().general.solidBlocksOnly;
    }

    /**
     * Glow strength around lit segments (0 none, 1 subtle, 2 full); defaults to
     * 1 before config is ready.
     */
    public static int glowLevel() {
        final ConfigHandle<SSDConfig> handle = config;
        final int level = (handle == null) ? 2 : handle.get().general.glowLevel;

        return Math.max(0, Math.min(1, level));
    }

    /**
     * Debug: whether to outline joined display grids. Defaults to {@code false}
     * before config is ready.
     */
    public static boolean debugOutlineGrids() {
        final ConfigHandle<SSDConfig> handle = config;

        return (handle != null) && handle.get().debug.outlineGrids;
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

        Registry.register(BuiltInRegistries.BLOCK, DIGIT_BLOCK_KEY, DIGIT_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK, HEX_BLOCK_KEY, HEX_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, DIGIT_ITEM_KEY, DIGIT_ITEM);
        Registry.register(BuiltInRegistries.ITEM, HEX_ITEM_KEY, HEX_ITEM);

        SSDBlockEntities.register();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> {
                    entries.accept(DIGIT_ITEM);
                    entries.accept(HEX_ITEM);
                });
    }

}
