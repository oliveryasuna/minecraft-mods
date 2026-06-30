package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.mc.omniconfig.platform.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Server-side mod entry point. Registered via {@code fabric.mod.json}'s
 * {@code entrypoints.main}.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>
 *         Register the {@code omniconfig:sync} packet payload (server + client
 *         directions) so {@code FabricNetworkTransport} can send/receive.
 *     </li>
 *     <li>
 *         Hook server-start / server-stop lifecycle events so
 *         {@link FabricPlatform#setServer} keeps the platform's main-thread
 *         executor pointed at the live {@code MinecraftServer}.
 *     </li>
 * </ul>
 */
public final class OmniConfigFabricMod implements ModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public OmniConfigFabricMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitialize() {
        FabricNetworkTransport.registerPayload();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            final Platform platform = Loaders.platform();
            if(platform instanceof final FabricPlatform fp) {
                fp.setServer(server);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            final Platform platform = Loaders.platform();
            if(platform instanceof final FabricPlatform fp) {
                fp.setServer(null);
            }
        });
    }

}
