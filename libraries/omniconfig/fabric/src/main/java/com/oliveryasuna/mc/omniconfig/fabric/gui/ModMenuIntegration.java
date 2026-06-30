package com.oliveryasuna.mc.omniconfig.fabric.gui;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
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
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        final Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        // Skip entirely if no provider (YACL absent) — otherwise ModMenu would
        // surface entries that throw IllegalStateException on click.
        if(!OmniConfigGui.hasProvider()) {
            return factories;
        }

        // ModMenu calls this on every mod-list open, so managers registered
        // after game start still show up — no caching required here.
        for(final Map.Entry<String, List<ConfigManager<?>>> bucket : OmniConfigGui.screensByModId().entrySet()) {
            final String modId = bucket.getKey();
            final List<ConfigManager<?>> managers = bucket.getValue();
            if(managers.size() == 1) {
                final ConfigManager<?> only = managers.getFirst();
                factories.put(modId, parent -> OmniConfigGui.openFor(Minecraft.getInstance(), parent, only));
            } else {
                factories.put(modId, parent -> new ConfigChooserScreen(parent, modId, managers));
            }
        }

        return factories;
    }

}
