---
title: YACL adapter
---

# YACL adapter

First-party COAL provider backed by [YetAnotherConfigLib (YACL)](https://github.com/isXander/YetAnotherConfigLib). Provides annotation-driven schema reading, JSON persistence, load-time validation with correction, and a YACL-rendered settings screen for any mod that consumes `coal-api`.

## Ships as

- **`coal-yacl-adapter-fabric`** — Fabric loader variant.
- **`coal-yacl-adapter-neoforge`** — NeoForge loader variant.

The MC-free adapter core (schema reader, JSON I/O, `ConfigProvider`, validators, event bus, config manager) lives in [`coal-adapter-common`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/adapter-common) and is shared with the Cloth adapter.

## Capabilities advertised

- `MIGRATION`
- `VALIDATION`
- `GUI_DELEGATION`

Not advertised: `SYNC`, `FILE_WATCH`, `JSON5`, `CUSTOM_FORMATS`. See the per-loader README limitations sections for the specific gaps.

## Priorities

- **`ConfigProviderFactory#priority()`** — `100`.
- **`ScreenProvider#priority()`** — `50`.

If both YACL and Cloth adapters are installed, the factory tie at `100` resolves non-deterministically. Install exactly one for predictable behavior.

## Persistence

`<config-dir>/<config-name>.json` via gson (2-space pretty-print, null-serializing, HTML escaping off). Format substitutions (TOML / JSON5 / custom) trigger a one-shot `WARN` at registration and fall back to JSON.

## Feature coverage matrix

Per-loader READMEs enumerate exactly which COAL annotations map to which YACL widgets, and what falls back to placeholders (e.g., `Map<K, V>`, deep-nested categories past two levels — YACL's `OptionGroup` doesn't recurse).

- [Fabric README](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/yacl-adapter-fabric) — full coverage table + limitations.
- [NeoForge README](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/yacl-adapter-neoforge) — same content, NG-flavored.

## Installation (users)

Drop into the `mods/` folder:

- `coal.jar` (matching loader).
- `coal-yacl-adapter-<loader>.jar`.
- YACL itself (`yet_another_config_lib_v3` on Fabric; equivalent on NeoForge).

## Installation (consumer mods)

Compile against `coal-api` + `coal-api-gui-<loader>`. The adapter is a **runtime mod**, not a compile-time dep — see [Getting started](../getting-started#1-add-the-dependencies).

## Testmod

Both loader modules ship a testmod that exercises every rendering path:

```
./gradlew :libraries:coal:coal-yacl-adapter-fabric:runTestmodClient
./gradlew :libraries:coal:coal-yacl-adapter-neoforge:runTestmodClient
```

Press `K` in-game to open the settings screen.

## Source

- [`libraries/coal/yacl-adapter-fabric/`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/yacl-adapter-fabric)
- [`libraries/coal/yacl-adapter-neoforge/`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/yacl-adapter-neoforge)
- [`libraries/coal/adapter-common/`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/adapter-common) — shared core.
