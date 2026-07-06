---
title: Introduction
---

# Introduction

COAL is **SLF4J for Minecraft mod configuration**.

If you've used SLF4J: your library depends on `slf4j-api`, and whoever assembles the runtime picks a backend (Logback, log4j, etc.). Your library never mentions Logback by name, and if the user installs a different backend, nothing in your code changes. COAL does the same thing for mod configs.

## The problem

Every Minecraft mod that needs configuration currently picks one of:

- **Ship its own config library**, coupling the mod to a specific loader, format, and GUI style. Users end up with five mods and five different config screens, five different file formats, five different reload semantics.
- **Depend on a third-party config library directly.** Now the mod pins that dependency's version, argues with its API surface, and can't work if a user prefers a different library.
- **Roll a bespoke JSON reader.** Works until you need validation, migration, a GUI, or sync — at which point you've written a config library from scratch.

## What COAL is

Three things, matching SLF4J's three pieces:

1. **`coal-api`** — the API and provider SPI. Annotations (`@Config`, `@Range`, `@OneOf`, ...), an entry point (`Coal.register(MyConfig.class)`), a config handle. This is what your mod depends on. It's MC-free — just an annotation-processing SPI you could unit-test without Minecraft on the classpath.
2. **Providers** — pluggable implementations. First-party providers ship for [YACL](./adapters/yacl) and [Cloth Config](./adapters/cloth); anyone can write their own by implementing the SPI. The user installs whichever they prefer — your mod doesn't know.
3. **`coal-noop`** — a last-resort provider bundled with the `coal` mod (Fabric + NeoForge variants). Accepts every call, logs, does nothing. It exists so a mod depending on `coal-api` **always** gets *some* `ConfigProvider` — the game boots, your mod initializes, values just don't persist and the GUI shows a "no backend installed" toast. Same role as `slf4j-nop`.

## What consumer mods get

- Annotate a POJO with `@Config`, hand it to `Coal.register(MyConfig.class)`, get a typed `ConfigHandle<MyConfig>` back.
- Load + save + validation + correction + migration + settings screen — all delegated to whichever provider the user installed.
- A stable API surface: providers can be swapped without recompiling your mod.

## What users get

- **One config UI for every COAL-consuming mod.** Install YACL and every COAL mod's settings screen renders in YACL. Prefer Cloth Config? Same thing, different install.
- **Consistent file locations and reload semantics** across every COAL-consuming mod — since the provider owns those decisions, not each mod.
- **No hard dependency on a specific config UI.** If nothing is installed, mods still work; they just save configs to disk without a GUI and log the fact.

## When to reach for COAL

- You're writing a new mod and want config + GUI without hand-rolling either.
- You're refactoring an existing mod's bespoke config off a library you no longer want to maintain.
- You want your mod's settings screen to match whatever other config-heavy mods on the user's install look like.

## When *not* to reach for COAL

- Your mod has no user-facing configuration. Depending on `coal-api` for nothing is overhead.
- You need a config format COAL providers don't support and don't want to write your own provider. The first-party providers ship JSON only in v1 — TOML / JSON5 falls back to JSON with a warning.
- You need capabilities (sync, hot-reload, file watching) that no installed provider advertises. Providers report their [capabilities](./concepts/capabilities) honestly; check `provider.supports(Capability.SYNC)` before assuming.

## Non-goals

- COAL is not a config *format*. It doesn't ship its own file syntax. Format choice is a provider concern.
- COAL is not a settings-screen widget library. GUI rendering is delegated wholesale to the installed [screen provider](./concepts/gui-delegation).
- COAL is not a permissions system. `Platform#permissionGate()` exists so providers can gate server-side edits, but the policy is external.

## Next

- [Getting started](./getting-started) — annotate a POJO and open a settings screen in ten minutes.
- [Provider model](./concepts/provider-model) — how factories are discovered and how priority resolves.
- [Specification](./spec/) — the normative document, for when you need the exact rule.
