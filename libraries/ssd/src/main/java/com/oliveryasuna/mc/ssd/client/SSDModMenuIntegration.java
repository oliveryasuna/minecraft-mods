package com.oliveryasuna.mc.ssd.client;

import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import com.oliveryasuna.mc.ssd.SSDMod;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;

/**
 * Exposes the SSD config in Mod Menu. The screen itself is built by whichever
 * COAL GUI adapter is installed (YACL), via {@link GuiRegistry#open}.
 */
public final class SSDModMenuIntegration implements ModMenuApi {

    //==================================================
    // Constructors
    //==================================================

    public SSDModMenuIntegration() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> GuiRegistry.open(Minecraft.getInstance(), parent, SSDMod.config().manager());
    }

}
