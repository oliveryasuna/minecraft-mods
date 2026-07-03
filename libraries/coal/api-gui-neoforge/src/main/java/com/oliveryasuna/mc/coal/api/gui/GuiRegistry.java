package com.oliveryasuna.mc.coal.api.gui;

import com.oliveryasuna.commons.language.condition.Arguments;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.coal.api.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side registry for {@link ScreenProvider} implementations.
 * <p>
 * Providers (typically wired at client mod init by whichever COAL
 * implementation you're using — e.g. {@code coal-rubric-*}, plus any
 * frontend-integration mods) call {@link #registerProvider(ScreenProvider)} to
 * expose themselves. Mods call {@link #open(Minecraft, Screen, ConfigManager)}
 * to launch a settings screen.
 * <p>
 * Selection is priority-based (see {@link ScreenProvider#priority()}) — higher
 * wins. Mods have no per-call frontend override; user-preference resolution is
 * a provider concern outside the spec.
 * <p>
 * <b>Thread-safety.</b> Backed by {@link CopyOnWriteArrayList}. Safe to
 * register from any thread; safe to iterate/read from the client render thread
 * while registration is in progress.
 */
public final class GuiRegistry {

    //==================================================
    // Static fields
    //==================================================

    private static final List<ScreenProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    //==================================================
    // Static methods
    //==================================================

    /**
     * Registers a {@link ScreenProvider}. Registration order does not affect
     * selection — {@link ScreenProvider#priority()} does.
     */
    public static void registerProvider(final ScreenProvider provider) {
        PROVIDERS.add(Arguments.requireNotNull(provider, "provider"));
    }

    /**
     * @return The highest-priority registered {@link ScreenProvider}, or
     * {@link Optional#empty()} if none has been registered.
     */
    public static Optional<ScreenProvider> selected() {
        if(PROVIDERS.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(sortedByPriority().getFirst());
    }

    /**
     * Opens the settings screen for {@code manager}.
     * <p>
     * Selects the highest-priority registered {@link ScreenProvider} and
     * delegates to its
     * {@link ScreenProvider#create(Minecraft, Screen, ConfigManager)}. If that
     * returns {@code null} (the provider refused the config), walks the
     * remaining providers in descending-priority order until one returns a
     * non-null {@link Screen}.
     *
     * @throws IllegalStateException if no provider is registered, or every
     *                               registered provider returned {@code null}
     *                               from {@code create(...)}.
     */
    public static Screen open(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        Arguments.requireNotNull(manager, "manager");

        if(PROVIDERS.isEmpty()) {
            throw new IllegalStateException("GuiRegistry.open: no ScreenProvider registered. A COAL provider must call GuiRegistry.registerProvider(...) at client init.");
        }

        final List<ScreenProvider> ordered = sortedByPriority();
        for(final ScreenProvider provider : ordered) {
            final Screen screen = provider.create(client, parent, manager);
            if(screen != null) {
                return screen;
            }
        }

        throw new IllegalStateException("GuiRegistry.open: every registered ScreenProvider returned null from create(). Providers tried: " + ordered);
    }

    private static List<ScreenProvider> sortedByPriority() {
        final List<ScreenProvider> copy = new ArrayList<>(PROVIDERS);
        copy.sort(Comparator.comparingInt(ScreenProvider::priority).reversed());

        return copy;
    }

    //==================================================
    // Constructors
    //==================================================

    private GuiRegistry() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
