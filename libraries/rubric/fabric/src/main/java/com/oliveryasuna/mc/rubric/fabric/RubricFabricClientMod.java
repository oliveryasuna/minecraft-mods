package com.oliveryasuna.mc.rubric.fabric;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.fabric.config.Frontend;
import com.oliveryasuna.mc.rubric.fabric.config.RubricConfig;
import com.oliveryasuna.mc.rubric.fabric.gui.ClothScreenProvider;
import com.oliveryasuna.mc.rubric.fabric.gui.RubricGui;
import com.oliveryasuna.mc.rubric.fabric.gui.YaclScreenProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class RubricFabricClientMod implements ClientModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    private static final String CLOTH_MOD_ID = "cloth-config";

    private static final String PREFERRED_FRONTEND_PATH = "gui.preferredFrontend";

    //==================================================
    // Constructors
    //==================================================

    public RubricFabricClientMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitializeClient() {
        if(FabricLoader.getInstance().isModLoaded(YACL_MOD_ID)) {
            RubricGui.registerProvider(new YaclScreenProvider());
        }
        if(FabricLoader.getInstance().isModLoaded(CLOTH_MOD_ID)) {
            RubricGui.registerProvider(new ClothScreenProvider());
        }

        // Wire Rubric's own preferredFrontend into RubricGui:
        // 1. Apply the current value once (server-side bootstrap already
        //    loaded the manager in RubricFabricMod.onInitialize).
        // 2. Subscribe to the path so a user edit propagates without restart.
        // 3. Register the self-config screen so users can edit it via
        //    ModMenu / Catalogue.
        final ConfigManager<RubricConfig> manager = RubricFabricMod.manager();
        if(manager != null) {
            RubricGui.setPreferredFrontend(manager.get().gui.preferredFrontend);
            manager.getEvents().subscribe(event -> {
                if(PREFERRED_FRONTEND_PATH.equals(event.path()) && event.newValue() instanceof final Frontend f) {
                    RubricGui.setPreferredFrontend(f);
                }
            });
            RubricGui.registerScreen(manager);
        }
    }

}
