package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.loader.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class Integrations {

    //==================================================
    // Static methods
    //==================================================

    public static Screen createOwnConfigScreen(final Screen parent) {
        final List<ConfigManager<?>> ours = RubricGui.screensByModId().getOrDefault(Constants.OWN_MOD_ID, List.of());
        if(ours.isEmpty()) {
            return null;
        }

        if(ours.size() == 1) {
            return RubricGui.openFor(Minecraft.getInstance(), parent, ours.getFirst());
        }

        return new ConfigChooserScreen(parent, Constants.OWN_MOD_ID, ours);
    }

    //==================================================
    // Constructors
    //==================================================

    private Integrations() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
