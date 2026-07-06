---
title: COAL
---

# COAL

**Config Options Abstraction Layer** — an SLF4J-style abstraction for Minecraft mod configuration.

Consumer mods depend on `coal-api`. Providers (backed by YACL, Cloth Config, or your own choice) implement the SPI. Users install whichever provider they prefer; consumer mods don't know or care.

## Start here

- [Introduction](./introduction) — what COAL is and when to reach for it.
- [Getting started](./getting-started) — a ten-minute walkthrough: annotate a POJO, register it, open the settings screen.

## Learn the model

- [Provider model](./concepts/provider-model)
- [Capabilities](./concepts/capabilities)
- [GUI delegation](./concepts/gui-delegation)
- [Lifecycle and events](./concepts/lifecycle-and-events)
- [Server ↔ client sync](./concepts/sync)

## Do the thing

- [Define a config](./guides/defining-a-config)
- [Handle load-time corrections](./guides/handling-load-corrections)
- [Migrate configs across versions](./guides/migrating-configs)
- [Write a provider](./guides/writing-a-provider)
- [Conformance-test a provider](./guides/conformance-testing)

## Look it up

- [Annotation reference](./reference/annotations)
- [SPI reference](./reference/spi)
- [Specification](./spec/) (normative)

## Adapters

- [YACL](./adapters/yacl) — first-party YACL-backed provider.
- [Cloth Config](./adapters/cloth) — first-party Cloth-backed provider.
- [Noop](./adapters/noop) — fallback provider bundled with `coal.jar`.
