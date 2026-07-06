---
title: Lifecycle and events
---

# Lifecycle and events

Load, save, reload, and the `EventBus` that surfaces changes.

## The load cycle

`ConfigManager.load()` returns a `LoadResult` — a record with three parts:

```java
public record LoadResult(
        ConfigSnapshot snapshot,
        List<Correction> corrections,
        Optional<MigrationReport> migration
) {}
```

- **`snapshot`** — the values that ended up in state. Includes anything corrected or migrated. This is what `handle.get()` will return until the next mutation or reload.
- **`corrections`** — one entry per value the corrector changed, with `path`, `before`, `after`, `reason`. Empty when everything validated clean. See [Handle load corrections](../guides/handling-load-corrections) for the full walk-through.
- **`migration`** — `Optional.empty()` when the on-disk version matches `@Config#version`; present when steps ran, listing each `AppliedStep` (from, to, ops applied).

Load is a bulk operation. The provider reads the file, runs migrations if the on-disk version is behind, applies corrections, materializes the POJO, and commits state. Callers see all-or-nothing — an exception propagates as `IOException`; state is unchanged.

## Save

`ConfigManager.save()` writes the current in-memory state to disk. Explicit; not auto-triggered by `set(...)`.

The first-party adapters save from **inside a lock** that guards the state POJO and the origins map:

1. Snapshot the nested-tree representation.
2. Release the lock.
3. Serialize + write to disk.

So concurrent `set(...)` after `save()` starts writing doesn't block the disk write, and vice versa — the write captures a consistent tree at its start, subsequent mutations land in the next save.

Providers throw `IOException` on write failure; the caller decides what to do. The in-memory state is not rolled back.

## Reload

Two ways state gets re-read:

**Explicit `reload()`** — the caller wants to re-read disk (e.g., user chose "Reload from disk" in a settings screen). Calls `load()` under the hood, then dispatches a `ReloadListener` callback with `(previous, current)` states so consumers can diff.

**File-watch reload** — only when the provider advertises [`Capability.FILE_WATCH`](./capabilities). The provider's `FileWatchService` fires on external edits (hand-editing the JSON), and the same reload path runs.

Providers without `FILE_WATCH` never fire the file-watch path — hand-edits sit on disk until a future explicit `reload()` or restart.

## Reload tiers — `@Reload`

The [`@Reload`](../reference/annotations#reload) annotation tells providers **when** a changed value should take effect. Three tiers:

| Tier      | When applied                         | Typical example                      |
|-----------|--------------------------------------|--------------------------------------|
| `LIVE`    | Immediately on change                | Client cosmetic (particle color).    |
| `WORLD`   | On next world (re)load. **Default.** | Server-visible game rule.            |
| `RESTART` | Only after a full game restart       | JVM-affecting option, mod loading.   |

`@RequiresRestart` is a convenience marker equivalent to `@Reload(Reload.Tier.RESTART)`. If both are present on the same element, `@Reload` wins.

The tier is **metadata** — it doesn't gate `set()` from succeeding, but consumer code can read `SchemaEntry.metadata().reloadTier()` and refuse to apply the value until the tier's condition is met. Providers may also visualize the tier in the GUI (e.g., YACL's "requires restart" indicator on `RESTART`-tier entries).

## The `EventBus`

Every `ConfigManager` exposes an [`EventBus`](../reference/spi#eventbus) for **change notifications**. Get it via `manager.events()`.

### `ChangeEvent`

Dispatched when a value at a path changes.

```java
public record ChangeEvent(
        String path,        // dotted, e.g. "graphics.brightness"
        Object oldValue,
        Object newValue,
        Origin origin,      // DEFAULT | LOCAL_EDIT | FROM_REMOTE
        Instant at
) {}
```

### Subscribing

```java
manager.events().subscribe(event -> {
    LOGGER.info("Config changed: {} = {}", event.path(), event.newValue());
});
```

Subscribe with a **path prefix** to only receive events for one sub-tree:

```java
manager.events().subscribe("graphics.", event -> {
    // Only fires for paths starting with "graphics.".
});
```

Both `subscribe` variants return an `EventBus.Registration` — an `AutoCloseable` you can call `close()` on to unsubscribe. Use try-with-resources or hold the reference.

### Reload listeners

For structural reload notifications (previous and current whole-config), add a `ReloadListener<S>`:

```java
handle.manager().addReloadListener((previous, current) -> {
    if (previous.enabled != current.enabled) {
        LOGGER.info("Enabled state flipped: {} → {}", previous.enabled, current.enabled);
    }
});
```

Called on `reload()` and on file-watch reload. Not called on individual `set(...)` — those fire `ChangeEvent` instead.

## Origin tracking

Each entry has an `Origin` — how it got its current value:

| Origin        | Set when                                                                  |
|---------------|---------------------------------------------------------------------------|
| `DEFAULT`     | Never touched — still the schema default.                                 |
| `LOCAL_EDIT`  | Set locally (via `set(...)`) or read from the persistent file.            |
| `FROM_REMOTE` | Pushed by a server sync (only relevant when `SYNC` is advertised).        |

Query per path via `manager.originOf("graphics.brightness")`.

Providers bulk-update origins via `markOrigins(paths, origin)` — typically after a sync push, to flip a bunch of paths to `FROM_REMOTE` in one go.

## Threading

Reads (`handle.get()`, `manager.get()`, `snapshot()`, `originOf(...)`) are lock-free and safe from any thread.

Writes (`set(...)`, `load()`, `save()`) and event dispatch are serialized by the provider. The first-party adapters use an internal monitor; concurrent `set(...)` calls interleave safely, but each mutation runs to completion before the next starts.

Event dispatch happens **after** the lock is released — a listener that itself calls `set(...)` won't deadlock, but reordering can happen if multiple mutations race.

Spec §18 has the full threading contract if you're writing a provider.

## Related

- [Handle load corrections](../guides/handling-load-corrections) — the `Correction` and `LoadResult` cycle up close.
- [Migrate configs](../guides/migrating-configs) — the migration side of `LoadResult`.
- [Server ↔ client sync](./sync) — where `FROM_REMOTE` origin comes from.
- [Specification §10, §18](../spec/#10-user-api--events-and-reload) — normative rules.
