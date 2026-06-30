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
    // Static fields
    //==================================================

    /**
     * The Fabric mod ID of this providing mod (matches {@code fabric.mod.json}).
     * ModMenu treats {@link ModMenuApi#getProvidedConfigScreenFactories()}
     * keys as <i>other</i> mods this provider serves — entries keyed by the
     * providing mod's own ID are silently dropped. The providing mod's own
     * screen has to come from {@link ModMenuApi#getModConfigScreenFactory()}.
     */
    private static final String OWN_MOD_ID = "omniconfig";

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
        final List<ConfigManager<?>> ours = OmniConfigGui.screensByModId().getOrDefault(OWN_MOD_ID, List.of());
        if(ours.isEmpty()) {
            return parent -> null;
        } else if(ours.size() == 1) {
            final ConfigManager<?> only = ours.getFirst();
            return parent -> OmniConfigGui.openFor(Minecraft.getInstance(), parent, only);
        }

        return parent -> new ConfigChooserScreen(parent, OWN_MOD_ID, ours);
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        final Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        // Always surface entries even when no frontend (YACL) is present —
        // OmniConfigGui.openFor returns a NoFrontendScreen placeholder that
        // tells the user how to install one and where the config file lives.
        // ModMenu calls this on every mod-list open, so managers registered
        // after game start still show up — no caching required here.
        for(final Map.Entry<String, List<ConfigManager<?>>> bucket : OmniConfigGui.screensByModId().entrySet()) {
            final String modId = bucket.getKey();
            // Skip our own mod ID — surfaced via getModConfigScreenFactory();
            // ModMenu would silently drop it here anyway.
            if(OWN_MOD_ID.equals(modId)) {
                continue;
            }

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
