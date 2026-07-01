package com.oliveryasuna.mc.rubric.fabric;

import com.oliveryasuna.mc.rubric.sync.NetworkTransport;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric {@link NetworkTransport}. Mojmap names; Fabric API 1.21.x payload
 * shape ({@link CustomPacketPayload} + {@link StreamCodec}).
 */
public final class FabricNetworkTransport {

    //==================================================
    // Static fields
    //==================================================

    public static final CustomPacketPayload.Type<RubricPayload> PAYLOAD_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("rubric", "sync"));

    public static final StreamCodec<ByteBuf, RubricPayload> PAYLOAD_CODEC =
            ByteBufCodecs.BYTE_ARRAY.map(RubricPayload::new, RubricPayload::bytes);

    //==================================================
    // Static methods
    //==================================================

    public static NetworkTransport server() {
        return new ServerImpl();
    }

    public static NetworkTransport client() {
        return new ClientImpl();
    }

    public static void registerPayload() {
        PayloadTypeRegistry.playS2C().register(PAYLOAD_TYPE, PAYLOAD_CODEC);
        PayloadTypeRegistry.playC2S().register(PAYLOAD_TYPE, PAYLOAD_CODEC);
    }

    /**
     * Attaches a live {@link MinecraftServer} to the server-side transport
     * — required before {@link NetworkTransport#sendToAllClients} can find
     * recipients. {@link FabricSyncBootstrap#installServer} wires this on
     * {@code ServerLifecycleEvents.SERVER_STARTING}; consumers shouldn't
     * need to call it directly.
     *
     * @param transport The server-side transport returned by {@link #server}.
     * @param server    The live server.
     * @throws IllegalArgumentException if {@code transport} isn't a
     *                                  server-side transport from this class.
     */
    public static void attachServer(
            final NetworkTransport transport,
            final MinecraftServer server
    ) {
        if(!(transport instanceof final ServerImpl serverImpl)) {
            throw new IllegalArgumentException("expected a server-side FabricNetworkTransport, got " + transport.getClass().getName());
        }
        serverImpl.attach(server);
    }

    //==================================================
    // Constructors
    //==================================================

    private FabricNetworkTransport() {
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

        private MinecraftServer server;
        private InboundHandler handler;

        //==================================================
        // Constructors
        //==================================================

        public ServerImpl() {
            super();
        }

        //==================================================
        // Methods
        //==================================================

        void attach(final MinecraftServer server) {
            this.server = server;
            ServerPlayNetworking.registerGlobalReceiver(PAYLOAD_TYPE, (payload, ctx) -> {
                if(handler != null) {
                    handler.onPayload(ctx.player(), payload.bytes());
                }
            });
        }

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
                ServerPlayNetworking.send(player, new RubricPayload(payload));
            }
        }

        @Override
        public void sendToAllClients(final byte[] payload) {
            if(server == null) return;
            for(final ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(isIntegratedHost(player)) {
                    continue;
                }
                ServerPlayNetworking.send(player, new RubricPayload(payload));
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
            this.server = null;
        }

    }

    private static final class ClientImpl implements NetworkTransport {

        //==================================================
        // Fields
        //==================================================

        private InboundHandler handler;

        //==================================================
        // Constructors
        //==================================================

        public ClientImpl() {
            super();

            ClientPlayNetworking.registerGlobalReceiver(PAYLOAD_TYPE, (payload, ctx) -> {
                if(handler != null) {
                    handler.onPayload(null, payload.bytes());
                }
            });
        }

        //==================================================
        // Methods
        //==================================================

        @Override
        public void sendToServer(final byte[] payload) {
            ClientPlayNetworking.send(new RubricPayload(payload));
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
