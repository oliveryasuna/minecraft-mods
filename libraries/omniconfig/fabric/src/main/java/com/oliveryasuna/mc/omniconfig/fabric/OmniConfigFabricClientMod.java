package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.mc.omniconfig.fabric.gui.OmniConfigGui;
import com.oliveryasuna.mc.omniconfig.fabric.gui.YaclScreenProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class OmniConfigFabricClientMod implements ClientModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    //==================================================
    // Constructors
    //==================================================

    public OmniConfigFabricClientMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitializeClient() {
        if(FabricLoader.getInstance().isModLoaded(YACL_MOD_ID)) {
            OmniConfigGui.registerProvider(new YaclScreenProvider());
        }
    }

}
