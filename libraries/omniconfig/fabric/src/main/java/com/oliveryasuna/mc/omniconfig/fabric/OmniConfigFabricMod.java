package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.fabric.config.OmniConfigConfig;
import com.oliveryasuna.mc.omniconfig.platform.Platform;
import com.oliveryasuna.mc.omniconfig.value.CodecRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("omniconfig");

    /**
     * Manager for OmniConfig's own self-config. Bootstrapped during
     * {@link #onInitialize}; null only if the initial load throws.
     */
    private static volatile ConfigManager<OmniConfigConfig> manager;

    //==================================================
    // Static methods
    //==================================================

    public static ConfigManager<OmniConfigConfig> manager() {
        return manager;
    }

    public static OmniConfigConfig config() {
        final ConfigManager<OmniConfigConfig> m = manager;
        // Defensive: caller may run before onInitialize on weird load orders.
        // Return a fresh default so callers always see a usable instance.
        return m != null ? m.get() : new OmniConfigConfig();
    }

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

        // Load OmniConfig's own config first — preferredFrontend / IO knobs
        // / sync knobs etc. need to be available before any consumer mod opens
        // a screen or sends a sync payload. CodecRegistry is empty: the self-
        // config has no MC leaf types.
        final ConfigManager<OmniConfigConfig> own = new ConfigManager<>(
                OmniConfigConfig.class,
                OmniConfigSerialization.defaultIO(),
                Loaders.platform(),
                new CodecRegistry()
        );
        try {
            own.load();
            OmniConfigFabricMod.manager = own;
        } catch(final IOException e) {
            LOGGER.error("failed to load self-config; defaults in effect", e);
        }

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
