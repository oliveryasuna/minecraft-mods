package com.oliveryasuna.mc.omniconfig.sync.protocol;

import com.oliveryasuna.mc.omniconfig.value.ValueNode;
import com.oliveryasuna.mc.omniconfig.value.ValueTree;

import java.util.List;

/**
 * One unit of sync traffic. Permitted shapes:
 * <ul>
 *     <li>
 *         {@link Handshake} — exchanged once per connection. Both sides
 *         declare the wire-protocol version they speak and the set of config
 *         ids they intend to participate in. A mismatch fails the connection.
 *     </li>
 *     <li>
 *         {@link Snapshot} — the authoritative side ships every
 *         {@code SERVER}/{@code COMMON} value for one config at once. Sent
 *         on join and whenever the authoritative side detects its own config
 *         changed.
 *     </li>
 *     <li>
 *         {@link Delta} — a list of single-entry changes for one config.
 *         Smaller than a snapshot when only a handful of entries moved.
 *         Receivers must accept either a Snapshot or a stream of Deltas — the
 *         producer decides which is more efficient.
 *     </li>
 *     <li>
 *         {@link ClientEdit} — a client-to-server request to mutate one or
 *         more {@code SERVER}/{@code COMMON} entries. The server runs the
 *         actor through its {@code PermissionGate} and the payload through
 *         the {@code InboundValidator} before applying anything; the
 *         accepted subset is broadcast back as a {@link Delta} so every
 *         connected client (including the originator) settles on the
 *         server-confirmed state.
 *     </li>
 * </ul>
 * Wire encoding is handled by {@link PayloadCodec}.
 */
public sealed interface SyncPayload permits SyncPayload.Handshake, SyncPayload.Snapshot, SyncPayload.Delta, SyncPayload.ClientEdit {

    //==================================================
    // Nested
    //==================================================

    record Handshake(
            int protocolVersion,
            List<String> configIds
    ) implements SyncPayload {

        //==================================================
        // Constructors
        //==================================================

        public Handshake {
            configIds = List.copyOf(configIds);
        }

    }

    record Snapshot(
            String configId,
            ValueTree tree
    ) implements SyncPayload {

    }

    record Delta(
            String configId,
            List<Entry> entries
    ) implements SyncPayload {

        //==================================================
        // Constructors
        //==================================================

        public Delta {
            entries = List.copyOf(entries);
        }

        //==================================================
        // Nested
        //==================================================

        public record Entry(
                String path,
                ValueNode value
        ) {

        }

    }

    record ClientEdit(
            String configId,
            List<Delta.Entry> entries
    ) implements SyncPayload {

        //==================================================
        // Constructors
        //==================================================

        public ClientEdit {
            entries = List.copyOf(entries);
        }

    }

}
