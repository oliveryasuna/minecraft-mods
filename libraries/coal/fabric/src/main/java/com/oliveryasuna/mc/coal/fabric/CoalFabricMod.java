package com.oliveryasuna.mc.coal.fabric;

import com.oliveryasuna.mc.coal.api.Coal;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Fabric entry point for the COAL mod.
 * <p>
 * Kicks off {@link Coal#bootstrap()} eagerly so the discovered provider name
 * shows up in the log at COAL mod-init, not at the first
 * {@link Coal#register(Class)} call from some consumer mod later.
 * <p>
 * Wires {@link FabricPlatform#setServer(net.minecraft.server.MinecraftServer)}
 * to server-lifecycle events so the main-thread executor targets the live
 * server on the server dist.
 */
public final class CoalFabricMod implements ModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public CoalFabricMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitialize() {
        // Eager bootstrap. Discovers the FabricPlatform installed via
        // ServiceLoader plus whichever ConfigProviderFactory has the highest
        // priority on classpath. Log line shows both.
        Coal.bootstrap();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if(FabricPlatform.installed != null) {
                FabricPlatform.installed.setServer(server);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if(FabricPlatform.installed != null) {
                FabricPlatform.installed.setServer(null);
            }
        });
    }

}
