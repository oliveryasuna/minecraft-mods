package com.oliveryasuna.mc.coal.neoforge;

import com.oliveryasuna.mc.coal.api.Coal;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entry point for the COAL mod.
 * <p>
 * Kicks off {@link Coal#bootstrap()} eagerly so the discovered provider name
 * shows up in the log at mod-init rather than at the first
 * {@link Coal#register(Class)} call from some consumer mod later.
 * <p>
 * NG's server main-thread executor is looked up on-demand via
 * {@link net.neoforged.neoforge.server.ServerLifecycleHooks#getCurrentServer()}
 * in {@link NeoForgePlatform}, so no server-lifecycle event wiring is needed
 * here (unlike Fabric).
 */
@Mod("coal")
public final class CoalNeoForgeMod {

    //==================================================
    // Constructors
    //==================================================

    public CoalNeoForgeMod(final IEventBus modEventBus) {
        super();

        Coal.bootstrap();
    }

}
