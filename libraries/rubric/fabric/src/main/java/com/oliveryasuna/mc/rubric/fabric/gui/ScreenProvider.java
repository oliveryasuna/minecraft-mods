package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.mc.rubric.core.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * SPI for providing a screen for editing a configuration.
 */
public interface ScreenProvider {

    //==================================================
    // Methods
    //==================================================

    /**
     * Stable identifier for this frontend, used by
     * {@code RubricGui.setPreferredFrontend} to pick one when multiple
     * providers are registered. Lowercase, dashed: e.g. {@code "yacl"},
     * {@code "cloth"}.
     */
    String id();

    Screen create(
            Minecraft client,
            Screen parent,
            ConfigManager<?> manager
    );

}
