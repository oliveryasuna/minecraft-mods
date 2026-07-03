package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.mc.rubric.core.ConfigManager;
import com.oliveryasuna.mc.rubric.loader.Constants;
import com.oliveryasuna.mc.rubric.fabric.gui.ConfigChooserScreen;
import com.oliveryasuna.mc.rubric.fabric.gui.Integrations;
import com.oliveryasuna.mc.rubric.fabric.gui.RubricGui;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModMenuIntegration implements ModMenuApi {

    //==================================================
    // Constructors
    //==================================================

    public ModMenuIntegration() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // ModMenu calls this method ONCE at mod-discovery time and caches the
        // returned factory. If we resolved the registry here (eagerly) and
        // returned a captured result, we'd bake in whatever state existed at
        // discovery time — often empty, since consumer client-init entrypoints
        // are still running. Return a factory that looks up the registry on
        // every click so late registrations still work.
        return Integrations::createOwnConfigScreen;
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        final Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        // Always surface entries even when no frontend (YACL) is present —
        // RubricGui.openFor returns a NoFrontendScreen placeholder that tells
        // the user how to install one and where the config file lives.
        // ModMenu calls this on every mod-list open, so managers registered
        // after game start still show up — no caching required here.
        for(final Map.Entry<String, List<ConfigManager<?>>> bucket : RubricGui.screensByModId().entrySet()) {
            final String modId = bucket.getKey();
            // Skip our own mod ID — surfaced via getModConfigScreenFactory();
            // ModMenu would silently drop it here anyway.
            if(Constants.OWN_MOD_ID.equals(modId)) {
                continue;
            }

            final List<ConfigManager<?>> managers = bucket.getValue();
            if(managers.size() == 1) {
                final ConfigManager<?> only = managers.getFirst();
                factories.put(modId, parent -> RubricGui.openFor(Minecraft.getInstance(), parent, only));
            } else {
                factories.put(modId, parent -> new ConfigChooserScreen(parent, modId, managers));
            }
        }

        return factories;
    }

}
