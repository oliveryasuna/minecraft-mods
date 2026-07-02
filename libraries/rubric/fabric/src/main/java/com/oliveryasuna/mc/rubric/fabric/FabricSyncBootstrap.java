package com.oliveryasuna.mc.rubric.fabric;

import com.oliveryasuna.mc.rubric.platform.PermissionGate;
import com.oliveryasuna.mc.rubric.sync.NetworkTransport;
import com.oliveryasuna.mc.rubric.sync.SyncService;
import com.oliveryasuna.mc.rubric.value.CodecRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Loader-side helpers that wire a {@link SyncService} into Fabric's event
 * surface so consumers don't have to learn the event API to use sync.
 * Build a manager + codec registry, call {@link #installServer} on the
 * common entry point and {@link #installClient} on the client entry point,
 * then register the manager and (on the client side) hand the resulting
 * service to {@code RubricGui.registerSyncService(manager, service)}.
 */
public final class FabricSyncBootstrap {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Builds the server-side {@link SyncService}, hooks
     * {@code SERVER_STARTING} to attach the live server, and hooks the
     * play-connection JOIN event to dispatch handshake + initial snapshot
     * per joining player. Started before returning.
     *
     * @param codecs Codec registry shared with the consumer's managers.
     * @param gate   Permission gate consulted by {@code handleClientEdit}.
     * @return The configured, started service. Register managers on it.
     */
    public static SyncService installServer(
            final CodecRegistry codecs,
            final PermissionGate gate
    ) {
        final NetworkTransport transport = FabricNetworkTransport.server();
        final SyncService syncService = new SyncService(SyncService.Role.SERVER, transport, codecs, gate);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> FabricNetworkTransport.attachServer(transport, server));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncService.onClientConnected(handler.player));
        syncService.start();
        return syncService;
    }

    /**
     * Builds the client-side {@link SyncService} around the client transport
     * (whose constructor self-registers the payload receiver) and starts it.
     *
     * @param codecs Codec registry shared with the consumer's managers.
     * @return The configured, started service. Register managers on it and
     * hand to {@code RubricGui.registerSyncService} so the screen's
     * Save &amp; Exit can forward dirty server-scoped edits.
     */
    public static SyncService installClient(final CodecRegistry codecs) {
        final NetworkTransport transport = FabricNetworkTransport.client();
        final SyncService syncService = new SyncService(SyncService.Role.CLIENT, transport, codecs);
        syncService.start();
        return syncService;
    }

    //==================================================
    // Constructors
    //==================================================

    private FabricSyncBootstrap() {
        super();
    }

}
