# COAL Specification

**Status:** Draft (pre-1.0). This document tracks the shape of the API as it exists on `main`. Section numbering and normative wording will stabilise for 1.0.

**Version:** matches `coal-api` `0.1.x`. Every section that pins a behavior an implementation must honor is versioned by the spec revision, not by the artifact version — see [§20](#20-versioning).

---

## Table of contents

1. [Introduction](#1-introduction)
2. [Conventions and terminology](#2-conventions-and-terminology)
3. [Discovery, bootstrap, and lifecycle](#3-discovery-bootstrap-and-lifecycle)
4. [The Format subsystem](#4-the-format-subsystem)
5. [User API — the `Coal` entry point](#5-user-api--the-coal-entry-point)
6. [User API — handles, managers, snapshots](#6-user-api--handles-managers-snapshots)
7. [User API — the annotation schema DSL](#7-user-api--the-annotation-schema-dsl)
8. [User API — programmatic schema (`ConfigSpec`)](#8-user-api--programmatic-schema-configspec)
9. [User API — migrations](#9-user-api--migrations)
10. [User API — events and reload](#10-user-api--events-and-reload)
11. [Provider SPI — factory and provider](#11-provider-spi--factory-and-provider)
12. [Provider SPI — schema reading](#12-provider-spi--schema-reading)
13. [Provider SPI — validation and correction](#13-provider-spi--validation-and-correction)
14. [Provider SPI — IO](#14-provider-spi--io)
15. [Platform contract](#15-platform-contract)
16. [Optional module — synchronization (`coal-api-sync`)](#16-optional-module--synchronization-coal-api-sync)
17. [Optional module — GUI delegation (`coal-api-gui-*`)](#17-optional-module--gui-delegation-coal-api-gui-)
18. [Threading model](#18-threading-model)
19. [Error handling](#19-error-handling)
20. [Versioning](#20-versioning)
21. [Conformance](#21-conformance)
22. [Appendix A — Rationale](#appendix-a--rationale)
23. [Appendix B — Glossary](#appendix-b--glossary)
24. [Appendix C — Reserved for future revisions](#appendix-c--reserved-for-future-revisions)

---

## 1. Introduction

### 1.1 Purpose

The **C**onfig **O**ptions **A**bstraction **L**ayer (COAL) is a stable API and provider SPI for Minecraft mod configuration. It plays the same role for configuration that SLF4J plays for logging: mods depend only on `coal-api`; the actual reading, writing, validating, migrating, and GUI-rendering of configuration is done by whichever `ConfigProvider` is on the classpath at runtime.

The COAL specification (this document) defines:

1. The public **user API** every mod may call.
2. The **provider SPI** every conforming implementation MUST honor.
3. The **Platform** contract every loader integration (Fabric, NeoForge) MUST honor.
4. The **discovery mechanism** that binds these three together.
5. Optional **modules** (`coal-api-sync`, `coal-api-gui-{fabric,neoforge}`) with their own capability contracts.
6. The **conformance** obligations that let a provider claim it implements COAL.

### 1.2 Scope

COAL specifies:

- The shape of every public type in the `com.oliveryasuna.mc.coal.api.*`, `com.oliveryasuna.mc.coal.api.sync.*`, and `com.oliveryasuna.mc.coal.api.gui.*` packages.
- The semantics of every documented public method — what it MUST return, when it MUST throw, what side effects it MAY have.
- The ServiceLoader-based discovery and installation flow.
- The threading and error-handling model.
- The rules for advertising and honoring `Capability` values.
- The wire-format contract for cross-provider sync interoperability.

### 1.3 Non-scope

COAL deliberately does not specify:

- How a `ConfigProvider` maps annotated Java classes onto a config file layout beyond what the schema types describe. Providers MAY choose any reflection strategy, code-generation strategy, or hybrid.
- Which file format a provider defaults to when a mod does not request one. Providers SHOULD respect `@Config(format = ...)` and the `ConfigSpec` `Format` field, but the choice of default format when the mod requests `AUTO` is provider-specific.
- User preference resolution for GUI frontends. `GuiRegistry` selects by priority; how a provider computes those priorities from user config is provider-specific.
- Provider-internal caching, hot-reload debouncing, or memoization policies. These MUST NOT be observable through the public API in a way that breaks the documented contracts.
- The set of *third-party* backing libraries a provider may use. `coal-yacl`, `coal-cloth`, `coal-rubric`, `coal-forge-config` are all valid outcomes of the same spec.

### 1.4 Audience

Three audiences read this spec, each with a different reading order:

- **Mod authors** (consumers of COAL) — start at [§5](#5-user-api--the-coal-entry-point) and work forward through [§10](#10-user-api--events-and-reload). Return to [§4](#4-the-format-subsystem) for file-format questions and to [§16–§17](#16-optional-module--synchronization-coal-api-sync) for the optional modules.
- **Provider authors** (implementers of the SPI) — read the entire document. Sections [§11–§14](#11-provider-spi--factory-and-provider) plus [§19](#19-error-handling), [§20](#20-versioning), and [§21](#21-conformance) are your obligations.
- **Loader-integration authors** (writers of `coal-fabric`, `coal-neoforge`, or equivalents for future loaders) — read [§3](#3-discovery-bootstrap-and-lifecycle) and [§15](#15-platform-contract).

### 1.5 Status

The spec is normative. Where a code file's Javadoc says "SHOULD" or "MUST", the spec is authoritative and the Javadoc is a summary. Where a Javadoc adds behavioral guarantees not present in the spec, those are **informational** and MAY be relaxed in future revisions of `coal-api`.

Where a code file uses lowercase "must" / "should" without RFC 2119 keywords, treat the spec as authoritative unless the spec is silent on that specific point.

---

## 2. Conventions and terminology

### 2.1 Requirement keywords

The key words **MUST**, **MUST NOT**, **SHOULD**, **SHOULD NOT**, **MAY**, **REQUIRED**, **SHALL**, **SHALL NOT**, **RECOMMENDED**, **NOT RECOMMENDED**, and **OPTIONAL** in this document are to be interpreted as described in [RFC 2119](https://www.rfc-editor.org/rfc/rfc2119) and [RFC 8174](https://www.rfc-editor.org/rfc/rfc8174) when, and only when, they appear in all capitals.

Lowercase "must", "should", "may" are used in explanatory prose and carry their ordinary English meaning.

### 2.2 Terminology

The following terms carry defined meanings in this document; see [Appendix B](#appendix-b--glossary) for the full list.

- **COAL runtime** — the code path in `Coal` that discovers a `Platform` and a `ConfigProvider` and delegates every subsequent call to them.
- **Consumer mod** — a mod that calls `Coal.register(...)` to obtain configuration handles.
- **Provider** — an implementation of `ConfigProvider` shipped by an implementation module (e.g., `coal-rubric`, `coal-noop`).
- **Loader integration** — a Minecraft mod that ships a `Platform` implementation and JiJ-bundles the COAL API jars. The two loader integrations in this repo are `coal-fabric` and `coal-neoforge`; both ship as a mod named `coal`.
- **Last-resort provider** — `coal-noop`. Bundled inside the `coal` mod. Priority `0`. Guaranteed to be present at runtime, so [§3.8](#38-the-last-resort-provider-coal-noop) applies.
- **Entry** — a single named setting within a config. Corresponds to a `SchemaEntry` at the provider layer and, for annotation-driven configs, to a `@Config`-annotated class field.
- **Config identity** — the `id` provided in `@Config(id = ...)` or `ConfigSpec.Builder(id)`. Uniquely identifies a config within a provider instance.
- **Dotted path** — see [§2.4](#24-dotted-paths).
- **Tree** — the parsed representation of a config file used during migration: `Map<String, Object>` where nested tables are also `Map<String, Object>`, lists are `List<Object>`, and scalars are `String`, `Number`, `Boolean`, or `null`.

### 2.3 Package prefixes

- `api.*` — the required `coal-api` module.
- `api.sync.*` — the optional `coal-api-sync` module. See [§16](#16-optional-module--synchronization-coal-api-sync).
- `api.gui.*` — the optional `coal-api-gui-fabric` / `coal-api-gui-neoforge` modules. See [§17](#17-optional-module--gui-delegation-coal-api-gui-).

Every reference in this document to a `com.oliveryasuna.mc.coal.api.X` type MAY be abbreviated to `X` or `api.X` when the containing module is unambiguous.

### 2.4 Dotted paths

A **dotted path** locates a scalar, table, or list inside a nested `Map<String, Object>` tree. Segments are separated by `.` (U+002E FULL STOP). Segments MUST NOT be empty. Empty-string paths MUST be treated as "the root" only in contexts that document that meaning explicitly (currently: `EntrySpec.categoryPath`).

A COAL runtime MUST NOT interpret leading, trailing, or repeated `.` characters — such paths are malformed. Providers MAY reject them at registration or treat them as look-through-to-missing at runtime. This spec does not require one over the other; providers SHOULD document their choice.

Dotted paths are the addressing scheme used by:

- `ConfigManager.set(String, Object)`, `ConfigManager.originOf(String)`, `ConfigManager.markOrigins(Collection, Origin)`
- `ConfigHandle.set(String, Object)`, `ConfigHandle.value(String, Class)`
- `ConfigSnapshot.get(String, Class)`, `ConfigSnapshot.getRaw(String)`, `ConfigSnapshot.isPresent(String)`
- Every `MigrationOp` factory
- `Schema.find(String)`
- `EntrySpec.categoryPath`

---

## 3. Discovery, bootstrap, and lifecycle

### 3.1 Overview

COAL uses `java.util.ServiceLoader` to discover both a `Platform` and one or more `ConfigProviderFactory` instances. The runtime resolves both, selects a factory, invokes `factory.create(Platform)`, and installs the resulting `ConfigProvider`.

There are exactly two public entry points into installation:

1. `Coal.bootstrap()` — [§3.5](#35-bootstrap-invariants).
2. `Coal.bootstrap(ConfigProvider)` — [§3.7](#37-explicit-bootstrap-override).

Everything else auto-triggers case (1) if no installation has happened yet — [§3.6](#36-auto-bootstrap-on-first-register).

### 3.2 Platform discovery

The COAL runtime MUST load `Platform` instances via `ServiceLoader.load(Platform.class)`.

- If zero `Platform` instances are present, the runtime MUST throw `ProviderNotFoundException` with a message that names the missing SPI and identifies the loader integrations that provide it (e.g., "Install a loader integration (e.g., the 'coal' mod for Fabric or NeoForge).").
- If more than one `Platform` instance is present, the runtime MUST throw `ProviderNotFoundException` and the exception's message MUST enumerate the fully-qualified class names of every discovered `Platform`.
- If exactly one `Platform` instance is present, the runtime MUST use it for the remainder of the bootstrap.

A loader integration provides its `Platform` by shipping `META-INF/services/com.oliveryasuna.mc.coal.api.platform.Platform` inside its mod jar. The value of that file MUST be the fully-qualified class name of an implementation with a public no-argument constructor.

The `Platform` instance discovered by `ServiceLoader` MUST NOT vary within a game process. Loader integrations MAY hold a reference to their own `Platform` instance (e.g., in a `static volatile` field) so that lifecycle events can mutate its internal state, but the object identity MUST remain the same for the process lifetime.

### 3.3 Provider factory discovery

The COAL runtime MUST load `ConfigProviderFactory` instances via `ServiceLoader.load(ConfigProviderFactory.class)`.

- If zero factories are discovered, the runtime MUST throw `ProviderNotFoundException` with a message that flags the missing bundled `coal-noop` as evidence that the classpath is misconfigured.
- If one or more factories are discovered, the runtime MUST proceed to selection ([§3.4](#34-provider-selection)).

Because the `coal` mod JiJ-bundles `coal-noop`, an installation with at least one loader integration on the mod list has at least one factory on the classpath at all times. A production `ProviderNotFoundException` from this path indicates a classpath issue, not a missing feature.

### 3.4 Provider selection

Given a non-empty list of discovered factories, the COAL runtime MUST:

1. Sort by `ConfigProviderFactory.priority()`, descending.
2. Select the factory at the head of the sorted list.
3. Invoke `selected.create(platform)`, passing the `Platform` from [§3.2](#32-platform-discovery).
4. Retain the returned `ConfigProvider` as the process-wide installed provider.
5. Log an `INFO`-level message containing the selected factory's `name()` and `priority()`.

If the list contained more than one factory, the runtime MUST additionally emit a `WARN`-level message enumerating every discovered factory (including the selected one) with each factory's `name()` and `priority()`.

The runtime MAY log the discovered `Platform`'s class name at `DEBUG` after selection.

Ties in priority are resolved by the order in which `ServiceLoader` visits factories. This spec does not further constrain the tie-break. Providers SHOULD choose distinct, deliberate priority values to avoid relying on the tie-break.

### 3.5 Bootstrap invariants

`Coal.bootstrap()` MUST:

1. Return immediately without side effects if a provider is already installed. In that case it MUST emit a `WARN`-level message identifying the installed provider by name.
2. Otherwise, perform [§3.2](#32-platform-discovery) and [§3.3](#33-provider-factory-discovery)–[§3.4](#34-provider-selection) under a single monitor so that concurrent callers observe first-wins semantics.
3. On success, `Coal.isBootstrapped()` MUST return `true` immediately after `bootstrap()` returns, and every subsequent call to `Coal.getProvider()` MUST return the installed provider until the process ends.

`Coal.bootstrap()` MUST NOT rethrow any exception raised by `ConfigProviderFactory.create(Platform)`. If `create` throws, the runtime MUST propagate the exception to the caller of `Coal.bootstrap()` unchanged. It MAY wrap non-`RuntimeException` throwables in a `ProviderNotFoundException`, but this spec does not require it.

### 3.6 Auto-bootstrap on first register

If a mod calls `Coal.register(...)` before `Coal.bootstrap()`, the runtime MUST auto-bootstrap according to [§3.5](#35-bootstrap-invariants) before performing the registration. The auto-bootstrap MUST be transparent to the caller: on success, `register` returns a valid `ConfigHandle`; on failure, `register` propagates the same exception `Coal.bootstrap()` would have propagated.

Loader integrations SHOULD call `Coal.bootstrap()` at mod-init so that any bootstrap errors surface before consumer mods try to register. Where `bootstrap()` succeeds at mod-init, subsequent `register` calls do not auto-bootstrap — they simply route to the installed provider.

### 3.7 Explicit bootstrap override

`Coal.bootstrap(ConfigProvider)` MUST:

1. Reject a `null` argument by throwing `NullPointerException` (or an equivalent argument-validation exception from a helper library).
2. Unconditionally install the supplied `ConfigProvider`, replacing any previously installed provider.
3. Emit an `INFO`-level log line naming the previous provider (if any) and the new provider.
4. Update the internal `providerName` bookkeeping so `Coal.providerName()` and `Coal.isNoopProvider()` return values consistent with the new installation.

This method is intended for tests and deliberate mid-run overrides. Production consumer mods SHOULD NOT call it.

### 3.8 The last-resort provider (`coal-noop`)

The `coal` mod (both Fabric and NeoForge variants) MUST JiJ-bundle `coal-noop`. `coal-noop` MUST:

- Report `name() == "coal-noop"`.
- Report `priority() == 0`.
- Be discovered via `META-INF/services/com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory` in the `coal-noop` jar.

Because bundled `coal-noop` is always on the classpath, `Coal.isNoopProvider()` acts as a runtime signal that no *real* provider is installed. Loader integrations SHOULD surface a user-visible warning when `isNoopProvider()` returns `true`. See [§17](#17-optional-module--gui-delegation-coal-api-gui-) for the shape of that warning on Fabric and NeoForge as of this spec revision.

Behavioral guarantees of `coal-noop` are specified in [§11.5](#115-the-coal-noop-provider).

### 3.9 Shutdown

COAL does not define a shutdown API. Installed providers are process-scoped. A conforming loader integration MAY invoke provider-internal shutdown (e.g., close file-watch services) through provider-specific hooks, but the COAL runtime itself has no `Coal.shutdown()` and MUST NOT expose one.

Consumers holding references to `ConfigHandle`, `ConfigManager`, `EventBus.Registration`, or `FileWatchService.Registration` values MUST assume those references stay valid for the process lifetime. Providers MUST NOT invalidate them without a documented lifecycle event (which this spec does not define).

---

## 4. The Format subsystem

### 4.1 The `Format` interface

`com.oliveryasuna.mc.coal.api.Format` describes the on-disk representation of a config file. It has three abstract methods:

- `String id()` — a stable, lowercase identifier. MUST NOT contain uppercase characters, whitespace, or path separators.
- `String defaultExtension()` — the default file extension, no leading dot.
- `boolean supportsComments()` — whether the format preserves per-entry comments across a load/save round trip.

`Format` is an **open set**. Consumer mods MAY use any `Format` instance; providers MAY refuse `Format` instances they do not recognize. See [§4.4](#44-custom-formats-and-capabilitycustom_formats).

### 4.2 Built-in singletons

`coal-api` defines three built-in `Format` singletons:

| Constant | `id()` | `defaultExtension()` | `supportsComments()` |
|---|---|---|---|
| `Format.TOML` | `"toml"` | `"toml"` | `true` |
| `Format.JSON` | `"json"` | `"json"` | `false` |
| `Format.JSON5` | `"json5"` | `"json5"` | `true` |

These constants are backed by `Format.SimpleFormat`, a nested `record` whose equality is defined solely by `id()`. Two `SimpleFormat` instances with the same id compare `equals`, regardless of extension or comment support.

### 4.3 Synthetic Format construction

`Format.of(String id)` MUST:

- Return the corresponding built-in singleton when `id` matches a built-in identifier case-insensitively. `Format.of("Toml") == Format.TOML`.
- Otherwise, return a fresh `SimpleFormat` with `id` used for both the identifier and the default extension, and `supportsComments()` set to `false`.

`Format.of(String id, String extension, boolean comments)` MUST return a fresh `SimpleFormat` constructed from the supplied arguments. This overload MUST NOT consult the built-in singletons — a caller who explicitly supplies `id = "toml"` and `extension = "conf"` gets exactly what they asked for.

### 4.4 Custom formats and `Capability.CUSTOM_FORMATS`

A provider MAY choose to support arbitrary `Format` ids beyond the three built-in singletons. A provider MUST advertise this by returning `true` from `supports(Capability.CUSTOM_FORMATS)`.

A provider that does NOT advertise `CUSTOM_FORMATS` MAY still receive a request to register a config with a synthetic `Format`. In that case the provider MUST either:

1. Fall back to a supported format (with a `WARN` log naming the mod, the requested format, and the substitution), or
2. Fail the registration by throwing `IllegalArgumentException` or `SerializationException` at load time.

Providers MUST NOT silently succeed with mismatched-format behavior — a mod requesting JSON5 comments and receiving stripped output is a data-loss surprise.

---

## 5. User API — the `Coal` entry point

`com.oliveryasuna.mc.coal.api.Coal` is a final, non-instantiable class. Its constructor is `private` and throws on invocation.

The complete public API is:

```java
public static final String NOOP_PROVIDER_NAME = "coal-noop";

public static void bootstrap();
public static void bootstrap(ConfigProvider explicitProvider);

public static <S> ConfigHandle<S> register(Class<S> type);
public static <S> ConfigHandle<S> register(Class<S> type, MigrationSpec migrations);
public static ConfigHandle<Map<String, Object>> register(ConfigSpec spec);
public static ConfigHandle<Map<String, Object>> register(ConfigSpec spec, MigrationSpec migrations);

public static ConfigProvider getProvider();
public static boolean isBootstrapped();
public static String providerName();
public static boolean isNoopProvider();
```

### 5.1 `bootstrap()`

Discovers and installs a provider per [§3](#3-discovery-bootstrap-and-lifecycle). First-wins. Subsequent calls no-op with a `WARN` log identifying the currently-installed provider.

### 5.2 `bootstrap(ConfigProvider)`

Unconditionally replaces any previously-installed provider per [§3.7](#37-explicit-bootstrap-override). Argument MUST NOT be null.

### 5.3 `register(Class<S>)` and `register(Class<S>, MigrationSpec)`

Registers an annotation-driven config. The type `S` MUST be annotated `@Config` at the class level; providers MUST reject un-annotated types by throwing `IllegalArgumentException` (or a subtype).

The single-arg overload is equivalent to calling the two-arg overload with `MigrationSpec.empty()`.

If COAL is not bootstrapped, the runtime MUST auto-bootstrap per [§3.6](#36-auto-bootstrap-on-first-register) before invoking the installed provider's `register(Class, MigrationSpec)`.

The returned `ConfigHandle` MUST NOT be `null`. See [§6.1](#61-confighandle) for handle semantics.

### 5.4 `register(ConfigSpec)` and `register(ConfigSpec, MigrationSpec)`

Registers a programmatically-described config whose state is a `Map<String, Object>`. See [§8](#8-user-api--programmatic-schema-configspec).

`spec` MUST NOT be `null`. The runtime MUST propagate the same exception surface as [§5.3](#53-registerclasss-and-registerclasss-migrationspec) for auto-bootstrap failures.

### 5.5 `getProvider()`

Returns the installed provider. If none is installed, the runtime MUST auto-bootstrap per [§3.6](#36-auto-bootstrap-on-first-register). Never returns `null`.

### 5.6 `isBootstrapped()`

Returns `true` if a provider is installed. Does NOT trigger auto-bootstrap.

### 5.7 `providerName()`

Returns the `name()` of the installed provider, or `null` if COAL is not bootstrapped. Loader integrations use this for warning surfaces per [§17](#17-optional-module--gui-delegation-coal-api-gui-).

### 5.8 `isNoopProvider()`

Returns `true` iff `providerName()` equals `Coal.NOOP_PROVIDER_NAME`. Returns `false` if COAL is not bootstrapped.

---

## 6. User API — handles, managers, snapshots

### 6.1 `ConfigHandle<S>`

Returned by every `Coal.register(...)` overload. The type parameter `S` is:

- The annotated class type for `register(Class<S>, ...)`.
- `Map<String, Object>` for `register(ConfigSpec, ...)`.

Interface:

```java
S get();
void set(String dottedPath, Object value);
void reload() throws IOException;
void save() throws IOException;
ConfigManager<S> manager();
ConfigSnapshot snapshot();
<T> ConfigValue<T> value(String dottedPath, Class<T> type);
```

Semantics:

- `get()` MUST return a live view of the current state. Providers MAY return the same instance across calls or a fresh copy per call; consumers MUST NOT rely on either. When the returned instance is mutable and the provider supports change notification, mutations to that instance MUST NOT bypass the change-notification pipeline. Providers that cannot enforce this SHOULD return a defensive copy from `get()`.
- `set(dottedPath, value)` MUST:
  - Update the underlying config state.
  - Trigger any change-notification pipeline the provider supports.
  - Update the origin of the affected path to `Origin.LOCAL_EDIT`.
  - Not perform a disk write. Callers invoke `save()` for that.
- `reload()` MUST re-parse the backing file, run any registered migrations, run corrections, and dispatch reload notifications to `ReloadListener`s registered on the `ConfigManager`.
- `save()` MUST render the current state to disk. Providers MAY apply a `BackupStrategy` before overwriting.
- `manager()` returns the underlying `ConfigManager<S>` for advanced usage. See [§6.2](#62-configmanagers).
- `snapshot()` returns a moment-in-time `ConfigSnapshot`. See [§6.4](#64-configsnapshot).
- `value(dottedPath, Class)` returns a typed accessor on the given path. See [§6.3](#63-configvaluet).

The `IOException` on `reload()` and `save()` MUST reflect an underlying I/O failure. Serialization/deserialization failures SHOULD be wrapped in `SerializationException` (see [§14.5](#145-serializationexception)) — but this spec does not require a specific exception hierarchy on either path.

### 6.2 `ConfigManager<S>`

Provider-facing surface for a single registered config. Interface:

```java
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
```

Semantics:

- `schema()` MUST return the same `Schema` instance across the manager's lifetime.
- `get()` and `set(...)` MUST mirror `ConfigHandle.get()` and `ConfigHandle.set(...)`.
- `load()` MUST behave equivalently to `ConfigHandle.reload()` but return a `LoadResult` — see [§6.5](#65-loadresult).
- `file()` returns the on-disk file path this manager reads and writes. Providers backed by an in-memory store SHOULD still return a stable `Path` (potentially under `Platform.configDir()`) even if that file does not exist.
- `events()` returns the `EventBus` for change notifications. See [§10.1](#101-eventbus-and-registration).
- `addReloadListener(...)` registers a listener that fires whenever the manager successfully completes a `load()`. See [§10.4](#104-reloadlistener).
- `originOf(dottedPath)` returns the current `Origin` of the value at that path. Never `null`. If the path is unknown, providers MUST return `Origin.DEFAULT`.
- `markOrigins(paths, origin)` bulk-updates the origin of every listed path. Intended for provider-internal use during load and sync — mod authors typically do not call it.
- `snapshot()` MUST return a `ConfigSnapshot` that reflects the state as of the moment of the call.

### 6.3 `ConfigValue<T>`

A typed accessor for a single path.

```java
T get();
void set(T value);
void onChange(Consumer<T> listener);
String path();
Class<T> type();
```

Semantics:

- `get()` returns the current value, coerced to `T`. If the underlying value is not assignable to `T`, providers MUST throw `ClassCastException`.
- `set(value)` MUST behave equivalently to `ConfigManager.set(path, value)` and MUST update `Origin`.
- `onChange(listener)` MUST invoke `listener` every time the value at `path()` changes for any reason. Providers MUST NOT drop changes even when the new value equals the old one, unless the provider has documented an equality-based short-circuit — this spec does not require one.

`path()` returns the dotted path this accessor was created with. `type()` returns the `Class<T>` supplied at creation.

### 6.4 `ConfigSnapshot`

An immutable view of a config's state at a specific instant.

```java
Instant capturedAt();
Schema schema();
<T> Optional<T> get(String dottedPath, Class<T> type);
Object getRaw(String dottedPath);
Set<String> paths();
boolean isPresent(String dottedPath);
```

Semantics:

- `capturedAt()` MUST return the instant at which the snapshot was created. Providers MAY use `Instant.now()` or, for special no-op cases (e.g. `coal-noop`), `Instant.EPOCH`.
- `schema()` returns the `Schema` used by the manager that produced this snapshot.
- `get(dottedPath, type)` returns the value at `dottedPath`, coerced to `type`, wrapped in `Optional`. Returns `Optional.empty()` if the path is absent OR if the value is not assignable to `type`.
- `getRaw(dottedPath)` returns the value at `dottedPath` without type coercion. Returns `null` if the path is absent.
- `paths()` returns every dotted path present in the snapshot. Providers MAY choose whether to include paths whose value is `null`.
- `isPresent(dottedPath)` returns `true` iff the path is in `paths()`.

Snapshots MUST NOT mutate. Providers MUST NOT return snapshots that share mutable state with the underlying config.

### 6.5 `LoadResult`

A `record` returned by `ConfigManager.load()`.

```java
record LoadResult(ConfigSnapshot snapshot,
                  List<Correction> corrections,
                  Optional<MigrationReport> migration)
```

Semantics:

- `snapshot` reflects the state after all load-time transformations (migration, correction). Never `null`.
- `corrections` lists every correction applied during load. Empty if none. MUST be non-null.
- `migration` is `Optional.of(report)` iff at least one migration step ran; `Optional.empty()` otherwise.

### 6.6 `Origin`

An `enum` describing the provenance of a value.

- `DEFAULT` — never touched. The value is whatever the schema declared as default.
- `LOCAL_EDIT` — set via `ConfigHandle.set(...)` / `ConfigManager.set(...)` / `ConfigValue.set(...)`, or read from disk.
- `FROM_REMOTE` — pushed by a server sync via `coal-api-sync`.

Providers that do not implement sync MAY still return `FROM_REMOTE` if they layer an external sync mechanism on top of COAL, but this spec does not require it.

---

## 7. User API — the annotation schema DSL

Annotations live in `com.oliveryasuna.mc.coal.api.annotation.*`. All annotations are `@Documented` and `@Retention(RUNTIME)`.

Providers MUST honor every annotation defined in this section for annotation-driven configs. A provider MAY additionally honor annotations defined outside this spec, but such extensions MUST NOT re-use the `com.oliveryasuna.mc.coal.api.annotation.*` package.

### 7.1 `@Config`

Marks the root of a configuration.

```java
@Target(TYPE)
public @interface Config {
    String id();
    String name();
    String format() default "toml";
    int version() default 1;
}
```

- `id` — owning mod ID / namespace. MUST be unique within a provider instance. Providers SHOULD reject collisions with `IllegalArgumentException`.
- `name` — base file name without extension.
- `format` — resolved via `Format.of(String)` — see [§4.3](#43-synthetic-format-construction).
- `version` — current schema version. Providers MUST compare this against the on-disk version to decide when migrations run.

### 7.2 `@Category`

Groups entries under a named section.

```java
@Target({FIELD, TYPE})
public @interface Category {
    String value();
}
```

- On a `TYPE`, sets the base category for every entry in the class.
- On a `FIELD`, places that entry under the named category, nested inside any type-level `@Category`.

Providers MUST support nested categories. Category names in the built path MUST NOT contain `.` (U+002E), because `.` is the segment separator in dotted paths.

### 7.3 `@Key`

Overrides the on-disk key for a field.

```java
@Target(FIELD)
public @interface Key {
    String value();
}
```

Providers MUST prefer `@Key(value)` over the reflected field name for both reads and writes.

### 7.4 `@Comment`

```java
@Target({FIELD, TYPE})
public @interface Comment {
    String[] value();
}
```

Each string is one comment line, in the order given. Providers MUST emit comments to disk iff `Format.supportsComments()` returns `true` for the config's `Format`. On formats without comment support, providers MUST silently drop them without error.

### 7.5 `@Hidden`

```java
@Target({FIELD, TYPE})
public @interface Hidden {}
```

Excludes an entry (or every entry under a `@Category` type) from GUI rendering. Providers implementing `Capability.GUI_DELEGATION` MUST filter `@Hidden`-marked entries out of any generated GUI. `@Hidden` MUST NOT affect on-disk persistence — hidden entries are still read from and written to disk.

### 7.6 `@Length`

```java
@Target(FIELD)
public @interface Length {
    int min() default 0;
    int max() default Integer.MAX_VALUE;
}
```

Constrains the length of `String`, `List`, or `Map` entries to `[min, max]` inclusive. Providers MUST reject values outside the range at validation time.

### 7.7 `@NotNull`

```java
@Target(FIELD)
public @interface NotNull {}
```

Rejects `null` values. Providers MUST treat a missing-on-disk value for an `@NotNull` field as invalid and MAY correct it to the field's declared default.

### 7.8 `@OneOf`

```java
@Target(FIELD)
public @interface OneOf {
    String[] value();
}
```

Restricts a `String` entry to an explicit allow-list. Providers SHOULD use `DROPDOWN` widgets for `@OneOf`-annotated entries when generating a GUI.

### 7.9 `@Pattern`

```java
@Target(FIELD)
public @interface Pattern {
    String value();
}
```

Requires a `String` entry to match a `java.util.regex.Pattern` fully (not partially). Providers MUST use `Matcher.matches()`, not `Matcher.find()`.

### 7.10 `@Range`

```java
@Target(FIELD)
public @interface Range {
    double min() default Double.NEGATIVE_INFINITY;
    double max() default Double.POSITIVE_INFINITY;
}
```

Constrains a numeric entry to `[min, max]` inclusive. Applies to `byte`, `short`, `int`, `long`, `float`, `double` and their boxed forms. Providers SHOULD use `SLIDER` widgets when both bounds are finite; providers MUST NOT throw if only one bound is set.

### 7.11 `@Reload`

```java
@Target({FIELD, TYPE})
public @interface Reload {
    Tier value() default Tier.WORLD;
    enum Tier { LIVE, WORLD, RESTART }
}
```

Declares when a changed value takes effect.

- On a `TYPE`, sets the default tier for every entry in that config.
- On a `FIELD`, overrides the type-level tier for that entry.

If unspecified, the effective tier is `Tier.WORLD`.

Tiers:

- `LIVE` — applied immediately on change. Intended for client / cosmetic values.
- `WORLD` — applied on the next world (re)load. The default.
- `RESTART` — applied only after a game restart.

Providers MUST NOT downgrade a `RESTART` tier to `LIVE` or `WORLD` silently. Providers MAY expose tier-aware reload behavior; if they do not, they MUST behave as if every tier were `WORLD`.

### 7.12 `@RequiresRestart`

```java
@Target(FIELD)
public @interface RequiresRestart {}
```

Convenience marker equivalent to `@Reload(Reload.Tier.RESTART)`. If both `@RequiresRestart` and `@Reload` are present on the same element, `@Reload` MUST win.

### 7.13 `@Sync`

```java
@Target({FIELD, TYPE})
public @interface Sync {
    Scope value() default Scope.CLIENT;
    enum Scope { CLIENT, SERVER, COMMON }
}
```

Declares the synchronization scope of an entry.

- On a `TYPE`, sets the default scope for every entry.
- On a `FIELD`, overrides the type-level scope.

If unspecified, the effective scope is `Scope.CLIENT`.

Scopes:

- `CLIENT` — client-owned, local, never synced.
- `SERVER` — server-authoritative. Pushed to clients for the session lifetime. Clients MUST NOT override it; server-scoped values MUST NOT be written to the client's disk.
- `COMMON` — server-authoritative when connected; usable from local defaults in singleplayer. Enforced like `SERVER` in multiplayer.

`@Sync` is present in `coal-api` as an intent hint even for providers that do not implement `Capability.SYNC`. Providers without sync MUST NOT emit runtime errors when they see a `SERVER` or `COMMON` scope — they SHOULD treat it as `CLIENT` and MAY log a one-time `INFO` note.

Providers implementing `Capability.SYNC` MUST honor scopes per [§16](#16-optional-module--synchronization-coal-api-sync).

### 7.14 `@Widget`

```java
@Target(FIELD)
public @interface Widget {
    Type value() default Type.AUTO;
    boolean allowInvalid() default false;

    enum Type { AUTO, TOGGLE, SLIDER, NUMBER_FIELD, TEXT_FIELD, DROPDOWN, COLOR }
}
```

Overrides the GUI control for an entry. Only meaningful for providers that implement `Capability.GUI_DELEGATION`.

Types:

- `AUTO` — inferred from data type and constraints (default).
- `TOGGLE` — boolean button.
- `SLIDER` — a bounded numeric slider. Requires bounded `@Range`.
- `NUMBER_FIELD` — free-form numeric input. RECOMMENDED to enforce `@Range` bounds when present; providers that cannot MAY substitute a slider.
- `TEXT_FIELD` — free-form text input.
- `DROPDOWN` — a discrete-choice list. Requires an enum field type OR `@OneOf` on a `String`.
- `COLOR` — color picker, backed by a `"#RRGGBB"` string.

**Type-plus-annotation prerequisites.** Each widget type has one or more prerequisites (field type, and — for some — a supporting annotation). Providers MUST apply the widget only when every prerequisite is met:

| Widget      | Prerequisite field type(s)                     | Prerequisite annotation |
|-------------|------------------------------------------------|-------------------------|
| `TOGGLE`    | `boolean` / `Boolean`                          | — |
| `SLIDER`    | numeric primitive or boxed wrapper             | bounded `@Range` |
| `NUMBER_FIELD` | numeric primitive or boxed wrapper          | — |
| `TEXT_FIELD` | `String`                                      | — |
| `DROPDOWN`  | enum field type OR `String`                    | none (enum) or `@OneOf` (String) |
| `COLOR`     | `String` in `"#RRGGBB"` form                   | — |

**Fallback rules.** When a `@Widget` hint's prerequisites are unmet, the provider MUST fall back to a compatible widget. The provider SHOULD choose the fallback closest to the hint's intent:

- `SLIDER` without `@Range` → `NUMBER_FIELD`.
- `NUMBER_FIELD` when the provider cannot render a bounded field for a `@Range`-annotated entry → `SLIDER`. Bounded-numeric-field support is RECOMMENDED but OPTIONAL; a provider without it MUST still enforce the bounds at commit time via the slider.
- `TOGGLE` on any non-boolean → the type-inferred default widget (i.e., the same widget the entry would have received under `AUTO`).
- `DROPDOWN` on a `String` without `@OneOf` → the type-inferred default (typically `TEXT_FIELD`).
- `DROPDOWN` on any type other than enum or `String` → the type-inferred default.
- `TEXT_FIELD` on any non-`String` → the type-inferred default.
- `COLOR` on any non-`String`, or a `String` the provider cannot decode as `"#RRGGBB"` → the type-inferred default (typically `TEXT_FIELD`), NOT `null`.

Providers MAY log at `INFO` on every fallback for diagnostic purposes. Providers MUST NOT throw at registration or render time solely because a `@Widget` hint's prerequisites were unmet — silent fallback is the required behavior.

**`AUTO` inference.** When `@Widget` is absent or set to `Type.AUTO`, providers MUST derive the widget from the entry's field type + supporting annotations. The recommended inference is: boolean → `TOGGLE`; numeric + bounded `@Range` → `SLIDER`; numeric without `@Range` → `NUMBER_FIELD`; enum → `DROPDOWN`; `String` + `@OneOf` → `DROPDOWN`; `String` without `@OneOf` → `TEXT_FIELD`. Providers MAY differ (e.g. choose `NUMBER_FIELD` even with `@Range`), but the fallback rules above still apply when an explicit hint is unmet.

`allowInvalid()`:

- `false` (default) — the GUI MUST NOT commit an entry that fails its declared constraints (`@Range`, `@Pattern`, decoded type). Providers MUST block save-and-exit with an in-GUI notification listing offending paths, and keep the underlying persisted value equal to the last-known-valid value.
- `true` — the GUI MAY commit invalid text; the persisted value MUST still be the last-known-valid decoded value. Load-time correction cleans up on next reload.

---

## 8. User API — programmatic schema (`ConfigSpec`)

`ConfigSpec` describes a config whose state is a `Map<String, Object>` rather than a POJO. Mod authors use it when the shape of their config is only known at runtime.

### 8.1 `ConfigSpec` class

```java
public final class ConfigSpec {
    public ConfigSpec(String id, String name, Format format, int version, List<EntrySpec> entries);

    public String getId();
    public String getName();
    public Format getFormat();
    public int getVersion();
    public List<EntrySpec> getEntries();
}
```

- The constructor MUST defensively copy `entries`.
- Getters MUST NOT return `null`.
- `getEntries()` MUST return an unmodifiable view.

### 8.2 `ConfigSpec.EntrySpec`

```java
public record EntrySpec(String key,
                        String categoryPath,
                        Class<?> type,
                        Object defaultValue,
                        EntryMetadata metadata) {}
```

- `key` — the leaf key of the entry. MUST NOT contain `.` (segment separator).
- `categoryPath` — dotted path of the containing category. Empty string means the root category. Providers reconstruct a `SchemaCategory` tree from the flat entry list at registration time.
- `type` — the declared type of the entry's value.
- `defaultValue` — the value used when the entry is absent on disk. MAY be `null` iff `metadata.isHidden()` is false and no `@NotNull` semantic applies (this is a call the mod author makes at construction time; providers MUST NOT assume non-null defaults).
- `metadata` — see [§8.4](#84-entrymetadata).

### 8.3 `ConfigSpec.Builder`

```java
public static final class Builder {
    public Builder(String id);

    public Builder name(String name);
    public Builder format(Format format);
    public Builder format(String formatId);
    public Builder version(int version);

    public <T> Builder entry(String key, Class<T> type, T defaultValue);
    public <T> Builder entry(String key, Class<T> type, T defaultValue,
                             Consumer<EntryMetadata.Builder> meta);

    public Builder category(String name, Consumer<Builder> category);

    public ConfigSpec build();
}
```

Semantics:

- Builder defaults: `format = Format.TOML`, `version = 1`.
- `format(String)` delegates to `Format.of(String)` — see [§4.3](#43-synthetic-format-construction).
- `entry(...)` overloads: the two-arg-metadata overload MUST invoke the caller's `Consumer<EntryMetadata.Builder>` with a fresh `EntryMetadata.Builder` populated from `EntryMetadata.builder()` defaults. The final `EntryMetadata` MUST be built and stored on the `EntrySpec`.
- `category(name, body)` opens a nested scope: every `entry(...)` call inside `body` MUST record its `categoryPath` as `<current-path>.<name>` (or just `name` at the root). Nested `category(...)` calls extend the path further. Entries added outside any `category(...)` block MUST record `categoryPath == ""`.
- `build()` MUST return a fresh `ConfigSpec`. Subsequent modifications to the `Builder` MUST NOT be observed by the already-built `ConfigSpec`.

### 8.4 `EntryMetadata`

Interface:

```java
List<String> comment();
Sync.Scope syncScope();
Reload.Tier reloadTier();
Widget.Type widget();
boolean isHidden();
List<Validator<?>> validators();
Optional<String> keyOverride();
```

`comment()`, `validators()` MUST return immutable views.

`EntryMetadata.builder()` returns a fresh mutable `Builder`, pre-populated with the following defaults:

| Method | Default |
|---|---|
| `comment` | empty list |
| `syncScope` | `Sync.Scope.CLIENT` |
| `reloadTier` | `Reload.Tier.WORLD` |
| `widget` | `Widget.Type.AUTO` |
| `hidden` | `false` |
| `validators` | empty list |
| `keyOverride` | empty (`Optional.empty()`) |

Builder interface:

```java
Builder comment(String... lines);
Builder syncScope(Sync.Scope scope);
Builder reloadTier(Reload.Tier tier);
Builder widget(Widget.Type type);
Builder hidden(boolean hidden);
Builder addValidator(Validator<?> validator);
Builder keyOverride(String key);

EntryMetadata build();  // via extends org.apache.commons.lang3.builder.Builder<EntryMetadata>
```

The default implementation returned by `Builder.build()` is `EntryMetadata.DefaultEntryMetadata`, a record:

```java
record DefaultEntryMetadata(List<String> comment,
                            Sync.Scope syncScope,
                            Reload.Tier reloadTier,
                            Widget.Type widget,
                            boolean isHidden,
                            List<Validator<?>> validators,
                            Optional<String> keyOverride)
        implements EntryMetadata;
```

The compact constructor MUST copy `comment` and `validators` via `List.copyOf(...)`. Callers MAY construct `DefaultEntryMetadata` directly for ad-hoc test doubles.

---

## 9. User API — migrations

### 9.1 `MigrationSpec`

```java
public interface MigrationSpec {
    MigrationSpec EMPTY = Collections::emptyList;
    static MigrationSpec empty();
    List<MigrationStep> steps();
}
```

- `MigrationSpec.empty()` MUST return the same singleton instance across calls. Zero allocations.
- Non-empty specs MUST return their steps in ascending `fromVersion` order. Providers MAY validate this on registration.

### 9.2 `MigrationStep`

```java
public interface MigrationStep {
    int fromVersion();
    int toVersion();
    List<MigrationOp> ops();
}
```

- `fromVersion` and `toVersion` MUST be positive integers.
- `toVersion() > fromVersion()`. Providers MAY reject non-monotonic steps.
- Steps SHOULD advance the version by one (`toVersion() == fromVersion() + 1`). Skipping is permitted but reduces migration composability.

### 9.3 `MigrationOp` — tree shape and path semantics

`MigrationOp` operates on the parsed config tree.

**Tree shape.** The tree is `Map<String, Object>`. Nested tables are also `Map<String, Object>`. Lists are `List<Object>`. Scalars are `String`, `Number`, `Boolean`, or `null`. Providers MUST NOT pass a tree containing any other Java type to `MigrationOp.apply(...)`. Migration authors MAY assume this shape.

**Path traversal.**

- **Write ops** (`setValue`, `setDefault`, `renameKey` destination) auto-create intermediate `Map<String, Object>` segments on demand. A previously-absent middle segment MUST be created as a fresh empty `Map<String, Object>` and inserted into its parent before the write proceeds.
- **Read ops** (`removeKey`, `transform`, `renameKey` source) treat missing intermediates as "absent" — no throw. The op simply no-ops on that path.

### 9.4 `MigrationOp` factories

The `MigrationOp` interface exposes five static factory methods. Their semantics MUST hold in every conforming implementation.

#### `renameKey(String from, String to)`

- Source absent → no-op.
- Source present, destination absent → move the value, auto-creating intermediates on the destination path. Remove the source entry.
- Source present, destination present → throw `IllegalStateException`. Rename would clobber existing data; migration authors want a loud failure here.

#### `removeKey(String path)`

- Absent → no-op.
- Present → remove the leaf entry. Parent maps MUST NOT be pruned even if left empty.

#### `setDefault(String path, Object value)`

- Absent → set `path` to `value`, auto-creating intermediates.
- Present, including a present-but-null value → no-op. Only "key entirely missing from its parent map" counts as absent.

#### `setValue(String path, Object value)`

- Always set `path` to `value`, overwriting any prior value at that path. Auto-creates intermediates.

#### `transform(String path, Function<Object, Object> fn)`

- Absent → no-op. `fn` MUST NOT be invoked.
- Present → replace with `fn.apply(current)`. A return value of `null` sets the entry to `null` (it does NOT remove the entry).

### 9.5 `MigrationRegistry`

```java
public interface MigrationRegistry {
    MigrationRegistry step(int fromVersion, int toVersion, MigrationOp... ops);
    MigrationSpec build();
}
```

A fluent alternative to constructing `MigrationSpec` explicitly.

### 9.6 `MigrationReport`

```java
public record MigrationReport(int fromVersion, int toVersion, List<AppliedStep> steps) {
    public record AppliedStep(int fromVersion, int toVersion, int opsApplied) {}
}
```

- `fromVersion` — the version parsed from disk before migration.
- `toVersion` — the version after migration completed. MUST equal the schema's declared version.
- `steps` — one entry per applied `MigrationStep`, in application order.

Providers MUST populate `MigrationReport` accurately; consumer mods MAY use it for diagnostics or to guard against unexpected migrations.

---

## 10. User API — events and reload

### 10.1 `EventBus` and `Registration`

```java
public interface EventBus {
    Registration subscribe(ChangeListener listener);
    Registration subscribe(String pathPrefix, ChangeListener listener);
    void dispatch(ChangeEvent event);

    interface Registration extends AutoCloseable {
        @Override void close();
    }
}
```

- `subscribe(listener)` registers a listener that MUST be invoked for every `ChangeEvent` on this bus.
- `subscribe(pathPrefix, listener)` registers a listener that MUST be invoked only for events whose `path` equals `pathPrefix` OR starts with `pathPrefix + "."`. Providers MUST use dotted-path semantics — subscribing to `"gui"` MUST match `"gui.foo"` but MUST NOT match `"guildhall"`.
- `dispatch(event)` is provider-internal. Consumer mods MUST NOT call it.
- `Registration.close()` MUST be idempotent and thread-safe. Closing a registration MUST cause its listener to stop receiving events. A closed registration re-closed MUST NOT throw.

### 10.2 `ChangeEvent`

```java
public record ChangeEvent(String path, Object oldValue, Object newValue, Origin origin, Instant at) {}
```

- `path` — the dotted path that changed.
- `oldValue`, `newValue` — the values before and after. Either MAY be `null`.
- `origin` — the origin of the *new* value.
- `at` — the instant at which the change was committed.

### 10.3 `ChangeListener`

Functional interface with `void onChange(ChangeEvent event)`. Listeners MUST NOT throw checked exceptions; providers MAY catch and log unchecked exceptions raised by listeners without propagating.

### 10.4 `ReloadListener<S>`

```java
@FunctionalInterface
public interface ReloadListener<S> {
    void onReload(S previous, S current);
}
```

Registered via `ConfigManager.addReloadListener(...)`. Fires when a load completes successfully.

- `previous` MAY be `null` if the load is the first for this manager.
- `current` MUST equal `manager.get()` at the moment of the call.
- Providers MUST invoke reload listeners AFTER dispatching any `ChangeEvent`s that resulted from the load.

Registered reload listeners MUST NOT be removable through the public API. Providers SHOULD document a provider-specific removal path if they support one.

---

## 11. Provider SPI — factory and provider

### 11.1 `ConfigProviderFactory`

```java
public interface ConfigProviderFactory {
    String name();
    int priority();
    String coalVersion();
    ConfigProvider create(Platform platform);
}
```

- `name()` MUST return a stable, human-recognizable identifier for the provider. Examples: `"coal-rubric"`, `"coal-yacl"`, `"coal-noop"`. MUST NOT be `null`.
- `priority()` — non-negative integer. Higher values win selection. `coal-noop` MUST return `0`. Real providers SHOULD choose a value greater than or equal to `100`.
- `coalVersion()` — the `coal-api` version the provider was compiled against. MUST follow SemVer-compatible format (`"MAJOR.MINOR.PATCH"`). Used by the runtime for version-drift checks per [§20](#20-versioning).
- `create(Platform)` — MUST return a fresh, fully-initialized `ConfigProvider`. MAY throw any exception; the runtime propagates it to the caller of `Coal.bootstrap()`.

The factory MUST be discoverable via `META-INF/services/com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory`.

### 11.2 `ConfigProvider`

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

- `name()` MUST return the same value as the factory's `name()`.
- `register(Class<S>, MigrationSpec)` — MUST:
  - Read the type's schema via `schemaReader()`.
  - Locate or create the on-disk file under `platform().configDir()`.
  - Run migrations, then load, then correct.
  - Return a `ConfigHandle<S>`.
  - Reject already-registered `id`s with `IllegalArgumentException`. Registration is per-`id`, not per-`Class`.
- `register(ConfigSpec, MigrationSpec)` — same, but for programmatic specs. Returned handle's state type is `Map<String, Object>`.
- `platform()` MUST return the same `Platform` instance the factory received in `create`.
- `schemaReader()` MAY throw `UnsupportedOperationException` if the provider does not expose a `SchemaReader`. Consumer mods SHOULD NOT call it directly.
- `corrector()` MAY throw `UnsupportedOperationException` under the same conditions.
- `defaultIO()` returns the `ConfigIO` used when a consumer does not supply one. MAY throw `UnsupportedOperationException` on providers that do not persist configs (e.g., `coal-noop`).
- `registeredConfigIds()` MUST return the set of currently-registered config ids. Providers MAY return an unmodifiable view or a snapshot.
- `getById(id)` returns the handle for `id`, or `Optional.empty()` if no such config is registered.
- `supports(Capability)` — see [§11.3](#113-capability-enum).

### 11.3 `Capability` enum

```java
public enum Capability {
    SYNC, MIGRATION, FILE_WATCH, VALIDATION, GUI_DELEGATION, JSON5, CUSTOM_FORMATS
}
```

Per-capability semantics:

- `SYNC` — provider implements `coal-api-sync`. See [§16](#16-optional-module--synchronization-coal-api-sync).
- `MIGRATION` — provider executes `MigrationSpec` steps on load. A provider MUST advertise this iff its `register(...)` methods apply `MigrationOp`s according to [§9](#9-user-api--migrations).
- `FILE_WATCH` — provider reloads the config when the backing file changes on disk. Advertising this REQUIRES `ConfigIO.fileWatchService()` to return non-empty for the provider's default IO.
- `VALIDATION` — provider runs validators declared via `@Range`, `@Pattern`, custom `Validator`s, etc., and applies corrections.
- `GUI_DELEGATION` — provider implements screen-provider selection via `coal-api-gui`. See [§17](#17-optional-module--gui-delegation-coal-api-gui-).
- `JSON5` — provider handles `Format.JSON5`. Providers not advertising this SHOULD fall back to plain JSON on `JSON5` requests with a `WARN` log, per [§4.4](#44-custom-formats-and-capabilitycustom_formats).
- `CUSTOM_FORMATS` — provider honors `Format` instances with ids outside `{toml, json, json5}`. See [§4.4](#44-custom-formats-and-capabilitycustom_formats).

A provider's `supports(Capability)` MUST return the same value across the process lifetime for the same capability. This is a stable declaration, not a runtime feature check.

### 11.4 `ProviderNotFoundException`

```java
public class ProviderNotFoundException extends RuntimeException {
    public ProviderNotFoundException(String message);
}
```

Unchecked. Thrown from `Coal.bootstrap()` when either `Platform` or `ConfigProviderFactory` discovery yields an invalid result. See [§3.2](#32-platform-discovery), [§3.3](#33-provider-factory-discovery).

### 11.5 The `coal-noop` provider

`coal-noop` is a normative example implementation. Every conforming COAL runtime MUST ship with `coal-noop` on the classpath via JiJ bundling per [§3.8](#38-the-last-resort-provider-coal-noop).

`coal-noop`'s public behavior:

- `NoopProvider.name()` returns `Coal.NOOP_PROVIDER_NAME`.
- `NoopProviderFactory.priority()` returns `0`.
- `NoopProviderFactory.coalVersion()` returns the value that matches the `coal-api` version it was compiled against.
- `register(Class<S>, MigrationSpec)` — instantiates a fresh `S` via a public no-argument constructor. On absence, MUST throw `IllegalArgumentException` with the message form `"coal-noop cannot instantiate <fully-qualified-name> — needs a public no-arg constructor"`, wrapping the underlying `ReflectiveOperationException`.
- `register(ConfigSpec, MigrationSpec)` — returns a handle over a `LinkedHashMap<String, Object>` populated with `entry.categoryPath + "." + entry.key` (or just `entry.key` when `categoryPath` is empty) mapped to `entry.defaultValue()`.
- `schemaReader()`, `corrector()`, `defaultIO()` — MUST throw `UnsupportedOperationException` with a message identifying the missing capability.
- `registeredConfigIds()` MUST return `Collections.emptySet()`.
- `getById(id)` MUST return `Optional.empty()`.
- `supports(Capability)` MUST return `false` for every capability.

Nested `NoopHandle<S>` behavior:

- `get()` returns the constructed instance.
- `set(...)`, `reload()`, `save()` are no-ops.
- `manager()` returns a no-op `ConfigManager` whose `schema()` throws `UnsupportedOperationException`, whose `load()` returns `new LoadResult(snapshot, emptyList(), Optional.empty())`, whose `file()` throws `UnsupportedOperationException`, whose `originOf(...)` returns `Origin.DEFAULT`, and whose `events()` returns a no-op `EventBus`.
- `snapshot()` returns a `ConfigSnapshot` whose `capturedAt()` returns `Instant.EPOCH`, whose `get(...)` returns `Optional.empty()`, whose `getRaw(...)` returns `null`, and whose `paths()` returns `Collections.emptySet()`.
- `value(...)` returns a `ConfigValue` whose `get()` returns `null` and whose `set/onChange` are no-ops.

Consumer mods that build against `coal-noop` alone MUST assume every side effect is discarded. `coal-noop` is a graceful-degradation last resort, not a functional provider.

---

## 12. Provider SPI — schema reading

The schema layer is what a provider builds when it registers a config. It is provider-facing but visible to consumer mods that walk it for reflection-like use cases.

### 12.1 `Schema`

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

- `type()` — the annotated Java class, or `Map.class` for `ConfigSpec`-derived schemas.
- `id()`, `name()`, `format()`, `version()` — mirror the annotation or `ConfigSpec` fields.
- `root()` — the root category.
- `find(dottedPath)` — locate an entry by its full dotted path. Returns `Optional.empty()` for unknown paths.
- `paths()` — every dotted path that has an entry.

The `Schema` instance for a given registration MUST NOT change over the manager's lifetime. Reloading the file MUST NOT produce a new `Schema` — the schema is a static description of the *type*, not of the *values*.

### 12.2 `SchemaCategory`

```java
public interface SchemaCategory {
    String name();
    List<String> comment();
    List<SchemaEntry> entries();
    List<SchemaCategory> categories();
    Optional<SchemaCategory> category(String name);
    Optional<SchemaEntry> entry(String key);
}
```

- `name()` — the category's leaf name. Empty string means the root category.
- `comment()` — comment lines attached to the category via `@Comment` at the class level or `Category`-adjacent metadata.
- `entries()` — entries directly under this category (not descendants).
- `categories()` — sub-categories.
- `category(name)`, `entry(key)` — local lookups by name/key.

### 12.3 `SchemaEntry`

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

- `key()` — the leaf key. Reflects `@Key` overrides where applicable.
- `type()` — see [§12.4](#124-valuetype-and-valuetypekind).
- `defaultValue()` — the declared default. MAY be `null`.
- `metadata()` — see [§8.4](#84-entrymetadata).
- `accessor()` — see [§12.5](#125-valueaccessor).
- `readFrom(instance)`, `writeTo(instance, value)` — convenience delegators to `accessor()`. Providers MAY implement them directly against the underlying reflection/codegen path.

### 12.4 `ValueType` and `ValueType.Kind`

```java
public interface ValueType {
    Kind kind();
    Class<?> rawType();
    Optional<ValueType> elementType();
    Optional<ValueType> valueType();
    List<SchemaEntry> children();

    enum Kind { SCALAR, ENUM, LIST, MAP, OBJECT }
}
```

- `kind()` — describes the shape.
- `rawType()` — the erased Java type.
- `elementType()` — present iff `kind == LIST`, the element type.
- `valueType()` — present iff `kind == MAP`, the value type. Key type is always `String` in COAL — providers MUST reject non-string map keys at registration.
- `children()` — present (non-empty) iff `kind == OBJECT`; the nested entries.

### 12.5 `ValueAccessor`

```java
public interface ValueAccessor {
    Object read(Object instance);
    void write(Object instance, Object value);
    Class<?> declaredType();
}
```

- `read(instance)` — extract the entry's current value from `instance`.
- `write(instance, value)` — set the entry's value on `instance`. MUST perform any provider-side coercion needed to match `declaredType()`; if coercion is impossible, MUST throw `ClassCastException`.
- `declaredType()` — the field or programmatic type this accessor was built against.

### 12.6 `SchemaReader`

```java
public interface SchemaReader {
    <S> ConfigModel<S> read(Class<S> type);
    ConfigModel<Map<String, Object>> read(ConfigSpec spec);
}
```

Builds a `ConfigModel` (schema + state factory) from either an annotated class or a `ConfigSpec`. Called once per registration, at registration time. Providers MAY cache results across identical input classes.

### 12.7 `ConfigModel`

```java
public interface ConfigModel<S> {
    Schema schema();
    S newState();
}
```

- `schema()` — the built schema.
- `newState()` — construct a fresh state instance populated with declared defaults. Called once at load time, again on reload if the provider replaces the state instance rather than mutating in place.

### 12.8 `EntryMetadata` from the provider side

Providers building `SchemaEntry` from an annotated class MUST populate `EntryMetadata` from the annotations declared in [§7](#7-user-api--the-annotation-schema-dsl). The mapping is:

| Source | `EntryMetadata` field |
|---|---|
| `@Comment(...)` on field | `comment()` |
| `@Sync(scope)` on field, or on the type for defaults | `syncScope()` |
| `@Reload(tier)` or `@RequiresRestart` | `reloadTier()` (see [§7.12](#712-requiresrestart)) |
| `@Widget(type)` | `widget()` |
| `@Hidden` | `isHidden()` |
| Constraint annotations (`@Range`, `@Pattern`, `@OneOf`, `@Length`, `@NotNull`) | one or more `Validator`s pushed into `validators()` |
| `@Key(value)` | `keyOverride()` |

Providers MUST synthesize `Validator` instances for the constraint annotations and add them to `validators()` at read time — consumer mods MUST NOT be required to author validators for the standard constraints.

---

## 13. Provider SPI — validation and correction

### 13.1 `Validator` and `ValidationContext`

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

- `validate(value, ctx)` MUST return either `ValidationResult.ok()` or `ValidationResult.invalid(...)`.
- `ctx.entry()` — the `SchemaEntry` being validated.
- `ctx.path()` — the dotted path of that entry.

Validators MUST NOT mutate any external state. Providers MAY invoke validators from any thread, including concurrently.

### 13.2 `ValidationResult`

```java
public sealed interface ValidationResult permits Ok, Invalid {
    static Ok ok();
    static Invalid invalid(List<ValidationIssue> issues);
    static Invalid invalid(String message, Object suggestion);

    boolean isOk();
    boolean isInvalid();
    List<ValidationIssue> issues();

    final class Ok implements ValidationResult { public Ok(); }
    record Invalid(List<ValidationIssue> issues) implements ValidationResult;
}
```

- `Ok` — validation succeeded. `issues()` MUST return `Collections.emptyList()`.
- `Invalid` — validation failed. `issues()` MUST return a non-empty list.
- `invalid(issues)` — the compact constructor MUST throw `IllegalArgumentException` when `issues.isEmpty()`. `Invalid` MUST copy `issues` via `List.copyOf(...)`.
- `invalid(message, suggestion)` — convenience wrapper that constructs `Invalid` with a single `ValidationIssue(message, Optional.of(suggestion))`.

### 13.3 `ValidationIssue`

```java
public record ValidationIssue(String message, Optional<Object> suggestion) {}
```

- `message` — a human-readable description of the failure. Providers displaying failures in a GUI SHOULD render this verbatim.
- `suggestion` — an optional replacement value the corrector MAY apply.

### 13.4 `Corrector`

```java
@FunctionalInterface
public interface Corrector {
    List<Correction> correct(Schema schema, Object instance);
}
```

Providers implementing `Capability.VALIDATION` MUST invoke a `Corrector` after each load. The `Corrector` walks the loaded instance, runs every applicable validator, and returns a `Correction` for every entry it changed. Providers MUST apply the corrections to `instance` before the load returns.

### 13.5 `Correction`

```java
public record Correction(String path, Object before, Object after, String reason) {}
```

- `path` — dotted path of the corrected entry.
- `before` — the value prior to correction.
- `after` — the value after correction.
- `reason` — human-readable explanation. Providers SHOULD prefer copying the `ValidationIssue.message` that motivated the correction.

Corrections applied during load MUST appear in `LoadResult.corrections`.

---

## 14. Provider SPI — IO

### 14.1 `ConfigIO`

```java
public interface ConfigIO {
    Optional<Map<String, Object>> read(Path file, Schema schema) throws IOException;
    void write(Path file, Map<String, Object> tree, Schema schema) throws IOException;
    boolean supports(Format format);
    Optional<BackupStrategy> backupStrategy();
    Optional<FileWatchService> fileWatchService();
}
```

- `read(file, schema)` — parse `file` into a tree. Returns `Optional.empty()` if the file does not exist; `Optional.of(tree)` on success. Throws `IOException` on read failure, `SerializationException` on parse failure.
- `write(file, tree, schema)` — render `tree` to `file`, respecting `schema.format()` for format selection. Providers SHOULD create parent directories if missing. If `backupStrategy()` is present, providers MUST invoke it prior to overwriting an existing `file`.
- `supports(format)` — declares which `Format` instances this IO can round-trip. MUST return `false` for any `Format` the IO would truncate or corrupt.
- `backupStrategy()` — optional. When present, providers MUST invoke `backupStrategy.backup(file)` before every destructive write.
- `fileWatchService()` — optional. When present, the provider MAY use it to auto-reload on external file changes. A provider MUST NOT advertise `Capability.FILE_WATCH` when its default IO's `fileWatchService()` is empty.

### 14.2 `FormatAdapter`

```java
public interface FormatAdapter {
    Format format();
    Map<String, Object> parse(byte[] bytes) throws SerializationException;
    byte[] render(Map<String, Object> tree, Schema schema) throws SerializationException;
    boolean supportsComments();
}
```

A format-scoped codec. `ConfigIO` implementations typically compose one `FormatAdapter` per supported format.

- `format()` — the `Format` this adapter handles.
- `parse(bytes)` — deserialize.
- `render(tree, schema)` — serialize. The `schema` argument lets the adapter emit comments, entry ordering, or format-specific decorations. Adapters that don't need it MAY ignore it.
- `supportsComments()` — declares whether comments survive a round trip. MUST equal `format().supportsComments()`.

### 14.3 `BackupStrategy`

```java
public interface BackupStrategy {
    Path backup(Path file) throws IOException;
    void prune(Path dir, String baseName, int retention) throws IOException;
}
```

- `backup(file)` — create a backup of `file`. Return the backup file's `Path`. Providers MAY use timestamped filenames or numbered rotations; this spec does not require a naming convention.
- `prune(dir, baseName, retention)` — remove old backups so that at most `retention` backups remain for the given `baseName` in `dir`. Called at the discretion of the provider.

### 14.4 `FileWatchService`

```java
public interface FileWatchService extends AutoCloseable {
    Registration watch(Path file, Runnable onChange);
    @Override void close();

    interface Registration extends AutoCloseable {
        @Override void close();
    }
}
```

- `watch(file, onChange)` — subscribe to changes on `file`. `onChange` is invoked whenever the watch service detects a change. Providers implementing this MUST debounce closely-spaced events to avoid reload storms; the debounce policy is provider-specific.
- `close()` on the service — MUST release every underlying OS resource (e.g., `WatchService` handles). MUST be idempotent.
- `Registration.close()` — MUST unsubscribe. MUST be idempotent and thread-safe.

`Runnable.onChange` MAY be invoked on any thread. Consumer mods that touch Minecraft state from `onChange` MUST route through `Platform.mainThreadExecutor()`.

### 14.5 `SerializationException`

```java
public class SerializationException extends RuntimeException {
    public SerializationException(String message);
    public SerializationException(String message, Throwable cause);
}
```

Unchecked. Thrown by `FormatAdapter` implementations on parse/render failure. `ConfigIO.read` MAY propagate it directly or wrap the underlying cause.

---

## 15. Platform contract

`com.oliveryasuna.mc.coal.api.platform.Platform` is the loader-integration boundary.

### 15.1 `Platform`

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

Semantics:

- `configDir()` — the loader-appropriate config directory. Providers MUST resolve relative config paths against this directory.
  - On Fabric: `FabricLoader.getInstance().getConfigDir()`.
  - On NeoForge: `FMLPaths.CONFIGDIR.get()`.
- `mainThreadExecutor()` — an `Executor` that runs its `Runnable` on the game main thread.
  - On the client dist: the executor MUST target `Minecraft.getInstance()`.
  - On the server dist: the executor MUST target the current `MinecraftServer` when one is running. Between server sessions, the executor MAY drop tasks or queue them; consumer code SHOULD guard around server-availability windows.
  - Same-thread submission MUST run the task immediately without deadlock risk. Providers MAY implement this by checking `Minecraft.isSameThread()` (Fabric) or `server.isSameThread()` (NeoForge).
- `logger(name)` — an SLF4J `Logger` with the given name. Providers MUST NOT rely on any particular logger backend.
- `environment()` — `Environment.CLIENT` or `Environment.SERVER`. Reflects the physical dist, not the logical side.
- `gameDir()` — the game root directory. MAY be `Optional.empty()` for headless test environments.
- `loaderName()` — a short identifier like `"fabric"` or `"neoforge"`. MAY be `Optional.empty()` for test platforms.
- `loaderVersion()` — the loader version string. MAY be `Optional.empty()` for the same reason.

Loader integrations MUST expose `Platform` via `META-INF/services/com.oliveryasuna.mc.coal.api.platform.Platform`. See [§3.2](#32-platform-discovery).

### 15.2 `Environment`

```java
public enum Environment { CLIENT, SERVER }
```

Reflects the physical dist. On a dedicated server, `environment() == SERVER`. On a Minecraft client (including one hosting an integrated server), `environment() == CLIENT`.

### 15.3 `PermissionGate`

```java
@FunctionalInterface
public interface PermissionGate {
    PermissionGate DENY_ALL = (actor, level) -> false;
    PermissionGate ALLOW_ALL = (actor, level) -> true;

    boolean canEdit(Object actor, int requiredLevel);
}
```

Consulted by `SyncService` to decide whether an inbound `ClientEdit` from a specific player should be accepted server-side.

- `actor` — the loader-specific representation of the caller. Typically a `Player` instance.
- `requiredLevel` — the permission level demanded by the edit's target scope. Interpretation is provider-specific.

The two provided singletons (`DENY_ALL`, `ALLOW_ALL`) are for tests and for providers that want to short-circuit permission entirely.

---

## 16. Optional module — synchronization (`coal-api-sync`)

`coal-api-sync` is an optional module. A provider implementing `Capability.SYNC` MUST depend on it AND honor every requirement in this section. A provider that does not implement sync MUST NOT depend on `coal-api-sync`.

Package: `com.oliveryasuna.mc.coal.api.sync.*`.

### 16.1 Advertising the capability

A `ConfigProvider` MUST return `true` from `supports(Capability.SYNC)` iff:

1. It ships a `SyncService` implementation.
2. It provides a `NetworkTransport` that MUST work on the loader it targets.
3. It honors `Sync.Scope` per [§7.13](#713-sync).

### 16.2 `ProtocolVersion`

```java
public record ProtocolVersion(int major, int minor) implements Comparable<ProtocolVersion> {
    public static final ProtocolVersion CURRENT = new ProtocolVersion(1, 0);

    public boolean isCompatibleWith(ProtocolVersion other);
    public int compareTo(ProtocolVersion o);
}
```

- `CURRENT` — the protocol version this `coal-api-sync` build targets.
- `isCompatibleWith(other)` — MUST return `true` iff `this.major == other.major`. Different-major peers are incompatible. Different-minor peers are compatible; feature-negotiation across a minor gap is the payload codec's responsibility, not the handshake's.
- `isCompatibleWith(null)` MUST return `false`.
- `compareTo` orders by `major`, then `minor`, ascending.

Providers negotiating with a peer MUST perform the compatibility check against `this`, not against `ProtocolVersion.CURRENT`, so forks may hold their own protocol version.

### 16.3 `SyncPayload`

```java
public sealed interface SyncPayload permits Handshake, Snapshot, Delta, ClientEdit {}

record Handshake(ProtocolVersion protocol, Set<String> knownConfigIds) implements SyncPayload;
record Snapshot(String configId, Map<String, Object> values) implements SyncPayload;
record Delta(String configId, Map<String, Object> changed) implements SyncPayload;
record ClientEdit(String configId, Map<String, Object> entries) implements SyncPayload;
```

Semantics:

- **`Handshake`** — first message. Announces protocol version and the set of config ids the sender knows about. The receiver MUST NOT push `Snapshot`s or `Delta`s for config ids the peer didn't announce.
- **`Snapshot`** — full authoritative state for `configId`. Sent server → client on join and on server-side rewrite (e.g., a moderator edit).
- **`Delta`** — incremental change. `changed` maps dotted paths to their new values. Sent server → client on every server-side edit that touches at least one server-scoped path.
- **`ClientEdit`** — a client's request to change one or more entries. Sent client → server. The server MUST validate scope and permissions before applying.

### 16.4 `PayloadCodec`

```java
public interface PayloadCodec {
    byte[] encode(SyncPayload payload);
    SyncPayload decode(byte[] bytes) throws WireFormatException;
}
```

Providers MAY choose any binary format for the payload. The codec MUST round-trip: `decode(encode(p))` MUST equal `p` for every `SyncPayload` a peer might send. `WireFormatException` is thrown on decode failure — malformed length prefixes, unknown payload discriminators, decoding errors on any field.

### 16.5 `NetworkTransport`

```java
public interface NetworkTransport {
    void sendToServer(byte[] payload);
    void sendToClient(Object clientHandle, byte[] payload);
    void sentToAllClients(byte[] payload);
    void subscribe(InboundHandler handler);
    void close();

    @FunctionalInterface
    interface InboundHandler {
        void onPayload(Object source, byte[] payload);
    }
}
```

> **Note:** the method spelling in `coal-api-sync` 0.1.x is `sentToAllClients`, retained here for source parity. A future minor MAY introduce a rename with the old spelling deprecated.

Semantics:

- `sendToServer(payload)` — client-side. MUST send `payload` to the currently-connected server, or throw `IllegalStateException` if not connected.
- `sendToClient(clientHandle, payload)` — server-side. `clientHandle` is the loader-specific representation of a specific player. MUST NOT dispatch to any other client.
- `sentToAllClients(payload)` — server-side broadcast. MUST dispatch to every currently-connected player.
- `subscribe(handler)` — register an inbound handler. `handler.onPayload(source, bytes)` runs on the network thread and MUST NOT block. Consumer code MUST route Minecraft-state changes through `Platform.mainThreadExecutor()`.
- `close()` — MUST release the transport. Subsequent send calls MAY throw `IllegalStateException`.

### 16.6 `SyncService`

```java
public interface SyncService {
    enum Role { SERVER, CLIENT }

    Role role();
    ProtocolVersion protocol();
    void register(ConfigManager<?> manager);
    void unregister(ConfigManager<?> manager);
    void start();
    void close();
    void onClientConnected(Object clientHandle);
    void broadcastDelta(String configId, List<String> changedPaths);
    void broadcastSnapshot(String configId);
    void sendClientEdit(String configId, Map<String, Object> entries);
}
```

- `role()` — declares whether this instance is running on the server or the client dist.
- `protocol()` — the local `ProtocolVersion` this service will announce during handshake.
- `register(manager)` — enroll a `ConfigManager` for sync. The service MUST use `ScopeEnforcer` to filter what actually crosses the wire.
- `unregister(manager)` — inverse.
- `start()` — begin listening and, on the server, be ready to send Handshakes when clients connect.
- `close()` — stop the service; release the `NetworkTransport`. MUST be idempotent.
- `onClientConnected(clientHandle)` — server-side hook, wired by the loader integration. MUST send a `Handshake` followed by a `Snapshot` for every registered manager that has server-authoritative content.
- `broadcastDelta(configId, changedPaths)` — server-side. MUST emit a `Delta` payload to every currently-connected client whose `Handshake` included `configId`.
- `broadcastSnapshot(configId)` — server-side. MUST emit a full `Snapshot` payload. Used after mass edits.
- `sendClientEdit(configId, entries)` — client-side. MUST enqueue a `ClientEdit` to the server for the entries at the given paths.

### 16.7 `ScopeEnforcer`

```java
public interface ScopeEnforcer {
    Map<String, Object> extractAuthoritative(ConfigManager<?> manager);
    List<String> applyAuthoritative(ConfigManager<?> manager, Map<String, Object> values);
}
```

- `extractAuthoritative(manager)` — server-side. Returns the subset of `manager.get()` that is server-authoritative (`Sync.Scope.SERVER` and `Sync.Scope.COMMON`), keyed by dotted path.
- `applyAuthoritative(manager, values)` — client-side. Applies `values` to `manager`, updating each affected path's `Origin` to `Origin.FROM_REMOTE`. Returns the list of dotted paths that actually changed.

Providers MUST NOT persist server-authoritative values to the client's disk. `applyAuthoritative` MUST leave the client's config file untouched for `SERVER`-scoped paths.

### 16.8 `InboundValidator`

```java
@FunctionalInterface
public interface InboundValidator {
    boolean accept(String configId, Map<String, Object> values);
}
```

Server-side. Filters an inbound `ClientEdit` payload. Providers MUST invoke this validator before applying any edit. When `accept` returns `false`, the edit MUST be discarded silently. The provider MAY log at `WARN`.

### 16.9 `WireFormatException`

```java
public class WireFormatException extends RuntimeException {
    public WireFormatException(String message);
    public WireFormatException(String message, Throwable cause);
}
```

Thrown by `PayloadCodec.decode(...)` on malformed input.

### 16.10 Server → client flow

The normative server → client synchronization flow is:

1. Server-side `SyncService.start()` is called. The service subscribes to its `NetworkTransport` and to each registered `ConfigManager.events()`.
2. When a player connects, the loader integration calls `syncService.onClientConnected(handle)`. The service sends a `Handshake` naming its `protocol()` and `registeredConfigIds()`, followed by a `Snapshot` for each registered manager.
3. Whenever a `SERVER`- or `COMMON`-scoped path changes locally on the server, the service emits a `Delta` naming the changed paths.
4. The client-side service applies incoming `Snapshot`s and `Delta`s via `ScopeEnforcer.applyAuthoritative`, marking paths with `Origin.FROM_REMOTE`.

### 16.11 Client → server flow

1. On the client, when the user edits a `SERVER`- or `COMMON`-scoped entry through a GUI or the mod's programmatic API, the mod calls `SyncService.sendClientEdit(configId, entries)`.
2. The client-side service encodes and dispatches the `ClientEdit`.
3. The server-side transport receives the `ClientEdit` on its inbound handler.
4. The server-side service validates via `InboundValidator.accept(...)`. If accepted, the service consults `PermissionGate.canEdit(...)`.
5. If the gate accepts, the service applies the edit to the target `ConfigManager` and broadcasts a `Delta` reflecting the applied changes.
6. If either check fails, the service MUST discard the edit. It MAY notify the originating client via a provider-defined channel; this spec does not require a specific rejection payload.

---

## 17. Optional module — GUI delegation (`coal-api-gui-*`)

Package: `com.oliveryasuna.mc.coal.api.gui.*`.

### 17.1 Two-variant story

The GUI module ships as **two loader-specific artifacts**: `coal-api-gui-fabric` and `coal-api-gui-neoforge`. The two variants MUST expose the same public source but compile against loader-specific Minecraft mappings — Loom-remapped intermediary bytecode for Fabric, Mojmap bytecode for NeoForge. Consumers MUST NOT depend on both.

A single compiled jar cannot be shared across the two loaders. Providers implementing `Capability.GUI_DELEGATION` MUST ship two variants of their own GUI adapter (`coal-<provider>-gui-fabric`, `coal-<provider>-gui-neoforge`).

### 17.2 `ScreenProvider`

```java
public interface ScreenProvider {
    String id();
    int priority();
    Screen create(Minecraft client, Screen parent, ConfigManager<?> manager);
}
```

- `id()` — a short identifier such as `"yacl"`, `"cloth"`. Providers MAY use this for logging and for cross-provider discrimination.
- `priority()` — higher wins. See [§17.4](#174-selection-and-fallback-semantics).
- `create(client, parent, manager)` — construct and return the settings `Screen` for the given manager, or return `null` if this provider refuses to render `manager`.

`ScreenProvider` implementations MUST NOT persist state. They are called on demand from the render thread.

### 17.3 `GuiRegistry`

`com.oliveryasuna.mc.coal.api.gui.GuiRegistry` is a final, non-instantiable class.

```java
public static void registerProvider(ScreenProvider provider);
public static Optional<ScreenProvider> selected();
public static Screen open(Minecraft client, Screen parent, ConfigManager<?> manager);
```

Semantics:

- Storage is a `java.util.concurrent.CopyOnWriteArrayList<ScreenProvider>`. Registration MAY happen from any thread; iteration is safe concurrently.
- `registerProvider(provider)` — enroll `provider`. `null` argument MUST throw `NullPointerException`.
- `selected()` — return the highest-priority registered provider, or `Optional.empty()` if none. MUST NOT trigger any side effect.
- `open(client, parent, manager)` — invoke `create` on the highest-priority registered provider. If that provider returns `null`, walk the remaining providers in descending-priority order until one returns non-null. If every provider returns `null`, throw `IllegalStateException`. If no provider has been registered, throw `IllegalStateException`.

`registerProvider` MUST NOT reject duplicate priorities. Providers with equal priorities are iterated in the order they were registered.

### 17.4 Selection and fallback semantics

Selection is priority-based. There is no per-call frontend override. A mod calling `GuiRegistry.open(...)` MUST NOT be able to force one provider over another; user preference resolution is handled entirely by providers, typically by re-registering their `ScreenProvider` with a priority chosen from a provider-side config.

Providers implementing `Capability.GUI_DELEGATION` MUST register their `ScreenProvider`(s) at client-init.

---

## 18. Threading model

### 18.1 The main-thread contract

Consumer mods MUST route any Minecraft-state mutation through `Platform.mainThreadExecutor()`. Every callback COAL invokes from a background thread — `ChangeListener`, `ReloadListener`, `FileWatchService.Registration`, `NetworkTransport.InboundHandler` — is documented as such and MUST be assumed to run off-main by default.

### 18.2 What runs on the main thread

- `Coal.bootstrap()` called from mod-init runs on the main thread on both loaders.
- `Coal.register(...)` inherits the thread of its caller; providers MUST tolerate registration from any thread. In practice mod authors call it from mod-init on the main thread.
- `ConfigHandle.get()` and `snapshot()` MAY be called from any thread.

### 18.3 What runs off the main thread

- `NetworkTransport.InboundHandler.onPayload(...)` — network thread.
- `FileWatchService.Registration` callbacks — OS-watch thread.
- Provider-internal reload debouncers — provider-specific thread pool.

### 18.4 EventBus dispatch

`EventBus.dispatch(...)` MUST invoke listeners synchronously on the thread that called `dispatch`. Providers changing a config in response to a network payload MUST NOT re-enter the main thread from the network thread; they MUST route through `Platform.mainThreadExecutor()` first.

Listeners MUST NOT block. Providers MAY log a `WARN` if a listener call takes longer than a provider-defined threshold; this spec does not require timing enforcement.

---

## 19. Error handling

### 19.1 Which exceptions escape which entry point

| Entry point | Documented exceptions |
|---|---|
| `Coal.bootstrap()` | `ProviderNotFoundException`, whatever `ConfigProviderFactory.create` throws |
| `Coal.bootstrap(ConfigProvider)` | `NullPointerException` on null arg |
| `Coal.register(Class<S>, ...)` | Bootstrap exceptions (if auto-triggering); `IllegalArgumentException` on missing `@Config` or already-registered `id`; provider-specific IO exceptions during initial load |
| `Coal.register(ConfigSpec, ...)` | Bootstrap exceptions; `IllegalArgumentException` on already-registered `id`; provider-specific IO exceptions |
| `ConfigHandle.reload()` | `IOException` |
| `ConfigHandle.save()` | `IOException` |
| `ConfigHandle.value(path, type).get()` | `ClassCastException` on type mismatch |
| `ConfigManager.load()` | `IOException` |
| `MigrationOp.renameKey(...).apply(...)` | `IllegalStateException` on destination collision |
| `FormatAdapter.parse(...)`, `.render(...)` | `SerializationException` |
| `PayloadCodec.decode(...)` | `WireFormatException` |
| `Registration.close()` | MUST NOT throw |

### 19.2 What providers MUST NOT do

- MUST NOT throw checked exceptions from methods that don't declare them.
- MUST NOT throw from `Registration.close()` under any circumstance.
- MUST NOT throw from `Origin`, `Environment`, `Format` singletons.
- MUST NOT throw `Error` subclasses in response to configuration issues. Reserve those for genuine JVM problems.

### 19.3 Logging surface

Every log line COAL emits at `INFO` or higher MUST include enough information to identify the affected provider and config id. Providers SHOULD prefix with the provider name (e.g., `[coal-rubric]`) — this spec does not require a specific format.

---

## 20. Versioning

### 20.1 Spec and API versioning

The **spec** (this document) is versioned independently from the **`coal-api`** artifact. The correspondence is:

- The revision of this spec matches the `coal-api` version listed at the top of this file. Every backwards-incompatible spec change MUST bump the `MAJOR` in `coal-api`'s version and MUST NOT ship without a spec revision.
- Backwards-compatible additions bump `MINOR`. Providers advertising `MINOR = N` conform to spec revision `MAJOR.N.*`.
- Bug fixes bump `PATCH`. Providers MUST NOT distinguish `PATCH`-level changes.

### 20.2 `coalVersion()` reporting

Every `ConfigProviderFactory` MUST return a version string from `coalVersion()` that matches the `coal-api` artifact it was compiled against. The string MUST follow the format `"MAJOR.MINOR.PATCH"`.

### 20.3 Compat rules

The COAL runtime MAY compare `factory.coalVersion()` against the `coal-api` version at runtime and:

- Reject a provider whose `MAJOR` is greater than the runtime's `MAJOR` (the runtime is older than the provider expects). Rejection SHOULD be a `WARN` + skip.
- Warn on a provider whose `MAJOR` is smaller than the runtime's `MAJOR` (the provider is older). Runtime SHOULD still install the provider but MUST log a `WARN`.
- Ignore `MINOR` and `PATCH` drift.

A future revision of this spec MAY tighten these rules.

### 20.4 Wire-protocol versioning

`ProtocolVersion.CURRENT` is versioned separately from `coal-api`. See [§16.2](#162-protocolversion).

---

## 21. Conformance

### 21.1 Baseline conformance

A provider claims **baseline conformance** by implementing every method in [§11.2](#112-configprovider) with the semantics documented in this spec. Baseline conformance requires no capabilities.

Baseline requirements:

- [§3](#3-discovery-bootstrap-and-lifecycle) — discovery obligations for a provider's `META-INF/services` entry.
- [§11.1](#111-configproviderfactory) and [§11.2](#112-configprovider) — factory and provider contract.
- [§6](#6-user-api--handles-managers-snapshots) — handle, manager, snapshot semantics.
- [§7](#7-user-api--the-annotation-schema-dsl) — annotation semantics for annotation-driven configs (or `IllegalArgumentException` at registration if the provider does not support annotation-driven configs; providers MAY be spec-only for programmatic configs, though this is atypical).
- [§8](#8-user-api--programmatic-schema-configspec) — `ConfigSpec` semantics.
- [§9](#9-user-api--migrations) — migration semantics IF `Capability.MIGRATION` is advertised. Otherwise, the provider MAY skip migration on load without error.
- [§10](#10-user-api--events-and-reload) — event and reload contract.
- [§12](#12-provider-spi--schema-reading) — schema-layer contract. Providers building against annotations MUST populate `EntryMetadata` per [§12.8](#128-entrymetadata-from-the-provider-side).
- [§13](#13-provider-spi--validation-and-correction) — validation semantics IF `Capability.VALIDATION` is advertised.
- [§14](#14-provider-spi--io) — IO contract.
- [§18](#18-threading-model), [§19](#19-error-handling) — threading and error handling.

### 21.2 Capability-conditional requirements

Advertising a `Capability` obligates the provider to honor its section-specific requirements:

| Capability | Section |
|---|---|
| `SYNC` | [§16](#16-optional-module--synchronization-coal-api-sync) |
| `MIGRATION` | [§9](#9-user-api--migrations) |
| `FILE_WATCH` | [§14.4](#144-filewatchservice) |
| `VALIDATION` | [§13](#13-provider-spi--validation-and-correction) |
| `GUI_DELEGATION` | [§17](#17-optional-module--gui-delegation-coal-api-gui-) |
| `JSON5` | [§4](#4-the-format-subsystem) |
| `CUSTOM_FORMATS` | [§4.4](#44-custom-formats-and-capabilitycustom_formats) |

### 21.3 The testkit

`coal-testkit` (planned; not shipped in `0.1.x`) is the mechanical conformance suite. Providers run it and MAY publish a JSON `ConformanceReport` containing:

- Testkit version.
- Per-capability result: `PASSED`, `PARTIAL`, `FAILED`, `NOT_ADVERTISED`.
- Timestamp.

The report is informational. The spec is authoritative.

---

## Appendix A — Rationale

### R.3 (§3) — Discovery model

**Why ServiceLoader for both `Platform` and `ConfigProviderFactory`?** SLF4J's model is proven: a stable API jar, one implementation on the classpath, discovery via `META-INF/services`. Using ServiceLoader for `Platform` too (rather than "loader calls `Coal.setPlatform(...)` explicitly at mod-init") removes cross-mod ordering concerns — if the loader integration is on the classpath, its Platform is discoverable.

**Why exactly one `Platform`?** Multi-Platform ambiguity is a configuration bug, not a runtime negotiation problem. A user with both `coal-fabric` and `coal-neoforge` on the classpath (say, from a modpack error) has a real problem to fix; the runtime should tell them.

**Why "first-wins" bootstrap (Q-B1)?** Same reasons SLF4J's `LoggerFactory` is first-wins: a bootstrap re-entered accidentally shouldn't destroy the state of already-registered configs. The `WARN` provides the breadcrumb.

**Why auto-bootstrap on `register` (Q-B3)?** Matches SLF4J's `LoggerFactory.getLogger`: mod authors who never explicitly call `bootstrap()` still work. Cost: the discovery log line appears when the first `register` runs rather than at mod-init, which is slightly harder to correlate. Acceptable.

**Why unconditional `bootstrap(ConfigProvider)` (Q-B4)?** Tests need it. A test that runs after another test can't tolerate a "provider already installed" throw. The INFO log makes the swap traceable.

**Why bundle `coal-noop` inside the `coal` mod (Q-B2 replacement)?** Original Q-B2 recommended an inline fallback provider in `coal-api` itself, so `Coal` could survive an empty classpath. Once the mod-packaging story matured, that inline fallback became unreachable in production (`coal-noop` is always JiJ-bundled) and the inline provider became dead code. Deleted in favor of the always-bundled `coal-noop`.

### R.4 (§4) — Format subsystem (Q-A)

**Why `Format` as an open set (Q-A2)?** A closed `Format.Registry` in `coal-api` would either be a mutable global (SLF4J-shaped but a "smell") or a per-provider registry (defensible but loses the "same `Format` instance across providers" story). The open-set choice with `Format.of(...)` returning a synthetic for unknown ids sidesteps both. Providers ignore ids they don't support and advertise `Capability.CUSTOM_FORMATS` if they do.

**Why case-insensitive built-in lookup (Q-A3)?** `Format.of("TOML") == Format.TOML` removes the "`@Config(format = \"toml\")` vs `@Config(format = \"Toml\")`" footgun.

**Why nested `record SimpleFormat` (Q-A1)?** One less top-level file, equality naturally scoped to `id()`. If `SimpleFormat` grows fields it can be extracted to a top-level; the outer type-name won't change.

### R.5 (§5) — `Coal` entry point

**Why static-only?** SLF4J's `LoggerFactory` pattern. A single provider is installed process-wide; every `Coal.register(...)` call routes to it. Static access is cheap and requires no wiring. Consumer mods that want dependency injection can wrap `Coal` in their own facade.

**Why `isNoopProvider()` in the API rather than a general "isProvider(String)" check?** The noop-fallback story is central to the loader-integration UX (chat message, title-screen toast). Making it a single-purpose accessor is honest — mod authors rarely need to distinguish other providers by name.

### R.7 (§7) — Annotation semantics

**Why `@Sync` in `coal-api` even for providers without sync (Q-A2 in the sync design doc)?** Consumer mods declare intent once with the annotation. Providers without sync see the annotation and no-op it; providers with sync honor it. If `@Sync` lived in `coal-api-sync`, mod authors would have to conditionally depend on the sync module just to declare intent.

**Why `@Reload.Tier.WORLD` as the default?** Client-only mods rarely change values that matter mid-tick; server-side configs typically want a world reload to re-apply. `LIVE` is the exception (cosmetic values), not the rule.

**Why `@RequiresRestart` as sugar for `@Reload(Tier.RESTART)`?** Discoverability. `@RequiresRestart` reads clearly next to a field; users don't have to know that "restart" is a `Reload.Tier` value.

**Why `@Widget` fallbacks are silent, not fail-loud?** A mod author writing `@Widget(SLIDER)` without `@Range` is expressing intent, not a hard requirement. Failing registration would force every consumer of a stricter provider to conditionally omit hints — friction with no user-visible benefit. Fallback + optional INFO log preserves the intent while keeping the GUI functional. The set of prerequisites was derived from actual v1 provider testing (see the `coal-yacl-adapter` testmod's `widgetHints` category), not from theoretical enumeration — every rule reflects a real widget-vs-type collision observed in practice.

**Why does `NUMBER_FIELD` fall back to `SLIDER` (rather than plain unbounded field) when the provider can't bound a field?** Bounded numeric input is a real requirement — the mod author wrote `@Range` for a reason. A slider always enforces its bounds. An unbounded numeric field breaks the bound silently. Fall-back to slider preserves the correctness invariant even if the provider loses the "field" affordance.

### R.9 (§9) — Migration semantics (Q-C)

**Why auto-create intermediates on write ops (Q-C1)?** Migrations authoring a new key deep in the tree shouldn't have to enumerate every intermediate segment. Read ops don't get the same treatment because their semantics ("act on an existing entry") diverge on absence.

**Why `renameKey` throws on collision (Q-C2)?** Silent overwrite is data loss. A rename that would clobber existing data is a migration-author bug worth surfacing at runtime with a stack trace.

**Why `setDefault` treats present-but-null as "present" (Q-C2)?** `null` is a valid explicit value in JSON. A migration that manifests a default over a deliberate `null` is a surprise.

**Why `removeKey` does NOT prune empty parents (Q-C2)?** Empty parent maps can be observable elsewhere (e.g., a `@Category` group that's now empty but the file still has the section). Auto-pruning has surprising downstream effects; explicit `removeKey` on the parent is safer.

**Why `transform(null)` is no-op (Q-C2)?** Matches `setDefault` philosophy: don't manifest keys that weren't there. If a migration author needs to seed a key based on the "absent" case, they use `setDefault` first, then `transform`.

### R.10 (§10) — Events, reload, `Registration.close()` (Q-E1)

**Why `Registration.close()` MUST be idempotent + thread-safe?** try-with-resources at the caller layer expects `close()` to be safe to double-call. AtomicBoolean-style flip is a one-line implementation cost. Not requiring it forces every caller to track "have I closed this already" — worse.

### R.11 (§11) — Provider SPI

**Why is `schemaReader()` allowed to throw `UnsupportedOperationException`?** `coal-noop` genuinely does not have a `SchemaReader`. Making it a required method with a mandatory throw preserves the API shape for real providers while letting the last-resort provider degrade gracefully.

**Why `Capability.CUSTOM_FORMATS`?** A conforming provider MAY handle three formats and nothing else. Advertising "I actually round-trip arbitrary formats" needs to be explicit so mod authors can distinguish `format = "hocon"` (surprise) from `format = "hocon"` (works).

### R.14 (§14) — IO contract

**Why `Optional<Map<String, Object>>` return from `ConfigIO.read`?** `Optional.empty()` distinguishes "file does not exist" from "file is empty" (an empty file returns `Optional.of(emptyMap())`). Providers previously conflated these; the API forces the distinction.

**Why `FileWatchService.close()` on both the service and each `Registration`?** OS file-watch resources are scarce. Explicit close on both surfaces makes ownership unambiguous.

### R.16 (§16) — Sync

**Why same-major = compatible for `ProtocolVersion`?** Major changes to `SyncPayload`'s sealed hierarchy or the wire format break decodability; different-minor just means one peer knows features the other doesn't. Feature negotiation across a minor gap is a `PayloadCodec` concern (each `SyncPayload` variant should degrade gracefully).

**Why does `compareTo` compare against `this` and not `CURRENT`?** Tests and forks may want to hold their own protocol version somewhere other than the compile-time `CURRENT`. Comparing against `this` preserves that flexibility without adding another parameter.

**Why sealed `SyncPayload`?** The runtime dispatch is on the variant type; sealed forces exhaustive handling in `PayloadCodec` and the service. A future minor adding a new variant is a compile-error at every switch — deliberate.

**Why is `sentToAllClients` misspelled?** It isn't misspelled deliberately — the original signature slipped through. A future minor rename with the old spelling deprecated is planned. Documented here so implementors don't cargo-cult the typo.

### R.17 (§17) — GUI delegation

**Why two variants (fabric + neoforge) with mirrored source?** A single Mojmap-compiled jar cannot be bundled into a Fabric prod runtime — Loom needs to remap references to intermediary, and it only does that for mod jars it builds. Two variants are the only correct answer until the mapping ecosystem changes.

**Why no `FrontendHint` per-call override (from the original design questions)?** A mod passing `YACL` per-call would override the user's preference. That's the wrong direction of control. Provider-side selection with user-preference-driven priority is the right shape.

### R.20 (§20) — Versioning

**Why is `coalVersion()` a string and not a `record`?** Version parsing is a `coal-tools` concern (Q-tools), not a `coal-api` concern. Keeping it a string minimizes the API surface.

---

## Appendix B — Glossary

| Term | Definition |
|---|---|
| **Baseline conformance** | See [§21.1](#211-baseline-conformance). |
| **Bootstrap** | The one-time discovery-and-installation sequence that binds a `Platform` and a `ConfigProvider` to `Coal`. |
| **Capability** | A `Capability` enum value the provider advertises support for. See [§11.3](#113-capability-enum). |
| **COAL runtime** | The static entry point (`Coal`) plus the installed provider. |
| **Config identity** | The `id()` string that uniquely identifies a config within a provider instance. |
| **Consumer mod** | A mod that depends on `coal-api` and calls `Coal.register(...)`. |
| **Dotted path** | `"a.b.c"`-style addressing scheme for nested trees. See [§2.4](#24-dotted-paths). |
| **Entry** | A single named setting. Provider-facing type: `SchemaEntry`. |
| **Format** | The on-disk representation of a config file. Open set. See [§4](#4-the-format-subsystem). |
| **JiJ** | "Jar-in-Jar". Loader mechanism for bundling library jars inside a mod jar. `coal-fabric` and `coal-neoforge` use JiJ to bundle `coal-api`, `coal-noop`, etc. |
| **Last-resort provider** | `coal-noop`. Always present. Priority `0`. |
| **Loader integration** | A Minecraft mod that ships a `Platform`. In this repo: `coal-fabric`, `coal-neoforge`, both shipping as a mod named `coal`. |
| **Origin** | Where a value came from. See [§6.6](#66-origin). |
| **Provider** | An implementation of `ConfigProvider`. |
| **Reload tier** | See [§7.11](#711-reload). |
| **Schema** | The static description of a config type. Distinct from the values it holds. See [§12.1](#121-schema). |
| **Snapshot** | An immutable view of a config's values at an instant. See [§6.4](#64-configsnapshot). |
| **Sync scope** | See [§7.13](#713-sync). |
| **Tree** | `Map<String, Object>` shape used during migration. See [§2.2](#22-terminology). |
| **Validator** | See [§13.1](#131-validator-and-validationcontext). |

---

## Appendix C — Reserved for future revisions

This appendix is intentionally left as a placeholder for known-future changes that MAY appear in a subsequent spec revision. It is not normative.

Known candidates for a `MINOR` revision:

- **`NetworkTransport.sendToAllClients`** — rename with `sentToAllClients` deprecated. See [R.16](#r16-16--sync).
- **Testkit `ConformanceReport` schema** — the JSON layout is not yet specified. See [§21.3](#213-the-testkit).
- **Provider-declared runtime shutdown** — the current spec has no shutdown API ([§3.9](#39-shutdown)). A future minor MAY introduce one, provided the migration path is backward-compatible.
- **Provider-scoped `Format` registry** — Q-A2's rejected option 2 (per-provider registries) is deferred, not permanently ruled out. A second real provider with strong "canonicalize custom formats" semantics could motivate it.
- **`ConfigHandle.snapshot(Instant)`** — a historical-snapshot accessor. Not planned but not ruled out.
- **`SyncPayload.Rejection`** — a new sealed variant for server → client rejection notifications, currently deferred to provider-defined channels ([§16.11](#1611-client--server-flow)).

Known candidates for a `MAJOR` revision:

- **Sealed `Value`** — replacing raw `Object` in `Map<String, Object>` with a sealed hierarchy (`Value.Scalar`, `Value.List`, `Value.Table`, `Value.Null`). Motivation: type-safe migration authorship. Cost: every consumer touches the API surface.
- **`Platform.side()` returning a logical (not physical) side** — currently `Environment` reflects the physical dist. A logical accessor would help mods that care about integrated servers, but this requires reworking the loader-integration contract.

The list is illustrative, not exhaustive. Reserved future work MUST be re-decided at spec-revision time; nothing in this appendix confers any obligation on providers today.
