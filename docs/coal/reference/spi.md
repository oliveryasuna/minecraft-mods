---
title: SPI reference
---

# SPI reference

Every consumer-facing and provider-facing type. Package prefix `com.oliveryasuna.mc.coal.api` is omitted from qualified names below.

For normative semantics see [spec §5](../spec/#5-user-api--the-coal-entry-point) onwards.

## Entry point — `.Coal`

Static entry point ([source](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api/src/main/java/com/oliveryasuna/mc/coal/api/Coal.java)).

| Method                                                               | Purpose                                                                                          |
|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `void bootstrap()`                                                   | Discover Platform + factories; install highest-priority provider. First-wins.                    |
| `void bootstrap(ConfigProvider p)`                                   | Unconditionally install `p`, replacing any previously installed provider. Tests + overrides.     |
| `<S> ConfigHandle<S> register(Class<S>)`                             | Register an annotated POJO; auto-bootstraps if not yet done.                                     |
| `<S> ConfigHandle<S> register(Class<S>, MigrationSpec)`              | Same, with migrations.                                                                           |
| `ConfigHandle<Map<String,Object>> register(ConfigSpec)`              | Register a dynamically-shaped config.                                                            |
| `ConfigHandle<Map<String,Object>> register(ConfigSpec, MigrationSpec)` | Same, with migrations.                                                                          |
| `ConfigProvider getProvider()`                                       | Installed provider (ensures bootstrap).                                                          |
| `boolean isBootstrapped()`                                           | Whether `bootstrap` (either variant) has been called.                                            |
| `String providerName()`                                              | Name of the installed provider, or `null` before bootstrap.                                      |
| `boolean isNoopProvider()`                                           | `true` when the installed provider is `coal-noop`.                                               |
| `String NOOP_PROVIDER_NAME` (constant)                               | `"coal-noop"`.                                                                                   |

## Handles and managers

### `.config.ConfigHandle<S>`

Returned by every `register` call. Long-lived; hold on to it.

```java
public interface ConfigHandle<S> {
    S get();
    void set(String dottedPath, Object value);
    void reload() throws IOException;
    void save() throws IOException;
    ConfigManager<S> manager();
    ConfigSnapshot snapshot();
    <T> ConfigValue<T> value(String dottedPath, Class<T> type);
}
```

### `.config.ConfigManager<S>`

Low-level surface — the handle's `manager()` method returns one.

```java
public interface ConfigManager<S> {
    Schema schema();
    S get();
    void set(String dottedPath, Object value);
    LoadResult load() throws IOException;
    void save() throws IOException;
    Path file();
    EventBus events();
    void addReloadListener(ReloadListener<S> listener);
    Origin originOf(String dottedPath);
    void markOrigins(Collection<String> paths, Origin origin);
    ConfigSnapshot snapshot();
}
```

### `.config.ConfigValue<T>`

Typed accessor for a single dotted path. Obtain via `handle.value(path, type)`.

```java
public interface ConfigValue<T> {
    T get();
    void set(T value);
    void onChange(Consumer<T> listener);
    String path();
    Class<T> type();
}
```

### `.config.ConfigSnapshot`

Point-in-time view of a config. Immutable.

```java
public interface ConfigSnapshot {
    Instant capturedAt();
    Schema schema();
    <T> Optional<T> get(String dottedPath, Class<T> type);
    Object getRaw(String dottedPath);
    Set<String> paths();
    boolean isPresent(String dottedPath);
}
```

### `.config.LoadResult`

Return of `ConfigManager#load()`.

```java
public record LoadResult(
        ConfigSnapshot snapshot,
        List<Correction> corrections,
        Optional<MigrationReport> migration
) {}
```

### `.config.Origin`

How a value at a path was set.

```java
public enum Origin {
    DEFAULT,       // never touched
    LOCAL_EDIT,    // set locally or read from disk
    FROM_REMOTE    // pushed by server sync
}
```

### `.config.ConfigSpec`

Dynamically-shaped config declaration. See [Define a config](../guides/defining-a-config#the-configspec-route) for the builder walkthrough.

## Events

### `.event.EventBus`

Per-manager pub/sub.

```java
public interface EventBus {
    Registration subscribe(ChangeListener listener);
    Registration subscribe(String pathPrefix, ChangeListener listener);
    void dispatch(ChangeEvent event);  // provider-internal

    interface Registration extends AutoCloseable {
        @Override void close();
    }
}
```

### `.event.ChangeEvent`

```java
public record ChangeEvent(
        String path,
        Object oldValue,
        Object newValue,
        Origin origin,
        Instant at
) {}
```

### `.event.ChangeListener` / `ReloadListener<S>`

Functional interfaces.

```java
@FunctionalInterface
public interface ChangeListener { void onChange(ChangeEvent event); }

@FunctionalInterface
public interface ReloadListener<S> { void onReload(S previous, S current); }
```

## Provider SPI

### `.spi.ConfigProviderFactory`

Discovered via `ServiceLoader`.

```java
public interface ConfigProviderFactory {
    String name();
    int priority();
    String coalVersion();
    ConfigProvider create(Platform platform);
}
```

### `.spi.ConfigProvider`

The installed provider.

```java
public interface ConfigProvider {
    String name();

    <S> ConfigHandle<S> register(Class<S> type, MigrationSpec migrations);
    ConfigHandle<Map<String, Object>> register(ConfigSpec spec, MigrationSpec migrations);

    Platform platform();
    SchemaReader schemaReader();
    Corrector corrector();
    ConfigIO defaultIO();

    Set<String> registeredConfigIds();
    Optional<ConfigHandle<?>> getById(String id);

    boolean supports(Capability capability);
}
```

### `.spi.Capability`

```java
public enum Capability {
    SYNC, MIGRATION, FILE_WATCH, VALIDATION, GUI_DELEGATION, JSON5, CUSTOM_FORMATS
}
```

See [Capabilities](../concepts/capabilities) for what each means.

### `.spi.ProviderNotFoundException`

Thrown when platform or factory discovery fails. Extends `RuntimeException`.

## Format

### `.Format`

```java
public interface Format {
    Format TOML  = new SimpleFormat("toml",  "toml",  true);
    Format JSON  = new SimpleFormat("json",  "json",  false);
    Format JSON5 = new SimpleFormat("json5", "json5", true);

    static Format of(String id);
    static Format of(String id, String extension, boolean comments);

    String id();
    String defaultExtension();
    boolean supportsComments();
}
```

Case-insensitive lookup. Unknown ids produce synthetic `Format`s with `supportsComments() == false` and the id as extension. Providers advertising `Capability.CUSTOM_FORMATS` may honor those; others reject them at load time.

Two `SimpleFormat` instances with the same `id` are equal regardless of extension or comment support — so `Format.of("toml").equals(Format.TOML)` holds.

## Platform

### `.platform.Platform`

Discovered exactly once via `ServiceLoader`. Loader integrations ship implementations.

```java
public interface Platform {
    Path configDir();
    Executor mainThreadExecutor();
    Logger logger(String name);
    Environment environment();
    Optional<Path> gameDir();
    Optional<String> loaderName();
    Optional<String> loaderVersion();
}
```

### `.platform.Environment`

```java
public enum Environment { CLIENT, SERVER }
```

### `.platform.PermissionGate`

Provider-facing hook for gating server-side edits.

```java
@FunctionalInterface
public interface PermissionGate {
    PermissionGate DENY_ALL  = (actor, level) -> false;
    PermissionGate ALLOW_ALL = (actor, level) -> true;

    boolean canEdit(Object actor, int requiredLevel);
}
```

## Schema (provider-facing, but consumer-readable)

### `.schema.Schema`

```java
public interface Schema {
    Class<?> type();
    String id();
    String name();
    Format format();
    int version();
    SchemaCategory root();
    Optional<SchemaEntry> find(String dottedPath);
    Set<String> paths();
}
```

### `.schema.SchemaCategory`

Nested tree — name, comment, entries, sub-categories.

### `.schema.SchemaEntry`

```java
public interface SchemaEntry {
    String key();
    ValueType type();
    Object defaultValue();
    EntryMetadata metadata();
    ValueAccessor accessor();
    Object readFrom(Object instance);
    void writeTo(Object instance, Object value);
}
```

### `.schema.EntryMetadata`

Materialized annotations for one entry.

```java
public interface EntryMetadata {
    static Builder builder();

    List<String> comment();
    Sync.Scope syncScope();
    Reload.Tier reloadTier();
    Widget.Type widget();
    boolean isHidden();
    List<Validator<?>> validators();
    Optional<String> keyOverride();
}
```

### `.schema.ValueType`

```java
public interface ValueType {
    Kind kind();
    Class<?> rawType();
    Optional<ValueType> elementType();     // for LIST / MAP value
    Optional<ValueType> valueType();       // for MAP
    List<SchemaEntry> children();          // for OBJECT

    enum Kind { SCALAR, ENUM, LIST, MAP, OBJECT }
}
```

## Migration

### `.migration.MigrationSpec`

```java
public interface MigrationSpec {
    static MigrationSpec empty();
    List<MigrationStep> steps();
}
```

### `.migration.MigrationStep`

```java
public interface MigrationStep {
    int fromVersion();
    int toVersion();
    List<MigrationOp> ops();
}
```

### `.migration.MigrationOp`

Functional interface with a single `void apply(Map<String, Object> tree)` method, plus five static factories: `renameKey`, `removeKey`, `setDefault`, `setValue`, `transform`. See [Migrate configs](../guides/migrating-configs) for semantics.

### `.migration.MigrationReport`

```java
public record MigrationReport(int fromVersion, int toVersion, List<AppliedStep> steps) {
    public record AppliedStep(int fromVersion, int toVersion, int opsApplied) {}
}
```

### `.migration.MigrationRegistry`

Fluent builder for providers that want to expose one.

## Validation

### `.validation.Validator<T>`

```java
@FunctionalInterface
public interface Validator<T> {
    ValidationResult validate(T value, ValidationContext ctx);

    interface ValidationContext {
        SchemaEntry entry();
        String path();
    }
}
```

### `.validation.ValidationResult`

Sealed — `Ok` or `Invalid`.

```java
public sealed interface ValidationResult permits ValidationResult.Ok, ValidationResult.Invalid {
    static Ok ok();
    static Invalid invalid(List<ValidationIssue> issues);
    static Invalid invalid(String message, Object suggestion);
    boolean isOk();
    boolean isInvalid();
    List<ValidationIssue> issues();
}
```

### `.validation.ValidationIssue`

```java
public record ValidationIssue(String message, Optional<Object> suggestion) {}
```

### `.validation.Corrector`

```java
@FunctionalInterface
public interface Corrector {
    List<Correction> correct(Schema schema, Object instance);
}
```

### `.validation.Correction`

```java
public record Correction(String path, Object before, Object after, String reason) {}
```

## IO

### `.io.ConfigIO`

```java
public interface ConfigIO {
    Optional<Map<String, Object>> read(Path file, Schema schema) throws IOException;
    void write(Path file, Map<String, Object> tree, Schema schema) throws IOException;
    boolean supports(Format format);
    Optional<BackupStrategy> backupStrategy();
    Optional<FileWatchService> fileWatchService();
}
```

### `.io.FormatAdapter`

```java
public interface FormatAdapter {
    Format format();
    Map<String, Object> parse(byte[] bytes) throws SerializationException;
    byte[] render(Map<String, Object> tree, Schema schema) throws SerializationException;
    boolean supportsComments();
}
```

### `.io.BackupStrategy` and `.io.FileWatchService`

Provider-optional. See spec §14.3 and §14.4.

### `.io.SerializationException`

Wraps format-parse / format-render failures. Extends `RuntimeException`.

## GUI SPI (`coal-api-gui-*`)

Two loader variants: `coal-api-gui-fabric` (Loom-intermediary) and `coal-api-gui-neoforge` (Mojmap). Same source, different bytecode.

### `.gui.GuiRegistry`

Static registry. See [GUI delegation](../concepts/gui-delegation).

```java
public final class GuiRegistry {
    public static void registerProvider(ScreenProvider provider);
    public static Optional<ScreenProvider> selected();
    public static Screen open(Minecraft client, Screen parent, ConfigManager<?> manager);
}
```

### `.gui.ScreenProvider`

```java
public interface ScreenProvider {
    String id();
    int priority();
    Screen create(Minecraft client, Screen parent, ConfigManager<?> manager);
}
```

## Sync SPI (`coal-api-sync`)

Optional module. See [Server ↔ client sync](../concepts/sync) for flows.

### `.sync.SyncService`

Big surface — registration, server-side broadcast, client-side edit forwarding, lifecycle. See [source](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api-sync/src/main/java/com/oliveryasuna/mc/coal/api/sync/SyncService.java).

### `.sync.ProtocolVersion`

```java
public record ProtocolVersion(int major, int minor) implements Comparable<ProtocolVersion> {
    public static final ProtocolVersion CURRENT = new ProtocolVersion(1, 0);
    public boolean isCompatibleWith(ProtocolVersion other);
}
```

Same `major` = compatible. Minor diffs are additive.

### `.sync.SyncPayload`

Sealed — `Handshake`, `Snapshot`, `Delta`, `ClientEdit`.

### `.sync.NetworkTransport`, `.sync.PayloadCodec`, `.sync.ScopeEnforcer`, `.sync.InboundValidator`

Provider-facing collaborators. See [spec §16](../spec/#16-optional-module--synchronization-coal-api-sync).

### `.sync.WireFormatException`

Codec-decode failures.

## Related

- [Specification](../spec/) — the normative document.
- [Provider model](../concepts/provider-model) — the discovery / selection story.
- [Write a provider](../guides/writing-a-provider) — the SPI up close.
