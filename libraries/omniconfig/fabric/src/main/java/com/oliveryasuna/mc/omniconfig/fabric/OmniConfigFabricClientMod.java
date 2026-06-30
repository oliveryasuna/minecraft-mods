package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.fabric.config.Frontend;
import com.oliveryasuna.mc.omniconfig.fabric.config.OmniConfigConfig;
import com.oliveryasuna.mc.omniconfig.fabric.gui.ClothScreenProvider;
import com.oliveryasuna.mc.omniconfig.fabric.gui.OmniConfigGui;
import com.oliveryasuna.mc.omniconfig.fabric.gui.YaclScreenProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class OmniConfigFabricClientMod implements ClientModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    private static final String CLOTH_MOD_ID = "cloth-config";

    private static final String PREFERRED_FRONTEND_PATH = "gui.preferredFrontend";

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
        if(FabricLoader.getInstance().isModLoaded(CLOTH_MOD_ID)) {
            OmniConfigGui.registerProvider(new ClothScreenProvider());
        }

        // Wire OmniConfig's own preferredFrontend into OmniConfigGui:
        // 1. Apply the current value once (server-side bootstrap already
        //    loaded the manager in OmniConfigFabricMod.onInitialize).
        // 2. Subscribe to the path so a user edit propagates without restart.
        // 3. Register the self-config screen so users can edit it via
        //    ModMenu / Catalogue.
        final ConfigManager<OmniConfigConfig> manager = OmniConfigFabricMod.manager();
        if(manager != null) {
            OmniConfigGui.setPreferredFrontend(manager.get().gui.preferredFrontend);
            manager.getEvents().subscribe(event -> {
                if(PREFERRED_FRONTEND_PATH.equals(event.path()) && event.newValue() instanceof final Frontend f) {
                    OmniConfigGui.setPreferredFrontend(f);
                }
            });
            OmniConfigGui.registerScreen(manager);
        }
    }

}
