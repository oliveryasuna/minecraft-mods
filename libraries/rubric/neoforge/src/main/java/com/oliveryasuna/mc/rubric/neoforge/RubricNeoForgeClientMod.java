package com.oliveryasuna.mc.rubric.neoforge;

import com.oliveryasuna.mc.rubric.core.ConfigManager;
import com.oliveryasuna.mc.rubric.loader.Constants;
import com.oliveryasuna.mc.rubric.loader.config.Frontend;
import com.oliveryasuna.mc.rubric.loader.config.RubricConfig;
import com.oliveryasuna.mc.rubric.neoforge.gui.ClothScreenProvider;
import com.oliveryasuna.mc.rubric.neoforge.gui.Integrations;
import com.oliveryasuna.mc.rubric.neoforge.gui.RubricGui;
import com.oliveryasuna.mc.rubric.neoforge.gui.YaclScreenProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-side mod entry point. Registered as a dist-scoped {@link Mod} so it
 * only loads on physical clients.
 */
@Mod(value = Constants.OWN_MOD_ID, dist = Dist.CLIENT)
public final class RubricNeoForgeClientMod {

    //==================================================
    // Static fields
    //==================================================

    private static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    private static final String CLOTH_MOD_ID = "cloth_config";

    private static final String PREFERRED_FRONTEND_PATH = "gui.preferredFrontend";

    //==================================================
    // Constructors
    //==================================================

    public RubricNeoForgeClientMod(final IEventBus modEventBus, final ModContainer container) {
        super();

        if(ModList.get().isLoaded(YACL_MOD_ID)) {
            RubricGui.registerProvider(new YaclScreenProvider());
        }
        if(ModList.get().isLoaded(CLOTH_MOD_ID)) {
            RubricGui.registerProvider(new ClothScreenProvider());
        }

        // Enables the "Config" button next to Rubric in NG's mod list. The
        // factory is invoked every open, so late-registered providers still
        // apply.
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parent) -> Integrations.createOwnConfigScreen(parent)
        );

        // Wire Rubric's own preferredFrontend into RubricGui:
        // 1. Apply the current value once (the common bootstrap already
        //    loaded the manager in RubricNeoForgeMod's constructor).
        // 2. Subscribe to the path so a user edit propagates without restart.
        // 3. Register the self-config screen so users can edit it via
        //    a mod-list frontend.
        final ConfigManager<RubricConfig> manager = RubricNeoForgeMod.manager();
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
