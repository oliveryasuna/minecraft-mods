package com.oliveryasuna.mc.ssd.config;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;

/**
 * Typed, null-safe facade over the COAL-managed {@link SSDConfig}. All the rest
 * of the mod reads settings through here rather than touching the config handle
 * or nested config objects; each getter returns a sensible default before the
 * config is registered.
 */
public final class SSDSettings {

    //==================================================
    // Static fields
    //==================================================

    private static final int MAX_GLOW = 1;

    private static volatile ConfigHandle<SSDConfig> handle;

    //==================================================
    // Static methods
    //==================================================

    /**
     * Registers the config with COAL. Call once during common init.
     */
    public static void initialize() {
        handle = Coal.register(SSDConfig.class);
    }

    /**
     * The config handle (for the settings screen). Non-null after
     * {@link #initialize()}.
     */
    public static ConfigHandle<SSDConfig> handle() {
        return handle;
    }

    /**
     * Whether unlit segments are faintly drawn (default {@code true}).
     */
    public static boolean showUnlitSegments() {
        final ConfigHandle<SSDConfig> h = handle;

        return (h == null) || h.get().general.showUnlitSegments;
    }

    /**
     * Whether camo is restricted to full solid blocks (default {@code true}).
     */
    public static boolean solidBlocksOnly() {
        final ConfigHandle<SSDConfig> h = handle;

        return (h == null) || h.get().general.solidBlocksOnly;
    }

    /**
     * Glow strength around lit segments, clamped to {@code [0, MAX_GLOW]}
     * (default max).
     */
    public static int glowLevel() {
        final ConfigHandle<SSDConfig> h = handle;
        final int level = (h == null) ? MAX_GLOW : h.get().general.glowLevel;

        return Math.max(0, Math.min(MAX_GLOW, level));
    }

    /**
     * Debug: whether to outline joined display grids (default {@code false}).
     */
    public static boolean debugOutlineGrids() {
        final ConfigHandle<SSDConfig> h = handle;

        return (h != null) && h.get().debug.outlineGrids;
    }

    //==================================================
    // Constructors
    //==================================================

    private SSDSettings() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
