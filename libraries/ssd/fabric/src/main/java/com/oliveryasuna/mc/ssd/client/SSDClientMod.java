package com.oliveryasuna.mc.ssd.client;

import com.oliveryasuna.mc.ssd.client.render.CamoDisplayModel;
import com.oliveryasuna.mc.ssd.content.SSDBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.world.level.block.Block;

/**
 * Client entry point. Registers the {@link CamoDisplayModel} for every SSD
 * block state, replacing the JSON block model so the display can render a live
 * camo + digit overlay.
 */
public final class SSDClientMod implements ClientModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public SSDClientMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            final CamoDisplayModel model = new CamoDisplayModel();

            for(final Block block : new Block[] {SSDBlocks.DIGIT, SSDBlocks.HEX}) {
                pluginContext.registerBlockStateResolver(block, resolverContext ->
                        resolverContext.block().getStateDefinition().getPossibleStates()
                                .forEach(state -> resolverContext.setModel(state, model)));
            }
        });

        SSDDebugRenderer.register();
    }

}
