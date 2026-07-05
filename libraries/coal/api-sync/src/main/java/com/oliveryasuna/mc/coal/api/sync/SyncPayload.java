package com.oliveryasuna.mc.coal.api.sync;

import java.util.Map;
import java.util.Set;

public sealed interface SyncPayload permits SyncPayload.Handshake, SyncPayload.Snapshot, SyncPayload.Delta, SyncPayload.ClientEdit {

    //==================================================
    // Nested
    //==================================================

    record Handshake(
            ProtocolVersion protocol,
            Set<String> knownConfigIds
    ) implements SyncPayload {

    }

    record Snapshot(
            String configId,
            Map<String, Object> values
    ) implements SyncPayload {

    }

    record Delta(
            String configId,
            Map<String, Object> changed
    ) implements SyncPayload {

    }

    record ClientEdit(
            String configId,
            Map<String, Object> entries
    ) implements SyncPayload {

    }

}
