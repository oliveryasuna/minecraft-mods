---
title: Provider model
---

# Provider model

How COAL discovers, selects, and installs a `ConfigProvider`. The normative rules live in [spec §3](../spec/#3-discovery-bootstrap-and-lifecycle) and [§11](../spec/#11-provider-spi--factory-and-provider); this page is the mental model.

## Two SPIs, one entry point

COAL bootstrap requires two things on the classpath:

- **A `Platform`** ([`com.oliveryasuna.mc.coal.api.platform.Platform`](../reference/spi#platform)) — one per JVM. Provides the config directory, main-thread executor, logger factory, environment, and loader identification. Loader integrations (`coal-fabric`, `coal-neoforge`) ship one via `META-INF/services/`.
- **One or more `ConfigProviderFactory`** ([`com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory`](../reference/spi#configproviderfactory)) — pluggable. Each reports `name()`, `priority()`, `coalVersion()`, and produces a `ConfigProvider` via `create(Platform)`. Discovered by `ServiceLoader`.

`Coal.bootstrap()` walks these two SPIs, picks a factory, and installs the resulting provider. Consumer mods rarely call `bootstrap()` explicitly — the first call to `Coal.register(...)` auto-triggers bootstrap.

## Discovery

```
ServiceLoader<Platform>              →   exactly one   →   Platform
ServiceLoader<ConfigProviderFactory> →   at least one  →   pick highest priority()
                                                       →   factory.create(platform)
                                                       →   installed ConfigProvider
```

### Platform

Zero or two-or-more `Platform` implementations throw `ProviderNotFoundException` at bootstrap. The message names each discovered class. Loader integrations must produce exactly one — `coal-fabric` ships `FabricPlatform`, `coal-neoforge` ships `NeoForgePlatform`.

The `Platform` instance is **process-stable**. Loader integrations may mutate its internal state as the game boots, but the object identity never changes.

### Factories

Zero factories is a classpath misconfiguration and throws `ProviderNotFoundException`. The `coal` mod JiJ-bundles `coal-noop`, so any real install always has at least one factory on the classpath.

## Selection

Given N discovered factories:

1. Sort by `priority()` descending.
2. Pick the head.
3. Log `INFO`: `"COAL provider 'coal-yacl-adapter' installed (priority 100)."`.
4. If N > 1, additionally log `WARN` naming every candidate — this surfaces classpath surprises loudly.
5. Ties in priority resolve by `ServiceLoader` iteration order. The spec doesn't further constrain the tie-break; providers should pick distinct, deliberate priorities.

### Priority conventions

- **`0`** — reserved for `coal-noop`. The last-resort fallback bundled with `coal.jar`.
- **`100`** — the default for first-party adapters (`coal-yacl-adapter`, `coal-cloth-adapter`).
- **Above `100`** — reserved for consumer-mod-provided overrides. If you're shipping a purpose-built provider that should outrank the first-party ones, use `200+`.

Multiple factories at priority `100` will trigger the `WARN` and resolve non-deterministically — install exactly one adapter for predictable behavior. This is why the YACL and Cloth adapter READMEs both tell users to pick one.

## The `coal-noop` last-resort

`coal-noop` is a **deep noop provider** bundled with the `coal` mod (both Fabric + NeoForge variants):

- `name()` returns `"coal-noop"`.
- `priority()` returns `0` — always outranked by anything else.
- `register(...)` returns a `ConfigHandle` where `get()` returns a fresh default POJO every time, `set(...)` is discarded, `save()` is a no-op, and every collaborator (`ConfigManager`, `EventBus`, `ConfigSnapshot`) is likewise a deep no-op.
- The GUI provider bundled with `coal-fabric` / `coal-neoforge` fires a **toast on first title-screen** and a **chat message on every world join** when it detects `Coal.isNoopProvider()`. Users know a real adapter isn't installed.

The purpose: **mods depending on `coal-api` always initialize.** The user might see defaults instead of persistence, but no NPE at mod-init from a missing provider.

Detect noop from consumer code:

```java
if (Coal.isNoopProvider()) {
    // Warn the user, gate features that need persistence, whatever.
}
```

## Bootstrap semantics

`Coal.bootstrap()` is **first-wins**. Concurrent calls serialize on an internal monitor; the first winner installs, subsequent calls no-op with a `WARN`. Loader integrations should call `bootstrap()` at mod-init so any classpath errors surface early — after a successful bootstrap, subsequent `register(...)` calls just route to the installed provider without re-scanning.

`Coal.bootstrap(ConfigProvider)` — the explicit override — is unconditional. It replaces any previously installed provider and updates the bookkeeping so `Coal.providerName()` / `Coal.isNoopProvider()` stay consistent. **This is for tests and deliberate mid-run overrides**; production consumer mods should not call it.

## Writing your own provider

The provider SPI is the whole point of COAL being pluggable. Anyone can write a factory + provider and ship it as a mod. See [Write a provider](../guides/writing-a-provider) for the walk-through. The [`coal-adapter-common`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/adapter-common) module is a working reference — it's shared between the YACL and Cloth adapters and implements every part of the SPI on top of gson + reflection.

## Related

- [Capabilities](./capabilities) — how a provider tells consumers what it does and doesn't support.
- [GUI delegation](./gui-delegation) — a separate SPI (`coal-api-gui-*`) with its own priority-based selection.
- [Specification §3](../spec/#3-discovery-bootstrap-and-lifecycle) — the normative rules.
