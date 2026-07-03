package com.oliveryasuna.mc.rubric.neoforge;

import com.oliveryasuna.mc.rubric.sync.NetworkTransport;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * NeoForge {@link NetworkTransport}. Mojmap names; NeoForge 21.x payload
 * shape ({@link CustomPacketPayload} + {@link StreamCodec}) registered via
 * {@link RegisterPayloadHandlersEvent}.
 */
public final class NeoForgeNetworkTransport {

    //==================================================
    // Static fields
    //==================================================

    public static final CustomPacketPayload.Type<RubricPayload> PAYLOAD_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rubric", "sync"));

    public static final StreamCodec<ByteBuf, RubricPayload> PAYLOAD_CODEC =
            ByteBufCodecs.BYTE_ARRAY.map(RubricPayload::new, RubricPayload::bytes);

    private static volatile ServerImpl SERVER_SINGLETON;
    private static volatile ClientImpl CLIENT_SINGLETON;

    //==================================================
    // Static methods
    //==================================================

    /**
     * Registers the {@code rubric:sync} payload on the mod event bus. Called
     * from the mod's constructor; the singleton {@link ServerImpl}
     * / {@link ClientImpl} instances receive whatever bytes NeoForge delivers.
     * Idempotent per event bus.
     */
    public static void registerPayload(final IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetworkTransport::onRegisterPayloadHandlers);
    }

    public static NetworkTransport server() {
        if(SERVER_SINGLETON == null) {
            synchronized(NeoForgeNetworkTransport.class) {
                if(SERVER_SINGLETON == null) {
                    SERVER_SINGLETON = new ServerImpl();
                }
            }
        }
        return SERVER_SINGLETON;
    }

    public static NetworkTransport client() {
        if(CLIENT_SINGLETON == null) {
            synchronized(NeoForgeNetworkTransport.class) {
                if(CLIENT_SINGLETON == null) {
                    CLIENT_SINGLETON = new ClientImpl();
                }
            }
        }
        return CLIENT_SINGLETON;
    }

    @SubscribeEvent
    private static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        // Optional() so a server without the mod loaded doesn't force clients
        // to disconnect — consumer mods installed only on the client (frontend
        // dev) keep working against vanilla servers.
        //
        // playBidirectional takes TWO handlers (server, client). Passing only
        // one binds it as server-only, which trips ClientNetworkRegistry's
        // "clientbound payload without a client handler" check at world load.
        final PayloadRegistrar registrar = event.registrar("rubric").optional();
        registrar.playBidirectional(
                PAYLOAD_TYPE,
                PAYLOAD_CODEC,
                // Server-side handler: C2S ClientEdit + any client -> server
                // response.
                (payload, context) -> {
                    if(context.player() instanceof final ServerPlayer sp) {
                        final ServerImpl s = SERVER_SINGLETON;
                        if(s != null && s.handler != null) {
                            s.handler.onPayload(sp, payload.bytes());
                        }
                    }
                },
                // Client-side handler: S2C Handshake / Snapshot / Delta from
                // the server. SyncService re-dispatches to the main thread via
                // Platform — NG fires this on the network thread.
                (payload, context) -> {
                    final ClientImpl c = CLIENT_SINGLETON;
                    if(c != null && c.handler != null) {
                        c.handler.onPayload(null, payload.bytes());
                    }
                }
        );
    }

    //==================================================
    // Constructors
    //==================================================

    private NeoForgeNetworkTransport() {
        super();
    }

    //==================================================
    // Nested
    //==================================================

    public record RubricPayload(
            byte[] bytes
    ) implements CustomPacketPayload {

        //==================================================
        // Methods
        //==================================================

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_TYPE;
        }

    }

    private static final class ServerImpl implements NetworkTransport {

        //==================================================
        // Fields
        //==================================================

        private volatile InboundHandler handler;

        //==================================================
        // Constructors
        //==================================================

        public ServerImpl() {
            super();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public void sendToServer(final byte[] payload) {
            throw new UnsupportedOperationException("server-side transport cannot sendToServer");
        }

        @Override
        public void sendToClient(
                final Object clientHandle,
                final byte[] payload
        ) {
            if(clientHandle instanceof final ServerPlayer player) {
                if(isIntegratedHost(player)) {
                    return;
                }
                PacketDistributor.sendToPlayer(player, new RubricPayload(payload));
            }
        }

        @Override
        public void sendToAllClients(final byte[] payload) {
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if(server == null) return;
            for(final ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(isIntegratedHost(player)) {
                    continue;
                }
                PacketDistributor.sendToPlayer(player, new RubricPayload(payload));
            }
        }

        /**
         * The host of an integrated server shares the JVM (and the
         * {@code ConfigManager} instance) with the client. Sending sync
         * payloads to that player round-trips through the loopback network,
         * and the client-side handler then tags the values as
         * {@code FROM_REMOTE} — which would cause the next local save to
         * strip them. LAN guests still receive normally.
         */
        private boolean isIntegratedHost(final ServerPlayer player) {
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            return server != null
                   && server.isSingleplayer()
                   && server.isSingleplayerOwner(player.getGameProfile());
        }

        @Override
        public void subscribe(final InboundHandler handler) {
            this.handler = handler;
        }

        @Override
        public void close() {
            this.handler = null;
        }

    }

    private static final class ClientImpl implements NetworkTransport {

        //==================================================
        // Fields
        //==================================================

        private volatile InboundHandler handler;

        //==================================================
        // Constructors
        //==================================================

        public ClientImpl() {
            super();
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public void sendToServer(final byte[] payload) {
            // ClientPacketDistributor is the client-only counterpart of
            // PacketDistributor as of NG 21.8 (see the javadoc on
            // PacketDistributor). Only referenced here inside client dist code
            // paths — the class is client-only.
            ClientPacketDistributor.sendToServer(new RubricPayload(payload));
        }

        @Override
        public void sendToClient(
                final Object clientHandle,
                final byte[] payload
        ) {
            throw new UnsupportedOperationException("client-side transport cannot sendToClient");
        }

        @Override
        public void sendToAllClients(final byte[] payload) {
            throw new UnsupportedOperationException("client-side transport cannot broadcast");
        }

        @Override
        public void subscribe(final InboundHandler handler) {
            this.handler = handler;
        }

        @Override
        public void close() {
            this.handler = null;
        }

    }

}
