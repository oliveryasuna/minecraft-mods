---
title: Annotation reference
---

# Annotation reference

Every annotation in `com.oliveryasuna.mc.coal.api.annotation`. Normative semantics live in [spec §7](../spec/#7-user-api--the-annotation-schema-dsl); this page is the quick lookup.

## Type-level

### `@Config`

`@Target(TYPE)`. Marks a class as a config root.

```java
@Config(id = "mymod", name = "mymod", format = "json", version = 1)
```

| Property   | Type   | Default   | Purpose                                                          |
|------------|--------|-----------|------------------------------------------------------------------|
| `id`       | String | required  | Owning mod / namespace. Used internally to key registrations.    |
| `name`     | String | required  | On-disk base file name, no extension.                            |
| `format`   | String | `"toml"`  | Format id — `"toml"`, `"json"`, `"json5"`, or a custom id.       |
| `version`  | int    | `1`       | Schema version for migrations.                                   |

### `@Category` (on type)

`@Target(TYPE, FIELD)`. Prefixes every entry in the type with the given section name.

```java
@Category("graphics")
public final class GraphicsOptions { /* fields */ }
```

### `@Comment` (on type)

`@Target(TYPE, FIELD)`. Attaches human-readable comment text — tooltip in the GUI, comment above the entry on disk (for `Format`s where `supportsComments()` is `true`).

```java
@Comment({"First line.", "Second line."})
```

## Field-level metadata

### `@Category` (on field)

Puts an entry under a named section.

```java
@Category("graphics")
public double brightness = 0.7;
```

### `@Key`

`@Target(FIELD)`. Overrides the on-disk key for a field. Useful for renaming a Java field without breaking existing config files.

```java
@Key("thingsPerTick")
public int operationsPerTick = 10;
```

### `@Comment` (on field)

See above.

### `@Hidden`

`@Target(TYPE, FIELD)`. Excludes the entry (or every entry in a category) from the generated GUI. Still persisted; still readable via the handle. On a type, hides every entry.

```java
@Hidden
public String internalToken = "";
```

## Field-level constraints

Constraints run at load-time via the corrector when the provider advertises [`Capability.VALIDATION`](../concepts/capabilities). Providers without validation read the metadata but don't apply corrections. See [Handle load corrections](../guides/handling-load-corrections).

### `@Range`

`@Target(FIELD)`. Numeric bounds, inclusive `[min, max]`. Applies to `byte`/`short`/`int`/`long`/`float`/`double` and their boxed forms.

```java
@Range(min = 0, max = 100)
public int percentage = 50;

@Range(min = 0.0, max = 1.0)
public double brightness = 0.7;
```

Defaults: `min = -Infinity`, `max = +Infinity`.

### `@OneOf`

`@Target(FIELD)`. Restricts a `String` entry to an explicit allow-list.

```java
@OneOf({"easy", "normal", "hard"})
public String difficulty = "normal";
```

### `@Pattern`

`@Target(FIELD)`. Requires a `String` entry to fully match a `java.util.regex.Pattern`.

```java
@Pattern("#[0-9a-fA-F]{6}")
public String particleColor = "#ff8800";
```

### `@Length`

`@Target(FIELD)`. Constrains the length of a `String` or the size of a `List` / `Map` to an inclusive `[min, max]` range.

```java
@Length(min = 3, max = 32)
public String username = "player";

@Length(max = 100)
public List<String> tags = new ArrayList<>();
```

Defaults: `min = 0`, `max = Integer.MAX_VALUE`.

### `@NotNull`

`@Target(FIELD)`. Rejects a missing/null persisted value. Corrects to the field's declared default.

```java
@NotNull
public String requiredField = "default-value";
```

## Field-level behavior

### `@Reload`

`@Target(TYPE, FIELD)`. Declares when a changed value takes effect.

```java
@Reload(Reload.Tier.LIVE)
public String particleColor = "#ff8800";

@Reload(Reload.Tier.RESTART)
public boolean experimentalFeature = false;
```

`Reload.Tier` values:

| Tier      | Applied                                  | Typical use                     |
|-----------|------------------------------------------|---------------------------------|
| `LIVE`    | Immediately on change                    | Client cosmetic.                |
| `WORLD`   | On next world (re)load. **Default.**     | Server-side game rule.          |
| `RESTART` | Only after a full game restart           | Mod loading / JVM options.      |

On a **type**, sets the default tier for every entry.

### `@RequiresRestart`

`@Target(FIELD)`. Convenience marker equivalent to `@Reload(Reload.Tier.RESTART)`. If both are present on the same field, `@Reload` wins.

```java
@RequiresRestart
public boolean experimentalFeature = false;
```

### `@Sync`

`@Target(TYPE, FIELD)`. Declares the synchronization scope of an entry. Only meaningful when the provider advertises [`Capability.SYNC`](../concepts/capabilities); otherwise read but not enforced.

```java
@Sync(Sync.Scope.SERVER)
@OneOf({"easy", "normal", "hard"})
public String difficulty = "normal";
```

`Sync.Scope` values:

| Scope     | Ownership                                                 |
|-----------|-----------------------------------------------------------|
| `CLIENT`  | Client-owned, local, never synced. **Default.**           |
| `SERVER`  | Server-authoritative. Pushed to clients; not client-disk. |
| `COMMON`  | Server-authoritative when connected; local in singleplayer. |

On a **type**, sets the default scope for every entry.

## GUI hints

### `@Widget`

`@Target(FIELD)`. Overrides the default widget picked by the type-inference rules.

```java
@Widget(Widget.Type.COLOR)
public String particleColor = "#ff8800";

@Widget(value = Widget.Type.SLIDER, allowInvalid = true)
@Range(min = 0.0, max = 1.0)
public double brightness = 0.7;
```

| Property       | Type          | Default          | Purpose                                                                 |
|----------------|---------------|------------------|-------------------------------------------------------------------------|
| `value`        | `Widget.Type` | `AUTO`           | Desired control.                                                        |
| `allowInvalid` | `boolean`     | `false`          | Whether the GUI may commit invalid edits on save (see below).           |

`Widget.Type` values:

| Type           | Renders as                                                                     |
|----------------|--------------------------------------------------------------------------------|
| `AUTO`         | Inferred from type + other constraints. **Default.**                           |
| `TOGGLE`       | Boolean on/off button.                                                         |
| `SLIDER`       | Slider — requires a bounded `@Range`.                                          |
| `NUMBER_FIELD` | Free-form numeric input.                                                       |
| `TEXT_FIELD`   | Free-form text input.                                                          |
| `DROPDOWN`     | Dropdown — used for enums and `@OneOf`.                                        |
| `COLOR`        | Color picker — backed by a `#RRGGBB` string.                                   |

`allowInvalid = false` (the default): the GUI's "Save & exit" is blocked when the widget is in an invalid state (e.g. a text field that fails `@Pattern`, or a codec that can't decode). A confirm screen lists the offending paths; the screen stays open until the user fixes them.

`allowInvalid = true`: the user may save over invalid input. The underlying field keeps its last valid value; load-time correction cleans up on next reload.

Widget fallback rules — when the requested widget's prerequisites aren't met (e.g., `SLIDER` on an unbounded numeric) — are per [spec §7.14](../spec/#714-widget). Providers fall back silently to the type-inferred default, optionally with an INFO log.

## Absent-annotation defaults

Missing annotations translate to:

- `@Sync` absent → `Sync.Scope.CLIENT` — local, never synced.
- `@Reload` absent → `Reload.Tier.WORLD` — applied on next world load.
- `@Widget` absent → `Widget.Type.AUTO` — inferred from type.
- `@Comment` absent → empty comment list.
- No constraints → the entry accepts any value of its Java type.

## Related

- [Define a config](../guides/defining-a-config) — annotations in context.
- [Handle load corrections](../guides/handling-load-corrections) — what happens when constraints fail.
- [Specification §7](../spec/#7-user-api--the-annotation-schema-dsl) — normative rules for every annotation.
