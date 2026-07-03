package com.oliveryasuna.mc.coal.api.gui;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.config.ConfigManager;

import java.util.Optional;

public final class GuiRegistry {

    //==================================================
    // Static methods
    //==================================================

    // Client-side only. Providers register their frontends here; mods query it.
    public static void registerProvider(final ScreenProvider provider) {
        // TODO: Implement.
    }

    /**
     * Whichever {@link ScreenProvider} the current provider selected
     * (user preference, priority, etc).
     */
    public static Optional<ScreenProvider> selected() {
        // TODO: Implement.
    }

    /**
     * Opens a settings screen for {@code manager}.
     * <p>
     * The provider chooses which {@link ScreenProvider} to use — mods don't
     * pick the frontend.
     */
    public static Screen open(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        // TODO: Implement.
    }

    //==================================================
    // Constructors
    //==================================================

    private GuiRegistry() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
