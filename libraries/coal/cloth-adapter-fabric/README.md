# coal-cloth-adapter (Fabric)

A [COAL](../../../docs/coal/index.md) provider adapter backed by [Cloth Config](https://github.com/shedaniel/cloth-config). Provides annotation-driven config schema reading, JSON
persistence, load-time validation with correction, and a Cloth-rendered settings screen for any mod that consumes `coal-api`.

**Loader**: Fabric (MC 1.21.8). The MC-free adapter core lives in [`../adapter-common/`](../adapter-common) and is shared with the YACL adapter — this module only contains the
Cloth / MC-typed `ClothScreenProvider`, the loader-specific `ClothConfigProviderFactory`, and the Fabric mod entry classes. See
[`../cloth-adapter-neoforge/`](../cloth-adapter-neoforge) for the NeoForge variant of the same behavior, and [`../yacl-adapter-fabric/`](../yacl-adapter-fabric) for the sibling
YACL-backed adapter.

## What it does

- Implements the COAL `ConfigProvider` SPI (`coal-api` §11).
- Advertises capabilities: **`MIGRATION`**, **`VALIDATION`**, **`GUI_DELEGATION`**.
- Reads consumer POJOs annotated with `@Config` and the COAL constraint / metadata annotations. Reflects them into a `Schema` with a `SchemaCategory` tree.
- Persists to `<config-dir>/<config-name>.json` via gson (2-space pretty-print, null-serializing, HTML escaping off).
- Registers a `ScreenProvider` at client-init with the COAL `GuiRegistry` at priority `40`, so any GUI opened via `GuiRegistry.open(...)` renders through Cloth Config.
- Runs the built-in `Corrector` on every load: values violating `@Range`, `@Pattern`, `@OneOf`, `@Length`, or `@NotNull` get replaced with the validator's suggestion or the field's
  declared default. Corrections appear in `LoadResult.corrections`.
- Materializes the config file on first launch if it doesn't exist (writes fresh defaults so users have something to hand-edit).

## Requirements

Runtime deps (declared in the mod's `depends`):

- `coal` (the COAL mod, Fabric variant) — ships the `Platform`, `coal-api` + `coal-api-gui-fabric` + `coal-noop` bundled.
- `cloth-config` — Cloth Config itself. Hard dep; the adapter is unusable without it.
- `fabric-api`, `fabricloader >= 0.16.0`, Minecraft `1.21.8`, Java 21.

The published jar JiJ-bundles `gson`, `oliveryasuna-commons-language`, and `coal-adapter-common`; consumers don't need to install those separately.

## Installation

For end users: install `coal.jar`, `coal-cloth-adapter-fabric.jar`, and Cloth Config side-by-side in the mods folder. Consumer mods that call `Coal.register(...)` automatically
pick up this provider (priority `100` beats bundled `coal-noop`'s `0`).

Priority-vs-YACL: if both adapters are installed side-by-side, provider selection follows the `ConfigProviderFactory#priority()` order. Both COAL Cloth and COAL YACL currently
declare factory priority `100`, so ties resolve by `ServiceLoader` iteration order — install exactly one for deterministic selection. The **screen provider** priority is separate
(YACL 50, Cloth 40) and only matters when both are registered against the same `GuiRegistry`.

For consumer mods (Gradle):

```kotlin
dependencies {
    modImplementation("com.oliveryasuna.mc:coal-api:<version>")
    modImplementation("com.oliveryasuna.mc:coal-api-gui-fabric:<version>")
    // The adapter itself is installed at runtime as a mod, not compile-time.
}
```

## Feature coverage

Identical to the NeoForge variant — the persistence + schema layer all lives in `coal-adapter-common` (shared with the YACL adapter), and this module supplies the Cloth-specific
screen rendering.

| COAL construct                                                              | Cloth rendering                                                                    |
|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| `@Config(id, name, format, version)`                                        | Screen title = `name`                                                              |
| `@Category("name")` on field                                                | Places entry under a root category tab                                             |
| `@Category("name")` on type                                                 | Prefixes every entry's category with `name`                                        |
| `@Comment({...})` on field                                                  | `setTooltip(...)`                                                                  |
| `@Comment({...})` on type                                                   | Tooltip on the root category                                                       |
| `@Hidden`                                                                   | Entry omitted from screen (still persisted)                                        |
| `@Key("override")`                                                          | Uses the override as the on-disk key                                               |
| `@Range(min, max)` on numeric                                               | Slider (`startIntSlider` / `startLongSlider`) — bounded number field for float/double |
| `@OneOf({...})` on `String`                                                 | `startStringDropdownMenu`, values-only autocomplete                                |
| `@Pattern("regex")` on `String`                                             | Load-time correction; no in-GUI enforcement                                        |
| `@Length(min, max)` on `String` / `Map`                                     | Load-time correction                                                               |
| `@Length(min, max)` on `List<T>`                                            | Load-time correction (see [Limitations](#limitations))                             |
| `@NotNull`                                                                  | Load-time correction                                                               |
| `@Sync(scope)`                                                              | Metadata read; no runtime effect (SYNC capability not advertised)                  |
| `@Reload(RESTART)` / `@RequiresRestart`                                     | `requireRestart()` — Cloth "requires restart" indicator                            |
| `@Reload(WORLD)` / `@Reload(LIVE)`                                          | Read but not mapped (no clean Cloth equivalent — see [Limitations](#limitations))  |
| `@Widget(COLOR)` on `#RRGGBB` `String`                                      | `startColorField`, alpha disabled — packed-int RGB with hex parse/format helpers   |
| `@Widget(...)` other                                                        | Falls back per spec §7.14 — see [Limitations](#limitations)                        |
| Boolean scalar                                                              | `startBooleanToggle`                                                               |
| Enum scalar                                                                 | `startEnumSelector`                                                                |
| Non-annotated numeric scalar                                                | Number field (`startIntField` / `startLongField` / `startFloatField` / `startDoubleField`) |
| Non-annotated `String`                                                      | `startStrField` plain text field                                                   |
| Nested POJO / `@Category` sub-tree                                          | `startSubCategory` — recursive, full nesting preserved (no depth cap)              |
| `List<T>` / `Map<K, V>` / codec-mediated leaves                             | Placeholder (see [Limitations](#limitations))                                      |

Cloth's `startSubCategory` accepts a `List<AbstractConfigListEntry>` and recurses cleanly, so the schema tree is preserved end-to-end. Compare with the YACL adapter, which
flattens third-and-deeper category levels because `OptionGroup` doesn't recurse.

## Format substitution

The adapter only ships a JSON codec in v1. A consumer requesting `Format.TOML` or `Format.JSON5` via `@Config(format = ...)` triggers a one-shot `WARN` at registration and falls
back to writing `<name>.json` — no data loss, but the requested format is not honored. See [Limitations](#limitations).

## Testmod

A demo mod under `src/testmod/` exercises every rendering path. Boot with:

```
./gradlew :libraries:coal:coal-cloth-adapter-fabric:runTestmodClient
```

Press `K` in-game to open the settings screen. See [`TestmodConfig`](src/testmod/java/com/oliveryasuna/mc/coal/cloth/testmod/TestmodConfig.java) for the full annotated example.

## Thread safety

- `AdapterConfigManager` (in `coal-adapter-common`) guards `state`, `origins`, and every read-modify-write flow with an internal monitor. `EventBus.dispatch` runs after the lock
  is released; disk I/O in `save()` happens after the lock captures a fresh nested tree.
- `AdapterConfigProvider#install` uses `putIfAbsent` for atomic registration — concurrent `register(id)` calls with the same id are safe; exactly one wins.
- Reads (`get()`, `rawAt(...)`) are lock-free via the volatile `state` reference; a concurrent `set()` may produce field-level torn reads on the state POJO but not a broken
  reference.

## Limitations

Feature-set gaps relative to the COAL spec + Cloth Config's own capability set.

### Capabilities not advertised

The provider's `supports(Capability)` returns `false` for:

- **`SYNC`** — Cloth has no networking primitives; a full `SyncService` implementation would need to layer `coal-api-sync` separately.
- **`FILE_WATCH`** — `JsonConfigIO#fileWatchService()` returns `Optional.empty()`. No hot-reload on external file changes.
- **`JSON5`** — gson doesn't tolerate `//` comments; would need a JSON5 codec (Jankson, etc.).
- **`CUSTOM_FORMATS`** — hard-coded to `Format.JSON`. TOML / JSON5 requests fall back to JSON with a `WARN`.

### Cloth constraints (can't route around)

- **`@Reload(WORLD)`** and **`@Reload(LIVE)`** are read but not mapped to any Cloth flag. Cloth only exposes a coarse "requires restart" bit via `requireRestart()`; the granular
  "next world load" vs "applied live" distinction isn't representable in Cloth's model. Only `@Reload(RESTART)` / `@RequiresRestart` maps cleanly.

### Not-yet-implemented (v2 candidates)

- **`List<T>`** currently renders as a disabled placeholder. Cloth ships list widgets (`startStrList`, `startIntList`, etc.), but wiring the element-type dispatch to schema is a
  v2 task.
- **`Map<K, V>`** renders as a disabled placeholder (`(edit on disk)`). Cloth has no map primitive.
- **`@Length(min, max)` on `List<T>`** is only enforced at load-time correction; the list widget (once wired) will need `setMinLength` / `setMaxLength` calls to enforce in-GUI.
- **`Widget.allowInvalid()`** — read but ignored. Would require hooking Cloth's per-entry `setErrorSupplier(...)`.
- **`Corrector` on `set(...)`** — validators run only on `load()`; `set(path, badValue)` at runtime is accepted without validation. `save()` writes whatever's in state.
- **`BackupStrategy`** — `JsonConfigIO#backupStrategy()` returns `Optional.empty()`. Overwrites in place.
- **`@Pattern` in-GUI validation** — the validator runs at correction time only; the text field accepts anything and the value gets corrected on next load.
- **Codec-mediated leaves** (`UUID`, `Instant`, `Duration`, `ResourceLocation`, ...) fall through to the placeholder path.
- **Prism / packaged-jar canary** — dev `runTestmodClient` is proven, but the published jar hasn't been dropped into a real Prism install for smoke-testing.
- **`coal-testkit` conformance run** — no `src/test` extending `AbstractCoalConformanceTest` yet; the adapter isn't self-checked against the spec.

## License

MIT. See [../LICENSE](../LICENSE) at the COAL family root.
