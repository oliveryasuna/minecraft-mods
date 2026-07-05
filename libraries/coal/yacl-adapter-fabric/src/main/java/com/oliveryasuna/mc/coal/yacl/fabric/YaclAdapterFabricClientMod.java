package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entry point. Registers {@link YaclScreenProvider} with the COAL
 * {@link GuiRegistry} at client-init.
 */
public final class YaclAdapterFabricClientMod implements ClientModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public YaclAdapterFabricClientMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitializeClient() {
        GuiRegistry.registerProvider(new YaclScreenProvider());
    }

}
