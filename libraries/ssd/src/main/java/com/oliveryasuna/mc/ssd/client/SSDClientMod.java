package com.oliveryasuna.mc.ssd.client;

import com.oliveryasuna.mc.ssd.SSDMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

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
        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.registerBlockStateResolver(SSDMod.SSD_BLOCK, resolverContext -> {
                    final CamoDisplayModel model = new CamoDisplayModel();

                    resolverContext.block().getStateDefinition().getPossibleStates()
                            .forEach(state -> resolverContext.setModel(state, model));
                }));
    }

}
