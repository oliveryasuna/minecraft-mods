---
title: Server ↔ client sync
---

# Server ↔ client sync

Config values marked `@Sync(SERVER)` or `@Sync(COMMON)` are pushed from the server to connected clients. The SPI lives in `coal-api-sync` and is optional — providers that don't ship it return `false` for `Capability.SYNC`, and the annotations become metadata read but not acted on.

Neither the YACL nor the Cloth adapter ships sync in v1. This page documents the SPI so you know what the annotations promise and what a `SYNC`-capable provider would do.

## The scopes

[`@Sync(Scope)`](../reference/annotations#sync) on a field or type declares ownership and propagation:

| Scope    | Ownership       | Behavior when `SYNC` is advertised                                                        |
|----------|-----------------|-------------------------------------------------------------------------------------------|
| `CLIENT` | Client-owned    | Local only. Never transmitted. **Default when `@Sync` is absent.**                        |
| `SERVER` | Server-authoritative | Pushed to clients for the session. Client cannot override; not written to client disk. |
| `COMMON` | Server-authoritative when connected; local defaults in singleplayer | Enforced like `SERVER` in multiplayer. |

The scope can be set at type level (default for every entry) or field level (per-entry override).

## The wire types

Full definitions in [`coal-api-sync`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/api-sync/src/main/java/com/oliveryasuna/mc/coal/api/sync). Normative rules in [spec §16](../spec/#16-optional-module--synchronization-coal-api-sync).

### `ProtocolVersion`

```java
public record ProtocolVersion(int major, int minor) implements Comparable<ProtocolVersion> {
    public static final ProtocolVersion CURRENT = new ProtocolVersion(1, 0);
    public boolean isCompatibleWith(ProtocolVersion other) { ... }
}
```

Compatibility rule: same `major`. Minor diffs are additive — a `1.3` peer can talk to `1.0` as long as neither uses a feature the other doesn't know. The compat check is against `this`, not `CURRENT`, so tests and forks can hold their own local version.

### `SyncPayload`

Sealed interface with four record variants:

| Payload         | Direction    | Purpose                                                          |
|-----------------|--------------|------------------------------------------------------------------|
| `Handshake`     | Both         | `ProtocolVersion` + set of known config ids.                     |
| `Snapshot`      | Server → client | Full authoritative state for one config.                      |
| `Delta`         | Server → client | Partial state — only changed paths.                           |
| `ClientEdit`    | Client → server | Client-initiated edit forwarded for authorization.            |

### `SyncService`

The `SYNC`-capable provider implements `SyncService` ([source](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api-sync/src/main/java/com/oliveryasuna/mc/coal/api/sync/SyncService.java)) and returns it from the provider's SPI wiring. Key methods:

**Registration**

- `register(ConfigManager<?>)` — mark this manager as syncable.
- `unregister(ConfigManager<?>)`.

**Server-side**

- `onClientConnected(Object clientHandle)` — fires when a client joins. Provider sends `Handshake` + `Snapshot`.
- `broadcastDelta(configId, changedPaths)` — after a server-side `set(...)` on a synced path.
- `broadcastSnapshot(configId)` — full re-push (e.g. after `reload()`).

**Client-side**

- `sendClientEdit(configId, entries)` — forward a local edit. The server applies its own `InboundValidator` before broadcasting a `Delta` back.

**Lifecycle**

- `start()` — attach to the loader's network layer.
- `close()` — detach on shutdown.

### `NetworkTransport`

Loader-agnostic byte transport. Provider ships loader-specific implementations (Fabric networking API, NG's `PacketDistributor`) behind this abstraction. Methods:

```java
void sendToServer(byte[] payload);
void sendToClient(Object clientHandle, byte[] payload);
void sentToAllClients(byte[] payload);
void subscribe(InboundHandler handler);
```

The `Object clientHandle` is loader-defined — `ServerPlayer` on Fabric/NG, an opaque token in tests.

### `PayloadCodec`, `ScopeEnforcer`, `InboundValidator`

- **`PayloadCodec`** — `byte[] ↔ SyncPayload`. Providers can pick their wire format (protobuf, JSON, nbt) as long as encode/decode round-trip.
- **`ScopeEnforcer`** — filters what to send + apply:
  - `extractAuthoritative(manager)` — pull just the `SERVER`/`COMMON` values to broadcast.
  - `applyAuthoritative(manager, values)` — apply an inbound payload; return the paths that changed so the provider can dispatch `ChangeEvent`s with `Origin.FROM_REMOTE`.
- **`InboundValidator`** — server-side authorization for `ClientEdit`. Return `false` to reject the edit before it broadcasts.

## Flows

### Server → client (join)

```
Client joins
  ↓
Server: SyncService.onClientConnected(handle)
  ↓
Server: send Handshake { protocol: CURRENT, knownConfigIds }
  ↓
Client: verify protocol.isCompatibleWith(local)
  ↓
Server: for each shared config id:
        send Snapshot { configId, extractAuthoritative(manager) }
  ↓
Client: for each Snapshot:
        applyAuthoritative(manager, values)
        markOrigins(changedPaths, FROM_REMOTE)
        dispatch ChangeEvents
```

### Server → client (delta on server-side change)

```
Server: set("gameplay.difficulty", HARD)  →  broadcastDelta("mymod", ["gameplay.difficulty"])
  ↓
Server: send Delta { configId, changed: { "gameplay.difficulty": "HARD" } }
  ↓
Client: applyAuthoritative → dispatch ChangeEvent with FROM_REMOTE origin
```

### Client → server (client edit)

```
Client: sendClientEdit("mymod", { "gameplay.difficulty": "HARD" })
  ↓
Server: InboundValidator.accept(configId, values)
        false → drop
        true  → applyAuthoritative → broadcastDelta to all clients
```

## What annotations do without `SYNC`

If the installed provider returns `false` for `Capability.SYNC`:

- `@Sync(SERVER)` and `@Sync(COMMON)` are read into `EntryMetadata` but the runtime effect is **the same as `@Sync(CLIENT)`** — the value is client-local and never transmitted.
- Consumer mods that assumed server-authority get client-side defaults instead. Check `Coal.getProvider().supports(Capability.SYNC)` before relying on server enforcement.

## Related

- [Capabilities](./capabilities) — how to check for `SYNC` support.
- [Lifecycle and events](./lifecycle-and-events) — where `Origin.FROM_REMOTE` shows up.
- [Specification §16](../spec/#16-optional-module--synchronization-coal-api-sync) — normative rules.
