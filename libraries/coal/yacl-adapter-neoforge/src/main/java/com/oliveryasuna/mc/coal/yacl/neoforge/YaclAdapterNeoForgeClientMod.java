package com.oliveryasuna.mc.coal.yacl.neoforge;

import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Client-side NeoForge entry point. Registers {@link YaclScreenProvider} with
 * the COAL {@link GuiRegistry} at client-init.
 * <p>
 * Registered as a dist-scoped {@link Mod} so it only loads on physical
 * clients.
 */
@Mod(value = "coal_yacl_adapter", dist = Dist.CLIENT)
public final class YaclAdapterNeoForgeClientMod {

    //==================================================
    // Constructors
    //==================================================

    public YaclAdapterNeoForgeClientMod(
            final IEventBus modEventBus,
            final ModContainer container
    ) {
        super();

        GuiRegistry.registerProvider(new YaclScreenProvider());
    }

}
