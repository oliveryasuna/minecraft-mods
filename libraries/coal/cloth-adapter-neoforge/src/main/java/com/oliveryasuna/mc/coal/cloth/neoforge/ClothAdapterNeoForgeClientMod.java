package com.oliveryasuna.mc.coal.cloth.neoforge;

import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Client-side NeoForge entry point. Registers {@link ClothScreenProvider} with
 * the COAL {@link GuiRegistry} at client-init. Priority {@code 40} — leaves the
 * higher slot to the YACL adapter when both are installed.
 */
@Mod(value = "coal_cloth_adapter", dist = Dist.CLIENT)
public final class ClothAdapterNeoForgeClientMod {

    //==================================================
    // Constructors
    //==================================================

    public ClothAdapterNeoForgeClientMod(
            final IEventBus modEventBus,
            final ModContainer container
    ) {
        super();

        GuiRegistry.registerProvider(new ClothScreenProvider());
    }

}
