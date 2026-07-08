package com.oliveryasuna.mc.ssd;

import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntities;
import com.oliveryasuna.mc.ssd.config.SSDSettings;
import com.oliveryasuna.mc.ssd.content.SSDBlocks;
import com.oliveryasuna.mc.ssd.content.SSDCreativeTab;
import com.oliveryasuna.mc.ssd.content.SSDItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point. Owns the mod identity and wires the modules together at
 * init; the modules themselves ({@link SSDBlocks}, {@link SSDItems},
 * {@link SSDBlockEntities}, {@link SSDCreativeTab}, {@link SSDSettings}) hold
 * their own content and registration.
 */
public final class SSDMod implements ModInitializer {

    //==================================================
    // Static fields
    //==================================================

    public static final String MOD_ID = "seven-segment-display";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    //==================================================
    // Static methods
    //==================================================

    /**
     * A {@link ResourceLocation} in the mod's namespace.
     */
    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
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
        SSDSettings.initialize();

        SSDBlocks.register();
        SSDItems.register();
        SSDBlockEntities.register();
        SSDCreativeTab.register();
    }

}
