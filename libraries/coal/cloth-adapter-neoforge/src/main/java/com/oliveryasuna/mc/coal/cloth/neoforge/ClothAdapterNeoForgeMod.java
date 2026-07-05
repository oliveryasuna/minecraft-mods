package com.oliveryasuna.mc.coal.cloth.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entry point for the adapter. No work at mod-init — the COAL mod's
 * {@code CoalNeoForgeMod} calls {@code Coal.bootstrap()} and our
 * {@code ClothConfigProviderFactory} is picked up via {@code META-INF/services}.
 */
@Mod("coal_cloth_adapter")
public final class ClothAdapterNeoForgeMod {

    //==================================================
    // Constructors
    //==================================================

    public ClothAdapterNeoForgeMod(final IEventBus modEventBus) {
        super();
    }

}
