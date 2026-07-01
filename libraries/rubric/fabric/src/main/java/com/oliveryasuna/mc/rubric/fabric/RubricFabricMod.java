package com.oliveryasuna.mc.rubric.fabric;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.fabric.config.RubricConfig;
import com.oliveryasuna.mc.rubric.platform.Platform;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
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
 *         Register the {@code rubric:sync} packet payload (server + client
 *         directions) so {@code FabricNetworkTransport} can send/receive.
 *     </li>
 *     <li>
 *         Hook server-start / server-stop lifecycle events so
 *         {@link FabricPlatform#setServer} keeps the platform's main-thread
 *         executor pointed at the live {@code MinecraftServer}.
 *     </li>
 * </ul>
 */
public final class RubricFabricMod implements ModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("rubric");

    /**
     * Manager for Rubric's own self-config. Bootstrapped during
     * {@link #onInitialize}; null only if the initial load throws.
     */
    private static volatile ConfigManager<RubricConfig> manager;

    //==================================================
    // Static methods
    //==================================================

    public static ConfigManager<RubricConfig> manager() {
        return manager;
    }

    public static RubricConfig config() {
        final ConfigManager<RubricConfig> m = manager;
        // Defensive: caller may run before onInitialize on weird load orders.
        // Return a fresh default so callers always see a usable instance.
        return m != null ? m.get() : new RubricConfig();
    }

    //==================================================
    // Constructors
    //==================================================

    public RubricFabricMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitialize() {
        FabricNetworkTransport.registerPayload();

        // Load Rubric's own config first — preferredFrontend / IO knobs
        // / sync knobs etc. need to be available before any consumer mod opens
        // a screen or sends a sync payload. CodecRegistry is empty: the self-
        // config has no MC leaf types.
        final ConfigManager<RubricConfig> own = new ConfigManager<>(
                RubricConfig.class,
                RubricSerialization.defaultIO(),
                Loaders.platform(),
                new CodecRegistry()
        );
        try {
            own.load();
            RubricFabricMod.manager = own;
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
