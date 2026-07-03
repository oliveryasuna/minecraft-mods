package com.oliveryasuna.mc.coal.api.gui;

import com.oliveryasuna.mc.coal.api.config.ConfigManager;

public interface ScreenProvider {

    //==================================================
    // Methods
    //==================================================

    // E.g., "yacl", "cloth".
    String id();

    int priority();

    Screen create(
            Minecraft client,
            Screen parent,
            ConfigManager<?> manager
    );

}
