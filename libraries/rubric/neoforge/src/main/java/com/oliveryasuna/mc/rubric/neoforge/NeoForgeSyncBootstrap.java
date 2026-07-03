package com.oliveryasuna.mc.rubric.neoforge;

import com.oliveryasuna.mc.rubric.platform.PermissionGate;
import com.oliveryasuna.mc.rubric.sync.NetworkTransport;
import com.oliveryasuna.mc.rubric.sync.SyncService;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Loader-side helpers that wire a {@link SyncService} into NeoForge's event
 * surface so consumers don't have to learn the event API to use sync. Build a
 * manager + codec registry, call {@link #installServer} on the common entry
 * point and {@link #installClient} on the client entry point, then register
 * the manager and (on the client side) hand the resulting service to
 * {@code RubricGui.registerSyncService(manager, service)}.
 */
public final class NeoForgeSyncBootstrap {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Builds the server-side {@link SyncService}, hooks
     * {@code PlayerEvent.PlayerLoggedInEvent} to dispatch handshake + initial
     * snapshot per joining player. Started before returning.
     *
     * @param codecs Codec registry shared with the consumer's managers.
     * @param gate   Permission gate consulted by {@code handleClientEdit}.
     * @return The configured, started service. Register managers on it.
     */
    public static SyncService installServer(
            final CodecRegistry codecs,
            final PermissionGate gate
    ) {
        final NetworkTransport transport = NeoForgeNetworkTransport.server();
        final SyncService syncService = new SyncService(SyncService.Role.SERVER, transport, codecs, gate);
        NeoForge.EVENT_BUS.addListener((final PlayerEvent.PlayerLoggedInEvent event) ->
                syncService.onClientConnected(event.getEntity()));
        syncService.start();
        return syncService;
    }

    /**
     * Builds the client-side {@link SyncService} around the client transport
     * (payload receiver is wired via {@code registerPayload} on the mod event
     * bus) and starts it.
     *
     * @param codecs Codec registry shared with the consumer's managers.
     * @return The configured, started service. Register managers on it and
     * hand to {@code RubricGui.registerSyncService} so the screen's
     * Save &amp; Exit can forward dirty server-scoped edits.
     */
    public static SyncService installClient(final CodecRegistry codecs) {
        final NetworkTransport transport = NeoForgeNetworkTransport.client();
        final SyncService syncService = new SyncService(SyncService.Role.CLIENT, transport, codecs);
        syncService.start();
        return syncService;
    }

    //==================================================
    // Constructors
    //==================================================

    private NeoForgeSyncBootstrap() {
        super();
    }

}
