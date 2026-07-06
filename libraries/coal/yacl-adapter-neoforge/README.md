# coal-yacl-adapter (NeoForge)

A [COAL](../../../docs/coal/index.md) provider adapter backed by [YetAnotherConfigLib (YACL)](https://github.com/isXander/YetAnotherConfigLib). Provides annotation-driven config schema
reading, JSON persistence, load-time validation with correction, and a YACL-rendered settings screen for any mod that consumes `coal-api`.

**Loader**: NeoForge (MC 1.21.8, NeoForge 21.8.53).

The MC-free adapter core — schema reader, JSON I/O, `AdapterConfigProvider`, validators, event bus, config manager, `AdapterScreenSupport` helpers — lives in
[`../adapter-common/`](../adapter-common) and is shared with the Cloth adapter. This module only contains the YACL / MC-typed `YaclScreenProvider`, the loader-specific
`YaclConfigProviderFactory`, and the NG mod entry classes; the sibling [`../yacl-adapter-fabric/`](../yacl-adapter-fabric) module ships the same behavior on Fabric, and
[`../cloth-adapter-neoforge/`](../cloth-adapter-neoforge) is the Cloth-backed sibling on NG.

## What it does

- Implements the COAL `ConfigProvider` SPI (`coal-api` §11).
- Advertises capabilities: **`MIGRATION`**, **`VALIDATION`**, **`GUI_DELEGATION`**.
- Reads consumer POJOs annotated with `@Config` and the COAL constraint / metadata annotations. Reflects them into a `Schema` with a `SchemaCategory` tree.
- Persists to `<config-dir>/<config-name>.json` via gson (2-space pretty-print, null-serializing, HTML escaping off).
- Registers a `ScreenProvider` at client-init with the COAL `GuiRegistry` at priority `50`, so any GUI opened via `GuiRegistry.open(...)` renders through YACL.
- Runs the built-in `Corrector` on every load: values violating `@Range`, `@Pattern`, `@OneOf`, `@Length`, or `@NotNull` get replaced with the validator's suggestion or the field's
  declared default. Corrections appear in `LoadResult.corrections`.
- Materializes the config file on first launch if it doesn't exist (writes fresh defaults so users have something to hand-edit).

## Requirements

Runtime deps (declared in the mod's `neoforge.mods.toml`):

- `coal` (the COAL mod, NeoForge variant) — ships the `Platform`, `coal-api` + `coal-api-gui-neoforge` + `coal-noop` bundled.
- `yet_another_config_lib_v3` — YACL itself. Hard dep on client; the screen provider is unusable without it.
- NeoForge `21.8.53+`, Minecraft `1.21.8`, Java 21.

The published jar `jarJar`s `gson` and `oliveryasuna-commons-language`; consumers don't need to install those separately.

## Installation

For end users: install `coal.jar` (NeoForge variant), `coal-yacl-adapter-neoforge.jar`, and YACL side-by-side in the mods folder. Consumer mods that call `Coal.register(...)`
automatically pick up this provider (priority `100` beats bundled `coal-noop`'s `0`).

For consumer mods (Gradle):

```kotlin
dependencies {
    implementation("com.oliveryasuna.mc:coal-api:<version>")
    implementation("com.oliveryasuna.mc:coal-api-gui-neoforge:<version>")
    // The adapter itself is installed at runtime as a mod, not compile-time.
}
```

## Feature coverage

Identical to the Fabric variant — the persistence + schema layer all lives in `coal-adapter-common` (shared with the Cloth adapter), and this module supplies the
YACL-specific screen rendering.

| COAL construct                                                              | YACL rendering                                                                   |
|-----------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `@Config(id, name, format, version)`                                        | Screen title = `name`                                                            |
| `@Category("name")` on field                                                | Places entry under a `ConfigCategory` tab                                        |
| `@Category("name")` on type                                                 | Prefixes every entry's category with `name`                                      |
| `@Comment({...})` on field                                                  | `OptionDescription.text(...)` tooltip                                            |
| `@Comment({...})` on type                                                   | Tooltip on the root category                                                     |
| `@Hidden`                                                                   | Entry omitted from screen (still persisted)                                      |
| `@Key("override")`                                                          | Uses the override as the on-disk key                                             |
| `@Range(min, max)` on numeric                                               | Slider (`Integer` / `Long` / `Double` / `Float`) — step `1` / `1L` / `0.01`      |
| `@OneOf({...})` on `String`                                                 | `DropdownStringController`, `allowAnyValue(false)`                               |
| `@Pattern("regex")` on `String`                                             | Load-time correction; no in-GUI enforcement                                      |
| `@Length(min, max)` on `String` / `Map`                                     | Load-time correction                                                             |
| `@Length(min, max)` on `List<T>`                                            | `ListOption.minimumNumberOfEntries` + `maximumNumberOfEntries` (GUI-enforced)    |
| `@NotNull`                                                                  | Load-time correction                                                             |
| `@Sync(scope)`                                                              | Metadata read; no runtime effect (SYNC capability not advertised)                |
| `@Reload(RESTART)` / `@RequiresRestart`                                     | `OptionFlag.GAME_RESTART` — YACL "requires restart" indicator                    |
| `@Reload(WORLD)` / `@Reload(LIVE)`                                          | Read but not mapped (no clean YACL equivalent — see [Limitations](#limitations)) |
| `@Widget(COLOR)` on `#RRGGBB` `String`                                      | `ColorControllerBuilder`, alpha disabled                                         |
| `@Widget(...)` other                                                        | Falls back per spec §7.14 — see [Limitations](#limitations)                      |
| Boolean scalar                                                              | `BooleanControllerBuilder` with yes/no formatter, coloured                       |
| Enum scalar                                                                 | `EnumControllerBuilder`                                                          |
| Non-annotated numeric scalar                                                | Number field (`Integer` / `Long` / `Double` / `Float`)                           |
| Non-annotated `String`                                                      | Plain text field                                                                 |
| `List<T>` where `T ∈ {String, Boolean, Integer, Long, Double, Float, Enum}` | `ListOption<T>` — add / remove / reorder entries                                 |
| Non-annotated nested POJO (raw `public Foo foo`)                            | Schema-layer flattened as a `foo.*` sub-category with each field as an entry     |
| `Map<K, V>`                                                                 | Placeholder (see [Limitations](#limitations))                                    |
| `List<complex-element>` (nested lists, maps, POJOs)                         | Placeholder                                                                      |

## Format substitution

The adapter only ships a JSON codec in v1. A consumer requesting `Format.TOML` or `Format.JSON5` via `@Config(format = ...)` triggers a one-shot `WARN` at registration and falls
back to writing `<name>.json` — no data loss, but the requested format is not honored. See [Limitations](#limitations).

## Testmod

A demo mod under `src/testmod/` exercises every rendering path. Boot with:

```
./gradlew :libraries:coal:coal-yacl-adapter-neoforge:runTestmodClient
```

Press `K` in-game to open the settings screen. See [`TestmodConfig`](src/testmod/java/com/oliveryasuna/mc/coal/yacl/testmod/TestmodConfig.java) for the full annotated example.

## Thread safety

- `AdapterConfigManager` (in `coal-adapter-common`) guards `state`, `origins`, and every read-modify-write flow with an internal monitor. `EventBus.dispatch` runs after the lock
  is released; disk I/O in `save()` happens after the lock captures a fresh nested tree.
- `AdapterConfigProvider#install` uses `putIfAbsent` for atomic registration — concurrent `register(id)` calls with the same id are safe; exactly one wins.
- Reads (`get()`, `rawAt(...)`) are lock-free via the volatile `state` reference; a concurrent `set()` may produce field-level torn reads on the state POJO but not a broken
  reference.

## Limitations

Feature-set gaps relative to the COAL spec + YACL's own capability set. Everything here also applies to the Fabric variant — the underlying code is shared.

### Capabilities not advertised

The provider's `supports(Capability)` returns `false` for:

- **`SYNC`** — YACL has no networking primitives; a full `SyncService` implementation would need to layer `coal-api-sync` separately.
- **`FILE_WATCH`** — `JsonConfigIO#fileWatchService()` returns `Optional.empty()`. No hot-reload on external file changes.
- **`JSON5`** — gson doesn't tolerate `//` comments; would need a JSON5 codec (Jankson, etc.).
- **`CUSTOM_FORMATS`** — hard-coded to `Format.JSON`. TOML / JSON5 requests fall back to JSON with a `WARN`.

### YACL constraints (can't route around)

- **`Map<K, V>`** renders as a disabled placeholder (`(edit on disk)`). YACL has no map primitive; encoding maps as key-value string lists would be lossy and not type-safe.
- **`@Reload(WORLD)`** and **`@Reload(LIVE)`** are read but not mapped to any YACL flag. YACL's `RELOAD_CHUNKS` / `WORLD_RENDER_UPDATE` / `ASSET_RELOAD` trigger immediate
  side-effects that don't match the "next world load" or "applied immediately" spec semantics. Only `@Reload(RESTART)` / `@RequiresRestart` maps cleanly to
  `OptionFlag.GAME_RESTART`.
- **Deep-nested categories** (three or more `@Category` levels deep) flatten into a single collapsible `OptionGroup` per branch on the second level. YACL's `OptionGroup` doesn't
  recurse — this is a YACL API limitation, not an adapter bug.

### Not-yet-implemented (v2 candidates)

- **`Widget.allowInvalid()`** — read but ignored. Would require hooking YACL's per-controller commit-block via `Option.Builder.addListener(...)`.
- **`Corrector` on `set(...)`** — validators run only on `load()`; `set(path, badValue)` at runtime is accepted without validation. `save()` writes whatever's in state.
- **`BackupStrategy`** — `JsonConfigIO#backupStrategy()` returns `Optional.empty()`. Overwrites in place.
- **`@Pattern` in-GUI validation** — the validator runs at correction time only; the text field accepts anything and the value gets corrected on next load.
- **Element-typed validators inside `ListOption`** — element controllers respect basic YACL type validation (Integer field rejects non-numeric text), but per-element `@Range` /
  `@Pattern` isn't threaded through.
- **Codec-mediated leaves** (`UUID`, `Instant`, `Duration`, `ResourceLocation`, ...) fall through to the placeholder path. Rubric solves this with a `Codec<T>` bridge; not in v1.
- **Prism / packaged-jar canary** — dev `runTestmodClient` is proven, but the published jar hasn't been dropped into a real Prism install for smoke-testing.
- **`coal-testkit` conformance run** — no `src/test` extending `AbstractCoalConformanceTest` yet; the adapter isn't self-checked against the spec.

## License

MIT. See [../LICENSE](../LICENSE) at the COAL family root.
