package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.core.ConfigManager;
import com.oliveryasuna.mc.rubric.loader.config.Frontend;
import com.oliveryasuna.mc.rubric.sync.SyncService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class RubricGui {

    //==================================================
    // Static fields
    //==================================================

    private static final Map<String, ConfigManager<?>> REGISTRY = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Mod ID owning each registered manager. Used by the ModMenu integration
     * to group multiple configs under the same mod entry. Defaults to
     * {@code schema.id()} when no explicit mod ID is provided.
     */
    private static final Map<String, String> MOD_ID_BY_SCHEMA = new ConcurrentHashMap<>();

    private static final List<ScreenProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    /**
     * Frontend preference applied by {@link #openFor}. On AUTO and ties, YACL
     * wins (then CLOTH, then any registered provider). Updated by the
     * Rubric bootstrap after loading {@code RubricConfig.gui.preferredFrontend}.
     */
    private static final AtomicReference<Frontend> PREFERRED_FRONTEND = new AtomicReference<>(Frontend.AUTO);

    /**
     * AUTO fallback order — first match wins. YACL ahead of CLOTH per the
     * documented default; extend when new frontends land.
     */
    private static final List<String> AUTO_PRIORITY = List.of("yacl", "cloth");

    /**
     * Per-config-id {@link SyncService} channel. The screen's save consults
     * this to forward dirty {@code SERVER}/{@code COMMON} entries to the server
     * as a {@link SyncService#sendClientEdit ClientEdit}. Consumers wire one
     * entry per synced config at mod init; configs with no entry skip the
     * client-edit path entirely.
     */
    private static final Map<String, SyncService> SYNC_BY_ID = new ConcurrentHashMap<>();

    //==================================================
    // Static methods
    //==================================================

    // Screen
    //--------------------------------------------------

    /**
     * Registers a manager, defaulting its ModMenu mod ID to
     * {@code manager.getSchema().id()}. Equivalent to
     * {@link #registerScreen(String, ConfigManager) registerScreen(manager.getSchema().id(), manager)}.
     */
    public static void registerScreen(final ConfigManager<?> manager) {
        Objects.requireNonNull(manager, "manager");
        registerScreen(manager.getSchema().id(), manager);
    }

    /**
     * Registers a manager explicitly under {@code modId}. The mod ID groups
     * multiple managers under one ModMenu entry: if a single mod owns more
     * than one config, register each here with the same {@code modId} and the
     * ModMenu integration will show a chooser sub-screen instead of dropping
     * all but one.
     */
    public static void registerScreen(
            final String modId,
            final ConfigManager<?> manager
    ) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(manager, "manager");

        REGISTRY.put(manager.getSchema().id(), manager);
        MOD_ID_BY_SCHEMA.put(manager.getSchema().id(), modId);
    }

    public static void unregisterScreen(final ConfigManager<?> manager) {
        unregisterScreen(manager.getSchema().id());
    }

    public static void unregisterScreen(final String schemaId) {
        REGISTRY.remove(schemaId);
        MOD_ID_BY_SCHEMA.remove(schemaId);
    }

    public static Optional<ConfigManager<?>> getScreen(final String schemaId) {
        return Optional.ofNullable(REGISTRY.get(schemaId));
    }

    public static Collection<ConfigManager<?>> registeredScreens() {
        synchronized(REGISTRY) {
            return List.copyOf(REGISTRY.values());
        }
    }

    /**
     * @return The mod ID under which {@code manager} is registered, or the
     * schema id if it was registered with the no-modId overload.
     */
    public static String modIdOf(final ConfigManager<?> manager) {
        return MOD_ID_BY_SCHEMA.getOrDefault(manager.getSchema().id(), manager.getSchema().id());
    }

    /**
     * Groups every registered manager by its ModMenu mod ID. Insertion-ordered
     * within each bucket so chooser entries stay stable across game restarts.
     */
    public static Map<String, List<ConfigManager<?>>> screensByModId() {
        final Map<String, List<ConfigManager<?>>> grouped = new LinkedHashMap<>();
        synchronized(REGISTRY) {
            for(final ConfigManager<?> manager : REGISTRY.values()) {
                grouped.computeIfAbsent(modIdOf(manager), k -> new ArrayList<>()).add(manager);
            }
        }
        return grouped;
    }

    // Provider
    //--------------------------------------------------

    /**
     * Opens the GUI for {@code manager}, falling back to a
     * {@link NoFrontendScreen} placeholder when no {@link ScreenProvider} is
     * registered (e.g., YACL absent). Never returns {@code null} or throws —
     * callers can wire this straight into ModMenu / Catalogue screen
     * factories without guarding.
     * <p>
     * Selection order: the configured preferred frontend (see
     * {@link #setPreferredFrontend}) first; on AUTO or when the pinned
     * frontend isn't installed, fall through {@link #AUTO_PRIORITY} (YACL,
     * then CLOTH); finally, any other registered provider in registration
     * order.
     */
    public static Screen openFor(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        Objects.requireNonNull(manager, "manager");

        final ScreenProvider selected = selectProvider();
        if(selected != null) {
            final Screen screen = selected.create(client, parent, manager);
            if(screen != null) {
                return screen;
            }
        }
        // Selected provider returned null (refused) — try every other in turn.
        for(final ScreenProvider provider : PROVIDERS) {
            if(provider == selected) {
                continue;
            }
            final Screen screen = provider.create(client, parent, manager);
            if(screen != null) {
                return screen;
            }
        }

        return new NoFrontendScreen(parent, manager);
    }

    private static ScreenProvider selectProvider() {
        if(PROVIDERS.isEmpty()) {
            return null;
        }

        final Frontend preferred = PREFERRED_FRONTEND.get();
        // Pinned, non-AUTO: try the exact match first; absent → fall through.
        if(preferred != null && preferred != Frontend.AUTO) {
            final String id = preferred.name().toLowerCase(Locale.ROOT);
            final ScreenProvider exact = providerById(id);
            if(exact != null) {
                return exact;
            }
        }

        // AUTO or pin-missed: walk the documented priority order.
        for(final String id : AUTO_PRIORITY) {
            final ScreenProvider p = providerById(id);
            if(p != null) {
                return p;
            }
        }

        // Unknown frontend that isn't in the priority list — just take the
        // first registered.
        return PROVIDERS.getFirst();
    }

    private static ScreenProvider providerById(final String id) {
        for(final ScreenProvider p : PROVIDERS) {
            if(id.equals(p.id())) {
                return p;
            }
        }

        return null;
    }

    /**
     * Updates the frontend preference consulted by {@link #openFor}. Called
     * by the Rubric bootstrap after loading
     * {@code RubricConfig.gui.preferredFrontend}, and again on every
     * change event for that path.
     */
    public static void setPreferredFrontend(final Frontend preferred) {
        PREFERRED_FRONTEND.set(Objects.requireNonNull(preferred, "preferred"));
    }

    public static Frontend getPreferredFrontend() {
        return PREFERRED_FRONTEND.get();
    }

    public static void registerProvider(final ScreenProvider provider) {
        PROVIDERS.add(Objects.requireNonNull(provider, "provider"));
    }

    /**
     * @return {@code true} when at least one {@link ScreenProvider} (YACL,
     * Cloth Config, etc.) has been registered. Callers that build menu
     * entries should guard on this — opening a config when no provider exists
     * throws from {@link #openFor}.
     */
    public static boolean hasProvider() {
        return !PROVIDERS.isEmpty();
    }

    // Sync service
    //--------------------------------------------------

    public static void registerSyncService(
            final ConfigManager<?> manager,
            final SyncService syncService
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(syncService, "syncService");

        SYNC_BY_ID.put(manager.getSchema().id(), syncService);
    }

    public static Optional<SyncService> getSyncService(final ConfigManager<?> manager) {
        return Optional.ofNullable(SYNC_BY_ID.get(manager.getSchema().id()));
    }

    public static Optional<SyncService> getSyncService(final String id) {
        return Optional.ofNullable(SYNC_BY_ID.get(id));
    }

    //==================================================
    // Constructors
    //==================================================

    private RubricGui() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
