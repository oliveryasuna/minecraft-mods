package com.oliveryasuna.mc.coal.yacl.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entry point for the adapter. No work at mod-init — the COAL mod's
 * {@code CoalNeoForgeMod} calls {@code Coal.bootstrap()} and our
 * {@code YaclConfigProviderFactory} is picked up via {@code META-INF/services}.
 * This class exists so NG has a {@code @Mod}-annotated entrypoint to load the
 * main-dist classes.
 */
@Mod("coal_yacl_adapter")
public final class YaclAdapterNeoForgeMod {

    //==================================================
    // Constructors
    //==================================================

    public YaclAdapterNeoForgeMod(final IEventBus modEventBus) {
        super();
    }

}
