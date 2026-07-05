package com.oliveryasuna.mc.coal.api.sync;

import com.oliveryasuna.mc.coal.api.config.ConfigManager;

import java.util.List;
import java.util.Map;

public interface SyncService {

    //==================================================
    // Methods
    //==================================================

    Role role();

    ProtocolVersion protocol();

    // Registration
    //--------------------------------------------------

    void register(ConfigManager<?> manager);

    void unregister(ConfigManager<?> manager);

    // Lifecycle
    //--------------------------------------------------

    void start();

    void close();

    // Server-side
    //--------------------------------------------------

    // Called when a client connects
    //

    void onClientConnected(Object clientHandle);

    // Broadcast on manager change
    //

    void broadcastDelta(
            String configId,
            List<String> changedPaths
    );

    void broadcastSnapshot(String configId);

    // Client-side
    //--------------------------------------------------

    // Forward a local edit to the server
    //

    void sendClientEdit(
            String configId,
            Map<String, Object> entries
    );

    //==================================================
    // Nested
    //==================================================

    enum Role {

        //==================================================
        // Values
        //==================================================

        SERVER,

        CLIENT

    }

}
