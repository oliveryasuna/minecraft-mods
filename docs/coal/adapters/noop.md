---
title: Noop provider
---

# Noop provider

The last-resort `ConfigProvider` bundled with `coal.jar` (both Fabric and NeoForge variants). Analogous to `slf4j-nop`.

Source: [`libraries/coal/noop/`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/noop). Normative rules in [spec §11.5](../spec/#115-the-coal-noop-provider).

## Identity

- `name()` returns `"coal-noop"` (the `Coal.NOOP_PROVIDER_NAME` constant).
- `priority()` returns `0` — the lowest legal value. Any other provider outranks it.
- Discovered via the same `ServiceLoader<ConfigProviderFactory>` mechanism as every other provider — it's not a special case in the runtime.

## What it does at every SPI call

**Deep noop**. Every returned collaborator is itself a no-op. Mods can freely chain `handle.manager().events().subscribe(...)` without crashing.

| Call                              | Behavior                                                                          |
|-----------------------------------|-----------------------------------------------------------------------------------|
| `register(Class<S>, ...)`         | Returns a handle whose `get()` returns a fresh instance populated with the POJO's default field values. |
| `register(ConfigSpec, ...)`       | Returns a handle whose `get()` returns a map populated with the spec's declared defaults. |
| `handle.set(...)`                 | Silently discarded.                                                               |
| `handle.save()`                   | No-op. No file created.                                                           |
| `handle.reload()`                 | No-op. State stays at defaults.                                                   |
| `handle.snapshot()`               | Returns a snapshot of the defaults.                                               |
| `manager.events().subscribe(...)` | Returns a `Registration` whose `close()` is a no-op. Listeners never fire.        |
| `manager.addReloadListener(...)`  | No-op — reload listeners never fire.                                              |
| `supports(Capability)`            | Returns `false` for every capability.                                             |

Consumer mods depending on `coal-api` behave normally at mod-init — they get a handle, they can read defaults, event subscription doesn't NPE. What they lose is **persistence** and **anything that would require a real provider**.

## When it's the selected provider

Two paths to noop being installed:

1. **No adapter mod on the classpath.** `coal.jar` is installed but no first-party or third-party adapter jar is present. Noop is the only factory, so noop wins.
2. **An adapter mod failed to load.** Something went wrong in the adapter's own bootstrap (mod-init error, missing dep), so its factory wasn't registered with `ServiceLoader`, and noop is the only remaining candidate.

## User-facing signal

The client-side portion of `coal-fabric` / `coal-neoforge` detects `Coal.isNoopProvider()` and surfaces two nudges:

- **Toast** — shown once when the player first reaches the title screen this session. Catches players before they start playing.
- **Chat message** — sent on every world/server join. Catches players who dive straight into a save.

Both messages tell the user that COAL has no real backend installed and their config settings won't persist.

## Detecting noop from consumer code

```java
if (Coal.isNoopProvider()) {
    // Warn the user, gate features that need persistence, or refuse to boot.
}
```

`Coal.providerName()` returns `"coal-noop"`; `Coal.isNoopProvider()` is the direct check.

## Why it exists

**Mods depending on `coal-api` always initialize.** Without noop, a consumer mod would need to guard every `Coal.register(...)` call in a try-catch or check `isBootstrapped()` first. With noop, the SPI call succeeds, the handle is returned, and the mod can continue setup — the *behavior* is degraded but the *code path* is intact.

Same rationale as SLF4J: `slf4j-nop` is what makes it safe for libraries to unconditionally call `LoggerFactory.getLogger(...)` in static initializers.

## Testkit note

`coal-noop` does **not** satisfy the baseline `AbstractCoalConformanceTest`. Baseline assumes a provider that persists state. Noop is documented in the spec as a normative reference for the *degraded* semantics — it's compliant with those, not with baseline. See [Conformance-test a provider](../guides/conformance-testing).

## Related

- [Provider model](../concepts/provider-model) — how noop gets selected when it's the only candidate.
- [Capabilities](../concepts/capabilities) — noop returns `false` for every capability.
- [Specification §11.5](../spec/#115-the-coal-noop-provider) — normative rules.
