---
title: Define a config
---

# Define a config

Two ways to declare a config schema:

1. **Annotated POJO** — the common case. Reflection produces the schema.
2. **`ConfigSpec` builder** — for dynamically-shaped configs where you don't have a compile-time type.

Both routes land in the same place: `Coal.register(...)` returns a `ConfigHandle<S>` and the provider handles the rest.

## The POJO route

Annotate a class with [`@Config`](../reference/annotations#config) and give it public fields. Field types drive widget choice; constraint annotations tighten validation.

```java
package com.example.mymod;

import com.oliveryasuna.mc.coal.api.annotation.*;

@Config(id = "mymod", name = "mymod", format = "json", version = 1)
public final class MyConfig {

    // Root category
    // -----------------

    @Comment("Master toggle for the feature.")
    public boolean enabled = true;

    @Comment("How many things per tick.")
    @Range(min = 1, max = 100)
    public int thingsPerTick = 10;

    // Categories tie into GUI structure
    // -----------------

    @Category("graphics")
    @Comment("Client-side rendering options.")
    @Reload(Reload.Tier.LIVE)
    public String particleColor = "#ff8800";

    @Category("graphics")
    @Widget(Widget.Type.SLIDER)
    @Range(min = 0.0, max = 1.0)
    public double brightness = 0.7;

    // Server-authoritative
    // -----------------

    @Category("gameplay")
    @Sync(Sync.Scope.SERVER)
    @OneOf({"easy", "normal", "hard"})
    public String difficulty = "normal";
}
```

Register once at mod init:

```java
public final class MyModMain implements ModInitializer {
    private static volatile ConfigHandle<MyConfig> HANDLE;

    public static ConfigHandle<MyConfig> handle() { return HANDLE; }

    @Override
    public void onInitialize() {
        HANDLE = Coal.register(MyConfig.class);
    }
}
```

### Field rules

- **Public, non-final, non-static, non-transient.** Everything else is ignored.
- **Every field must have a sensible default.** Providers use the field's declared value as the fallback when a persisted value fails validation, and to materialize the config file on first launch.
- **Types drive widget picking**:

| Java type                  | Default widget                                         |
|----------------------------|--------------------------------------------------------|
| `boolean`                  | Toggle                                                 |
| `byte` / `short` / `int` / `long` / `float` / `double` (+ boxed) | Number field; slider if `@Range` present |
| `String`                   | Text field; dropdown if `@OneOf`; color if `@Widget(COLOR)` |
| `enum`                     | Dropdown                                               |
| `List<T>`                  | List editor (adapter-dependent — see coverage matrices) |
| `Map<K, V>`                | Currently placeholder in first-party adapters          |
| Nested POJO (non-annotated `public Foo foo`) | Recurses into a `foo.*` sub-category      |

Full annotation vocabulary in the [annotation reference](../reference/annotations).

### The `@Config` fields

- **`id`** — the owning mod / namespace. Used internally to disambiguate registered configs; convention is your mod id.
- **`name`** — the on-disk **base name** without extension. `<config-dir>/<name>.<ext>`.
- **`format`** — a hint. `"toml"` (default), `"json"`, `"json5"`, or a custom id. Providers advertising the format honor it; providers that don't fall back to their supported default with a `WARN`.
- **`version`** — starts at `1`. Bump when the schema changes, and provide a `MigrationSpec` — see [Migrate configs](./migrating-configs).

### Categories

`@Category("name")` on a field puts it under that section in the schema tree.

`@Category("name")` on the **type** prefixes every entry's category — useful for grouping a whole nested POJO under one section.

Nested POJOs (fields whose type is a non-annotated `public` class with its own fields) become sub-categories automatically. So:

```java
@Config(id = "mymod", name = "mymod")
public final class MyConfig {
    public GraphicsOptions graphics = new GraphicsOptions();
}

public final class GraphicsOptions {
    @Range(min = 0.0, max = 1.0)
    public double brightness = 0.7;
    public String skyColor = "#4488ff";
}
```

...produces the same schema as writing every field on `MyConfig` under `@Category("graphics")` — one implicit sub-category per nested POJO.

### Key overrides

Use `@Key("old_name")` when the field name in Java differs from the on-disk key. Common for renames that would otherwise need a migration:

```java
@Key("thingsPerTick")   // old on-disk key
public int operationsPerTick = 10;  // new Java field name
```

### Comments

`@Comment({"line 1", "line 2"})` becomes tooltip text in the GUI and (for `Format` values whose `supportsComments()` returns `true` — TOML and JSON5) a comment above the entry on disk.

## The `ConfigSpec` route

For configs whose shape is only known at runtime — dynamic entries loaded from a manifest, a scripting layer, etc. — build a `ConfigSpec` with the fluent builder:

```java
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.Format;

ConfigSpec spec = new ConfigSpec.Builder("mymod")
        .name("mymod_dynamic")
        .format(Format.JSON)
        .version(1)
        .entry("enabled", Boolean.class, true)
        .category("graphics", g -> g
                .entry("brightness", Double.class, 0.7, meta -> meta
                        .comment("Client brightness [0.0, 1.0].")
                        .widget(Widget.Type.SLIDER)))
        .entry("mode", String.class, "normal", meta -> meta
                .comment("Difficulty tier."))
        .build();

ConfigHandle<Map<String, Object>> handle = Coal.register(spec);
```

`ConfigSpec.Builder#category(name, consumer)` nests — a child inside a parent resolves to `"parent.child"`.

The returned handle is typed as `ConfigHandle<Map<String, Object>>` — values live in a map keyed by dotted path. Use `handle.value("graphics.brightness", Double.class)` for a typed accessor on a specific entry.

Providers translate a `ConfigSpec` into the same `Schema` shape they'd build from a POJO — everything downstream (validators, migrations, GUI rendering) is identical.

## Reading and writing

```java
// Whole POJO
MyConfig cfg = MyModMain.handle().get();
if (cfg.enabled) { ... }

// Typed accessor
ConfigValue<Integer> tickRate = MyModMain.handle().value("thingsPerTick", Integer.class);
int rate = tickRate.get();
tickRate.set(25);
tickRate.onChange(newRate -> LOGGER.info("Tick rate: {}", newRate));

// Direct set on the handle (untyped)
MyModMain.handle().set("thingsPerTick", 25);

// Persist
MyModMain.handle().save();
```

Reads (`get()`, `value(...).get()`) are lock-free. `set(...)` fires a `ChangeEvent` on the manager's `EventBus`. `save()` writes the current state to disk; it isn't auto-triggered by `set(...)`.

For a point-in-time snapshot with all values:

```java
ConfigSnapshot snap = MyModMain.handle().snapshot();
Optional<Double> brightness = snap.get("graphics.brightness", Double.class);
Instant when = snap.capturedAt();
```

## What happens on first launch

If `<config-dir>/<name>.<ext>` doesn't exist:

1. Provider materializes the config file from the schema's declared defaults.
2. `Coal.register(...)` returns a handle with values matching those defaults.
3. Subsequent runs read the file, apply migrations if `version` changed, apply corrections, and materialize the state.

Users get a hand-editable file even if they never open the settings screen.

## Related

- [Annotation reference](../reference/annotations) — every annotation in one table.
- [Handle load corrections](./handling-load-corrections) — what happens when a persisted value fails a constraint.
- [Migrate configs](./migrating-configs) — how to evolve a schema without breaking user files.
- [Specification §7, §8](../spec/#7-user-api--the-annotation-schema-dsl) — normative rules.
