package com.oliveryasuna.mc.rubric.neoforge;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.loader.Constants;
import com.oliveryasuna.mc.rubric.loader.RubricSelf;
import com.oliveryasuna.mc.rubric.loader.RubricSerialization;
import com.oliveryasuna.mc.rubric.loader.config.RubricConfig;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Common-side mod entry point. Discovered via {@code neoforge.mods.toml}'s
 * javafml loader.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>
 *         Register the {@code rubric:sync} packet payload (both directions)
 *         on the mod event bus so {@code NeoForgeNetworkTransport} can
 *         send/receive.
 *     </li>
 *     <li>
 *         Load Rubric's own self-config so downstream consumers see a live
 *         {@link ConfigManager} at their own {@code @Mod} construction time.
 *     </li>
 * </ul>
 */
@Mod(Constants.OWN_MOD_ID)
public final class RubricNeoForgeMod {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("rubric");

    /**
     * Manager for Rubric's own self-config. Bootstrapped during the mod
     * constructor; null only if the initial load throws.
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
        // Defensive: caller may run before this constructor on weird load
        // orders. Return a fresh default so callers always see a usable
        // instance.
        return m != null ? m.get() : new RubricConfig();
    }

    //==================================================
    // Constructors
    //==================================================

    public RubricNeoForgeMod(final IEventBus modEventBus) {
        super();

        NeoForgeNetworkTransport.registerPayload(modEventBus);

        // Load Rubric's own config first — preferredFrontend / IO knobs /
        // sync knobs etc. need to be available before any consumer mod opens
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
            RubricNeoForgeMod.manager = own;
            // Shared MC-touching code reads self-config via this indirection —
            // avoids referring to loader-specific mod classes from mc-common.
            RubricSelf.configSupplier(own::get);
        } catch(final IOException e) {
            LOGGER.error("failed to load self-config; defaults in effect", e);
        }
    }

}
