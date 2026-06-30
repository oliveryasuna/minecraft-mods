package com.oliveryasuna.mc.omniconfig.fabric.gui;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * SPI for providing a screen for editing a configuration.
 */
@FunctionalInterface
public interface ScreenProvider {

    //==================================================
    // Methods
    //==================================================

    Screen create(
            Minecraft client,
            Screen parent,
            ConfigManager<?> manager
    );

}
