# coal-adapter-common

Shared Minecraft-free implementation core for COAL provider adapters. Contains an annotation-driven `SchemaReader`, a gson-backed `ConfigIO`, a default `Corrector`, a thread-safe `ConfigManager`, an `EventBus`, a screen-support helper, and the `AdapterConfigProvider` implementation of the COAL SPI.

> [!IMPORTANT]
> **Provisional API — v1 shape may change.**
>
> This module is published to Maven Central so third-party adapter authors can bootstrap on top of a working reference implementation. **The public class shapes here are not covered by the [COAL specification](../../../docs/coal/spec/index.md) stability guarantees.** Signatures may evolve without a major-version bump until the third-party adapter surface stabilizes.
>
> If you're writing an adapter and this concerns you, either (a) copy the classes you need into your own module (this is small and self-contained), or (b) pin a specific version and follow the changelog on upgrade.

## What it does

- Reads a `Class<S>` annotated with COAL's `@Config` (and every constraint / metadata annotation) into a `Schema` + `ConfigModel<S>`.
- Reads a `ConfigSpec` (dynamic route) into the same shape.
- Persists `Map<String, Object>` trees as JSON via gson (2-space pretty-print, null-serializing, HTML escaping off).
- Runs the built-in `Corrector` on every load: `@Range` / `@Pattern` / `@OneOf` / `@Length` / `@NotNull` corrections surface in `LoadResult.corrections`.
- Provides an `AdapterConfigProvider` that plugs into the COAL runtime — the four first-party loader modules (YACL Fabric / NeoForge and Cloth Fabric / NeoForge) all use this class as their `ConfigProvider`, differing only in the loader-local `ConfigProviderFactory` and `ScreenProvider`.

## Consumers

- **First-party adapters** — [`coal-yacl-adapter-fabric`](../yacl-adapter-fabric/), [`coal-yacl-adapter-neoforge`](../yacl-adapter-neoforge/), [`coal-cloth-adapter-fabric`](../cloth-adapter-fabric/), [`coal-cloth-adapter-neoforge`](../cloth-adapter-neoforge/).
- **Third-party adapter authors** — anyone implementing the [COAL provider SPI](../../../docs/coal/guides/writing-a-provider.md) who wants persistence + validation + migration + event-bus wiring off the shelf.

## Class overview

| Class                    | Purpose                                                                                          |
|--------------------------|--------------------------------------------------------------------------------------------------|
| `AdapterConfigProvider`  | `ConfigProvider` implementation. Parameterized by provider name (constructor arg).               |
| `AdapterConfigManager`   | `ConfigManager<S>` — thread-safe state + origins + save/load coordination.                       |
| `AdapterConfigHandle`    | `ConfigHandle<S>` — the caller-facing handle returned from `register(...)`.                      |
| `AdapterConfigValue`     | Typed path accessor (`ConfigValue<T>`) with `onChange` support.                                  |
| `AdapterConfigSnapshot`  | Immutable point-in-time view.                                                                    |
| `AdapterEventBus`        | `EventBus` implementation with path-prefix filtering.                                             |
| `AdapterScreenSupport`   | Helper functions shared by loader-specific `ScreenProvider` implementations (validator lookup, `#RRGGBB` parse / format helpers, etc.). |
| `AnnotationSchemaReader` | Reflection-based `SchemaReader<Class<S>>` for annotated POJOs.                                   |
| `DefaultCorrector`       | Walks the schema, runs validators, applies corrections.                                          |
| `JsonConfigIO`           | `ConfigIO` implementation for `Format.JSON`. Substitutes JSON on `Format.TOML` / `JSON5` with a `WARN`. |
| `JsonFormatAdapter`      | The gson-backed `FormatAdapter` used by `JsonConfigIO`.                                          |
| `Schemas`                | Static utilities on the `Schema` shape.                                                          |
| `Validators`             | Built-in `Validator` implementations for the constraint annotations.                             |
| `Values`                 | Static helpers for reading/writing values by dotted path.                                         |

## Consuming as a Gradle dep

```kotlin
dependencies {
    implementation("com.oliveryasuna.mc:coal-adapter-common:<version>")
}
```

You'll also want `coal-api` on the classpath (transitive from this module) and a Platform implementation — typically shipped by whichever loader integration your adapter targets.

## What's advertised

The `AdapterConfigProvider` returns `true` from `supports(...)` for:

- `Capability.MIGRATION`
- `Capability.VALIDATION`
- `Capability.GUI_DELEGATION`

Not advertised: `SYNC`, `FILE_WATCH`, `JSON5`, `CUSTOM_FORMATS`. Adapters that want those layer them on top — `JsonConfigIO#fileWatchService()` returns `Optional.empty()`, and there's no bundled `SyncService` in v1.

## Testing

Uses `coal-testkit` as a `testImplementation` dep. Both the YACL and Cloth adapter conformance suites live in this module's `src/test/` — they exercise the shared behavior via harnesses that mirror the loader-local factory identities.

## License

MIT. See [../LICENSE](../LICENSE) at the COAL family root.