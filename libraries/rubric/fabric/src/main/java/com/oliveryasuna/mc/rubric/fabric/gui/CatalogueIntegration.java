package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.loader.Constants;
import com.oliveryasuna.mc.rubric.loader.gui.ConfigChooserScreen;
import com.oliveryasuna.mc.rubric.loader.gui.Integrations;
import com.oliveryasuna.mc.rubric.loader.gui.RubricGui;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class CatalogueIntegration {

    //==================================================
    // Static methods
    //==================================================

    public static Screen createConfigScreen(
            final Screen parent,
            final ModContainer container
    ) {
        return Integrations.createOwnConfigScreen(parent);
    }

    public static Map<String, BiFunction<Screen, ModContainer, Screen>> createConfigProvider() {
        final Map<String, BiFunction<Screen, ModContainer, Screen>> factories = new LinkedHashMap<>();
        for(final Map.Entry<String, List<ConfigManager<?>>> bucket : RubricGui.screensByModId().entrySet()) {
            final String modId = bucket.getKey();
            if(Constants.OWN_MOD_ID.equals(modId)) {
                continue;
            }

            final List<ConfigManager<?>> managers = bucket.getValue();
            if(managers.size() == 1) {
                final ConfigManager<?> only = managers.getFirst();
                factories.put(modId, (parent, container) -> RubricGui.openFor(Minecraft.getInstance(), parent, only));
            } else {
                factories.put(modId, (parent, container) -> new ConfigChooserScreen(parent, modId, managers));
            }
        }

        return factories;
    }

    //==================================================
    // Constructors
    //==================================================

    private CatalogueIntegration() {
        super();
    }

}
