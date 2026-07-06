---
title: Migrate configs across versions
---

# Migrate configs across versions

When you change a config's schema — rename a key, remove one, change a type — existing users have persisted config files at the old shape. Migrations transform old files into the new shape at load time so users don't lose their settings.

Requires a provider that advertises [`Capability.MIGRATION`](../concepts/capabilities). Both first-party adapters do.

## The idea

Every `@Config` has a `version` (integer, default `1`). Persisted files store their version at save time. On load, the provider compares the on-disk version to `@Config#version`:

- **Same** — no migration needed, load as-is.
- **On-disk lower** — walk the `MigrationSpec` steps from disk-version → schema-version. Each step transforms the parsed tree.
- **On-disk higher** — provider-defined. First-party adapters currently load as-is and let validation clean up unknown fields.

Migrations run **before** validation. The corrector sees the migrated tree, then corrects anything that still doesn't fit the schema.

## Registering migrations

`Coal.register(Class, MigrationSpec)` (or the `ConfigSpec` variant) takes a spec. Build one with `MigrationRegistry`:

```java
import com.oliveryasuna.mc.coal.api.migration.*;
import static com.oliveryasuna.mc.coal.api.migration.MigrationOp.*;

MigrationSpec migrations = // ...built via a registry the provider exposes,
                          // OR construct directly:
MigrationSpec migrations = () -> List.of(
        new MigrationStep() {
            @Override public int fromVersion() { return 1; }
            @Override public int toVersion()   { return 2; }
            @Override public List<MigrationOp> ops() {
                return List.of(
                        renameKey("particleColor", "graphics.particleColor"),
                        setDefault("graphics.brightness", 0.7),
                        removeKey("deprecatedFlag")
                );
            }
        }
);

HANDLE = Coal.register(MyConfig.class, migrations);
```

`MigrationSpec` is functional (`List<MigrationStep> steps()`), so anonymous implementations or lambdas both work. `MigrationSpec.empty()` for the no-migrations case (the default when you call `Coal.register(Class)`).

## The tree shape

Migration ops mutate a **parsed tree** — the on-disk format, decoded into a nested map:

- Nested tables → `Map<String, Object>`
- Lists → `List<Object>`
- Scalars → `String`, `Number`, `Boolean`, or `null`

Dotted paths address entries. Write ops auto-create intermediate map segments; read ops treat missing intermediates as absent (no throw). See [`MigrationOp` javadoc](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api/src/main/java/com/oliveryasuna/mc/coal/api/migration/MigrationOp.java) for the exact contract.

## The built-in ops

Static factories on [`MigrationOp`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api/src/main/java/com/oliveryasuna/mc/coal/api/migration/MigrationOp.java):

### `renameKey(from, to)`

Moves the value at `from` to `to`. Both are dotted paths.

- Source absent → no-op.
- Source present, destination absent → move.
- Source present, destination present → throws `IllegalStateException` (clobber refused).

### `removeKey(path)`

Removes the leaf at `path`. Absent → no-op. Parent maps are **not pruned** even if left empty (a later step can prune explicitly by removing the parent path).

### `setDefault(path, value)`

Set `path` to `value` only when the leaf key is entirely absent from its parent map. A present-but-null value counts as present and is left untouched. Auto-creates intermediate segments.

Use this when you added a new field with a non-empty default and want to backfill existing files.

### `setValue(path, value)`

Set `path` to `value`, overwriting any existing entry. Auto-creates intermediate segments.

### `transform(path, fn)`

Apply `fn` to the current value at `path` and store the result.

- Path absent → no-op, `fn` is not invoked.
- Path present → `parent.put(key, fn.apply(current))`. A returned `null` sets the value to `null`; it does **not** remove the entry.

Useful for value coercions:

```java
transform("gameplay.difficulty", value -> {
    if (value instanceof Number n) {
        // Old int-based enum encoding → new string.
        return switch (n.intValue()) {
            case 0 -> "easy";
            case 1 -> "normal";
            case 2 -> "hard";
            default -> "normal";
        };
    }
    return value;
})
```

## Ordering

Steps run in ascending `fromVersion` order:

1. Load persisted file, extract on-disk version `V`.
2. For each step in `MigrationSpec` where `fromVersion >= V`, sorted ascending, run the step.
3. Apply resulting tree to the schema.

Within a step, ops run in list order.

Steps **should** be sequential (`1→2`, `2→3`, `3→4`) — the provider chains them. A step from `1→3` is allowed but non-linear paths (`1→2` **and** `1→3`) are ambiguous; providers pick one or throw. Keep it linear.

## Migration report

`LoadResult.migration()` returns `Optional<MigrationReport>`:

```java
public record MigrationReport(
        int fromVersion,
        int toVersion,
        List<AppliedStep> steps
) {
    public record AppliedStep(int fromVersion, int toVersion, int opsApplied) {}
}
```

- `Optional.empty()` — on-disk version matched schema version, nothing to do.
- Present — steps ran; you can log the transition:

```java
loadResult.migration().ifPresent(report -> {
    LOGGER.info("Migrated config from v{} to v{}", report.fromVersion(), report.toVersion());
});
```

## A worked example

Version `1` had a flat `particleColor` field. Version `2` moves it under a `graphics` category and adds `graphics.brightness`.

```java
// v1 file on disk:
// { "particleColor": "#ff8800" }
// (version tracked out-of-band; providers manage the version field.)

@Config(id = "mymod", name = "mymod", format = "json", version = 2)
public final class MyConfig {
    @Category("graphics")
    public String particleColor = "#ff8800";

    @Category("graphics")
    @Range(min = 0.0, max = 1.0)
    public double brightness = 0.7;
}

MigrationSpec migrations = () -> List.of(new MigrationStep() {
    @Override public int fromVersion() { return 1; }
    @Override public int toVersion()   { return 2; }
    @Override public List<MigrationOp> ops() {
        return List.of(
                renameKey("particleColor", "graphics.particleColor"),
                setDefault("graphics.brightness", 0.7)
        );
    }
});

HANDLE = Coal.register(MyConfig.class, migrations);
```

Result:

```json
{
    "graphics": {
        "particleColor": "#ff8800",
        "brightness": 0.7
    }
}
```

## Idempotency

Migrations should be idempotent-friendly. `setDefault` won't overwrite an existing value. `renameKey` throws on clobber. `removeKey` and `transform` no-op when the path is absent.

That means if a step accidentally runs twice — the provider should never let this happen, but you never know — the second run has minimal impact. Design your ops so a double-run doesn't lose data.

## Testing

Write plain JUnit tests against a hand-crafted `Map<String, Object>` tree:

```java
@Test
void v1_to_v2_moves_particleColor() {
    Map<String, Object> tree = new HashMap<>();
    tree.put("particleColor", "#ff8800");

    for (MigrationOp op : migrations.steps().get(0).ops()) {
        op.apply(tree);
    }

    assertEquals("#ff8800", ((Map<?, ?>) tree.get("graphics")).get("particleColor"));
    assertFalse(tree.containsKey("particleColor"));
}
```

No COAL runtime needed — `MigrationOp` is a pure function of the tree.

## What providers without `MIGRATION` do

If `Coal.getProvider().supports(Capability.MIGRATION)` returns `false`:

- The `MigrationSpec` you pass to `register(...)` is **ignored**.
- On-disk files at old versions load as-is; the corrector cleans up what it can. Users likely lose data from renamed fields.
- Check for support before shipping breaking schema changes:

```java
if (!Coal.getProvider().supports(Capability.MIGRATION)) {
    LOGGER.warn("Provider lacks MIGRATION — v{}→v{} rename won't apply.",
            oldVersion, newVersion);
}
```

`coal-noop` doesn't advertise `MIGRATION`. Both first-party adapters do.

## Related

- [Define a config](./defining-a-config) — where `@Config#version` lives.
- [Handle load corrections](./handling-load-corrections) — what happens to values migrations couldn't fix.
- [Specification §9](../spec/#9-user-api--migrations) — normative rules.
