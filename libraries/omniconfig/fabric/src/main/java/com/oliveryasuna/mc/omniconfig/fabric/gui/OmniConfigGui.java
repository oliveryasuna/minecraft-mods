package com.oliveryasuna.mc.omniconfig.fabric.gui;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.sync.SyncService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OmniConfigGui {

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
     */
    public static Screen openFor(
            final Minecraft client,
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        Objects.requireNonNull(manager, "manager");

        for(final ScreenProvider provider : PROVIDERS) {
            final Screen screen = provider.create(client, parent, manager);
            if(screen != null) {
                return screen;
            }
        }

        return new NoFrontendScreen(parent, manager);
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

    private OmniConfigGui() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
