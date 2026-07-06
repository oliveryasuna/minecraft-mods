package com.oliveryasuna.mc.coal.cloth.fabric;

import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entry point. Registers {@link ClothScreenProvider} with the COAL
 * {@link GuiRegistry} at client-init. Priority {@code 40} — leaves the higher
 * slot to the YACL adapter when both are installed.
 */
public final class ClothAdapterFabricClientMod implements ClientModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public ClothAdapterFabricClientMod() {
        super();
    }

    //==================================================
    // ClientModInitializer
    //==================================================

    @Override
    public void onInitializeClient() {
        GuiRegistry.registerProvider(new ClothScreenProvider());
    }

}
