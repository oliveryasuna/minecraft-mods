---
title: Capabilities
---

# Capabilities

A provider tells consumers what it does — and doesn't — via `ConfigProvider#supports(Capability)`. This lets consumer mods gracefully degrade when a feature isn't available on the user's installed provider, without hard-failing at mod-init.

## The enum

Full definition in [`com.oliveryasuna.mc.coal.api.spi.Capability`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api/src/main/java/com/oliveryasuna/mc/coal/api/spi/Capability.java). Normative semantics in [spec §11.3](../spec/#113-capability-enum).

| Capability        | Advertised means                                                                                       |
|-------------------|--------------------------------------------------------------------------------------------------------|
| `MIGRATION`       | Executes `MigrationSpec` steps on load. Provider without this runs migrations no-op-style.             |
| `VALIDATION`      | Runs the built-in `Corrector` on load — `@Range` / `@Pattern` / `@OneOf` / `@Length` / `@NotNull` corrections surface in `LoadResult.corrections`. |
| `GUI_DELEGATION`  | Selects a `ScreenProvider` via `GuiRegistry` (from `coal-api-gui-<loader>`) and renders settings screens. |
| `SYNC`            | Implements the `coal-api-sync` SPI — server↔client config sync per `@Sync(scope)`.                     |
| `FILE_WATCH`      | Reloads when the file changes on disk (via `ConfigIO#fileWatchService()`).                             |
| `JSON5`           | Handles `Format.JSON5`. Absent providers falling back to JSON with a `WARN`.                           |
| `CUSTOM_FORMATS`  | Honors `Format` instances outside `toml`/`json`/`json5` — anything from `Format.of("mycustom")`.       |

## What "advertised" means

`supports(Capability.X)` returning `true` is a **normative commitment** by the provider. The spec-conformant testkit checks that a provider claiming `SYNC` implements the full `coal-api-sync` SPI, that `VALIDATION` produces the expected `LoadResult.corrections` for known-bad values, and so on ([spec §21.2](../spec/#212-capability-conditional-requirements)).

Providers should return `false` for capabilities they don't fully implement. There's no partial credit — degrade gracefully via `false` rather than half-shipping a feature.

## When consumer mods should check

Most consumer code doesn't need to check. If you're not calling `coal-api-sync` yourself, you don't care if `SYNC` is advertised — the annotations are read either way, they just don't do anything without a supporting provider. But there are a few real cases:

**Feature-gating logic that depends on server-authoritative values.**

```java
if (Coal.getProvider().supports(Capability.SYNC)) {
    // Trust @Sync(SERVER) values to be pushed from the server.
    // If not supported, treat them as client-local.
}
```

**Warning users when hot-reload is expected but not supported.**

```java
if (!Coal.getProvider().supports(Capability.FILE_WATCH)) {
    LOGGER.warn("File watching not supported — hand-edits won't apply until reload.");
}
```

**Choosing between hard reload and refresh.**

```java
if (Coal.getProvider().supports(Capability.VALIDATION)) {
    // On reload, corrections surface in LoadResult and I can show them to the user.
} else {
    // Values are trusted verbatim; no correction path.
}
```

## First-party adapter coverage

Neither the YACL nor the Cloth adapter advertises the whole set. Both currently advertise:

- **`MIGRATION`** — migration steps run on load.
- **`VALIDATION`** — annotation-based validators run on load, corrections in `LoadResult.corrections`.
- **`GUI_DELEGATION`** — a `ScreenProvider` registers itself with `GuiRegistry` at client-init.

Neither currently advertises:

- **`SYNC`** — no bundled `coal-api-sync` implementation in v1.
- **`FILE_WATCH`** — the shared `JsonConfigIO#fileWatchService()` returns `Optional.empty()`.
- **`JSON5`** — gson doesn't tolerate `//` comments.
- **`CUSTOM_FORMATS`** — hard-coded to JSON. TOML / JSON5 requests fall back to JSON with a `WARN` at registration.

Detail per-adapter is in the [YACL adapter](../adapters/yacl) and [Cloth adapter](../adapters/cloth) reference pages.

## Where the capability actually gets read

The provider is the source of truth. `Coal.getProvider().supports(Capability.X)` walks:

1. The `Coal` singleton (set up during bootstrap).
2. The installed `ConfigProvider` implementation.
3. Its `supports(Capability)` method — a hand-written switch/enum-map per provider.

There's no reflection, no service discovery — just a boolean method that the provider owner controls. If a provider silently drops a feature they used to advertise, that's a bug in the provider, not in COAL.

## Related

- [Provider model](./provider-model) — how the provider gets picked.
- [Server ↔ client sync](./sync) — the `SYNC` capability up close.
- [Conformance testing](../guides/conformance-testing) — what the testkit checks per-capability.
- [Specification §11.3](../spec/#113-capability-enum) — normative rules.
