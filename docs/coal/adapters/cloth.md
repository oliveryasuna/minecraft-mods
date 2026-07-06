---
title: Cloth Config adapter
---

# Cloth Config adapter

First-party COAL provider backed by [Cloth Config](https://github.com/shedaniel/cloth-config). Provides annotation-driven schema reading, JSON persistence, load-time validation with correction, and a Cloth-rendered settings screen for any mod that consumes `coal-api`.

## Ships as

- **`coal-cloth-adapter-fabric`** — Fabric loader variant.
- **`coal-cloth-adapter-neoforge`** — NeoForge loader variant.

Shares the MC-free adapter core with the YACL adapter — [`coal-adapter-common`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/adapter-common) is the persistence layer for both. The Cloth adapters only add the Cloth-specific `ScreenProvider` and the loader entry classes.

## Capabilities advertised

- `MIGRATION`
- `VALIDATION`
- `GUI_DELEGATION`

Same set as the YACL adapter. Not advertised: `SYNC`, `FILE_WATCH`, `JSON5`, `CUSTOM_FORMATS`.

## Priorities

- **`ConfigProviderFactory#priority()`** — `100`.
- **`ScreenProvider#priority()`** — `40`.

Note the screen-provider priority is **`40`**, one below YACL's `50`. If both adapters register screen providers, YACL wins for the settings-screen render. The factory priority tie at `100` between YACL and Cloth is why installing exactly one is recommended.

## Distinguishing from YACL

Cloth's `startSubCategory` accepts a `List<AbstractConfigListEntry>` and **recurses cleanly**, so the Cloth adapter preserves the full category tree end-to-end — no depth cap. YACL's `OptionGroup` doesn't recurse, so the YACL adapter flattens third-and-deeper category levels.

Cloth ships list widgets (`startStrList`, `startIntList`, etc.) but the current adapter wires them as placeholders — v2 work.

## Persistence

Same as YACL adapter — `<config-dir>/<config-name>.json` via gson. See [YACL adapter](./yacl#persistence).

## Feature coverage matrix

Per-loader READMEs enumerate the annotation-to-Cloth-widget mapping:

- [Fabric README](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/cloth-adapter-fabric) — full coverage table + limitations.
- [NeoForge README](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/cloth-adapter-neoforge).

## Installation (users)

Drop into the `mods/` folder:

- `coal.jar` (matching loader).
- `coal-cloth-adapter-<loader>.jar`.
- Cloth Config itself (`cloth-config` on Fabric; equivalent on NeoForge).

## Installation (consumer mods)

Compile against `coal-api` + `coal-api-gui-<loader>`. See [Getting started](../getting-started#1-add-the-dependencies).

## Testmod

```
./gradlew :libraries:coal:coal-cloth-adapter-fabric:runTestmodClient
./gradlew :libraries:coal:coal-cloth-adapter-neoforge:runTestmodClient
```

Press `K` in-game.

## Source

- [`libraries/coal/cloth-adapter-fabric/`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/cloth-adapter-fabric)
- [`libraries/coal/cloth-adapter-neoforge/`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/cloth-adapter-neoforge)
- [`libraries/coal/adapter-common/`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/adapter-common) — shared core.
