package com.oliveryasuna.mc.coal.api.sync;

public interface NetworkTransport {

    //==================================================
    // Methods
    //==================================================

    void sendToServer(byte[] payload);

    void sendToClient(
            Object clientHandle,
            byte[] payload
    );

    void sentToAllClients(byte[] payload);

    void subscribe(InboundHandler handler);

    void close();

    //==================================================
    // Nested
    //==================================================

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
