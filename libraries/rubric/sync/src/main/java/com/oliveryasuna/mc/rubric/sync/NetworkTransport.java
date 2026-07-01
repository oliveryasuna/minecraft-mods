package com.oliveryasuna.mc.rubric.sync;

/**
 * SPI for shipping
 * {@link com.oliveryasuna.mc.rubric.sync.protocol.SyncPayload} bytes between
 * server and clients.
 * <p>
 * One implementation per loader (Fabric, NeoForge). The {@code sync} module is
 * Minecraft-agnostic and never references {@code ServerPlayer},
 * {@code MinecraftServer}, etc. — those types are owned by the loader adapter,
 * which is also responsible for picking a packet channel name and choosing
 * between the loader's networking APIs.
 * <p>
 * Methods are role-explicit:
 * <ul>
 *     <li>
 *         A <strong>server-side</strong> transport implements
 *         {@link #sendToClient(Object, byte[])} and
 *         {@link #sendToAllClients(byte[])}; calling
 *         {@link #sendToServer(byte[])} is undefined.
 *     </li>
 *     <li>
 *         A <strong>client-side</strong> transport implements
 *         {@link #sendToServer(byte[])}; calling the other two is undefined.
 *     </li>
 *     <li>
 *         Both sides implement {@link #subscribe(InboundHandler)} to receive
 *         payloads.
 *     </li>
 * </ul>
 * The shape of {@code clientHandle} and {@code source} is opaque to
 * {@code sync}; tests use a trivial record, loader adapters use their
 * platform's player-identifier type.
 * The {@link com.oliveryasuna.mc.rubric.platform.PermissionGate} from
 * {@code core} is queried by the consumer at the point of inbound
 * application — the transport itself does not enforce permissions.
 */
public interface NetworkTransport extends AutoCloseable {

    //==================================================
    // Methods
    //==================================================

    void sendToServer(byte[] payload);

    void sendToClient(
            Object clientHandle,
            byte[] payload
    );

    void sendToAllClients(byte[] payload);

    void subscribe(InboundHandler handler);

    @Override
    void close();

    //==================================================
    // Nested
    //==================================================

    /**
     * Callback for inbound bytes. {@code source} is the opaque handle of the
     * remote endpoint that sent the payload — on client-side transports this
     * may always be {@code null} (the only possible source is the server).
     */
    @FunctionalInterface
    interface InboundHandler {

        //==================================================
        // Methods
        //==================================================

        void onPayload(
                Object source,
                byte[] payload
        );

    }

}
