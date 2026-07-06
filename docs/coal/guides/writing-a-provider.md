---
title: Write a provider
---

# Write a provider

You'll write a COAL provider if:

- You want to back COAL with a config library the first-party adapters don't cover.
- You're building an in-house / project-specific provider tuned to one mod pack's needs.
- You're forking a first-party adapter to change a specific behavior.

The SPI is small. A minimal viable provider is a `ConfigProvider` + `ConfigProviderFactory` + a `META-INF/services/` entry. A production-quality provider adds a `SchemaReader`, `Corrector`, `ConfigIO`, and (for GUI-delegating providers) a `ScreenProvider`.

The [`coal-adapter-common`](https://github.com/oliveryasuna/minecraft-mods/tree/main/libraries/coal/adapter-common) module is a working reference — used by both the YACL and Cloth adapters. This guide walks the shape without reproducing every line.

## Two SPIs

### `ConfigProviderFactory`

Discovered via `ServiceLoader`. Reports:

```java
public interface ConfigProviderFactory {
    String name();          // e.g. "coal-mycompany-adapter"
    int priority();         // >= 100 for real providers; 0 is reserved for coal-noop
    String coalVersion();   // the coal-api version you compiled against
    ConfigProvider create(Platform platform);
}
```

Ship the factory in your jar with a service entry:

```
META-INF/services/com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory
```

...containing one line: your factory's fully-qualified class name.

### `ConfigProvider`

The behavioural surface. See [`ConfigProvider`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api/src/main/java/com/oliveryasuna/mc/coal/api/spi/ConfigProvider.java) for the full method list:

- **Identity** — `name()`.
- **Registration** — `register(Class<S>, MigrationSpec)` and the `ConfigSpec` variant. Both return a `ConfigHandle`.
- **SPI collaborators** — `platform()`, `schemaReader()`, `corrector()`, `defaultIO()`. Consumer mods rarely call these directly; they exist so tests and advanced users can wire around them.
- **Accessors** — `registeredConfigIds()`, `getById(String)`.
- **Capabilities** — `supports(Capability)`.

## The internal pieces

Beyond the two SPI classes, a real provider needs:

### `SchemaReader`

Turns a `Class<S>` (or `ConfigSpec`) into a [`ConfigModel<S>`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api/src/main/java/com/oliveryasuna/mc/coal/api/schema/ConfigModel.java) — which pairs a `Schema` with a `newState()` factory.

The [`Schema`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api/src/main/java/com/oliveryasuna/mc/coal/api/schema/Schema.java) tree carries category structure, per-entry `ValueType`s, and per-entry `EntryMetadata`. `EntryMetadata` bundles comment, sync scope, reload tier, widget hint, hidden flag, validator list, and key override — every annotation gets materialized here.

The reference implementation for the first-party adapters — `AnnotationSchemaReader` in `coal-adapter-common` — uses reflection on `@Config`-annotated types. If you're writing a from-scratch provider you can copy or draw inspiration from that class.

### `Corrector`

Runs validators over a state instance and returns a `List<Correction>`. Called from `load()` after migrations. The reference implementation walks the schema, invokes each entry's validators, and applies suggestions (or field defaults) to failures.

### `ConfigIO`

Handles disk-side persistence:

```java
public interface ConfigIO {
    Optional<Map<String, Object>> read(Path file, Schema schema) throws IOException;
    void write(Path file, Map<String, Object> tree, Schema schema) throws IOException;
    boolean supports(Format format);
    Optional<BackupStrategy> backupStrategy();
    Optional<FileWatchService> fileWatchService();
}
```

The reference `JsonConfigIO` in `coal-adapter-common` uses gson. Providers advertising other formats plug in `FormatAdapter`s per [spec §14.2](../spec/#142-formatadapter).

### `ConfigManager<S>`

The heart of the runtime. Owns state, dispatches events, coordinates load / save / reload. The reference `AdapterConfigManager` is thread-safe (internal monitor guards state + origins) and lock-free on reads.

### Optional: `ScreenProvider`

If you're providing GUI delegation, register a `ScreenProvider` with `GuiRegistry.registerProvider(...)` at client-init. See [GUI delegation](../concepts/gui-delegation) for how selection works.

Advertise `Capability.GUI_DELEGATION` from your `ConfigProvider#supports`.

### Optional: `SyncService`

For sync support, implement `SyncService` from `coal-api-sync`. See [Server ↔ client sync](../concepts/sync) for the flow. Advertise `Capability.SYNC`.

## Picking a priority

- **`0`** — reserved for `coal-noop`.
- **`1–99`** — reserved for degraded / partial providers (rarely useful).
- **`100`** — first-party first-in-line. YACL and Cloth adapters ship at `100`.
- **`101+`** — deliberate overrides. Use this when your provider should outrank the first-party ones in a mixed install (e.g., a project-specific provider that adds sync).

Ties at the same priority resolve by `ServiceLoader` iteration order — non-deterministic. Don't rely on it; pick a distinct value.

## Capability discipline

`supports(Capability.X)` returning `true` is a **contract**. The conformance testkit checks it. Return `true` only for capabilities you fully implement.

Common startup pattern (from `AdapterConfigProvider`):

```java
private static final Set<Capability> SUPPORTED = Set.of(
        Capability.MIGRATION,
        Capability.VALIDATION,
        Capability.GUI_DELEGATION
);

@Override
public boolean supports(final Capability capability) {
    return SUPPORTED.contains(capability);
}
```

## Bootstrap surface — what the runtime expects

The COAL runtime calls **only**:

- `factory.name()`, `factory.priority()`, `factory.coalVersion()` at discovery.
- `factory.create(platform)` — once, at bootstrap.
- Every method on the returned `ConfigProvider`, but not before it's returned.

Your factory's `create` may set up caches, thread pools, IO handles — it's the natural place. It **must** succeed against the test-scope `Platform` used by the conformance testkit ([`TestkitPlatform`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/testkit/src/main/java/com/oliveryasuna/mc/coal/testkit/TestkitPlatform.java)).

## Testing

- **Baseline conformance** — extend `AbstractCoalConformanceTest`, implement `newFactory()`. See [Conformance-test a provider](./conformance-testing).
- **Per-capability conformance** — one abstract test class per capability (e.g. `AbstractMigrationCapabilityTest`, `AbstractValidationCapabilityTest`). Extend for each capability your provider advertises.
- **Ship a `ConformanceReport`** — publish the JSON result artifact with your release.

## Publishing

Standard mod publishing rules. Two loader variants (Fabric + NeoForge) are the norm — see the [`coal-yacl-adapter-fabric`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/yacl-adapter-fabric/build.gradle.kts) build script for a working template, including how to consume the `namedElements` variant of `coal-api-gui-fabric`, which cross-project mod deps need `modLocalRuntime`, and which artifacts JiJ.

## Related

- [Conformance-test a provider](./conformance-testing) — how the testkit validates your work.
- [Provider model](../concepts/provider-model) — how the runtime picks your factory.
- [Capabilities](../concepts/capabilities) — the honest-advertising contract.
- [Specification §11 through §16](../spec/#11-provider-spi--factory-and-provider) — normative rules for every SPI collaborator.
