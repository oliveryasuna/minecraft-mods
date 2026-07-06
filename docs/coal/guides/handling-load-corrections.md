---
title: Handle load-time corrections
---

# Handle load-time corrections

When a persisted config value violates a constraint annotation, the provider **corrects** it at load time and surfaces the correction in `LoadResult.corrections`.

Requires a provider that advertises [`Capability.VALIDATION`](../concepts/capabilities). Both first-party adapters do.

## When corrections happen

Every `load()`. Not on `set(...)` — set is trusted (in the first-party adapters; some future provider could correct on set too). If your mod calls `handle.set("foo", -1)` with a value that would fail `@Range(min = 0)`, the value goes into state as-is; the next `save()` writes it; and the next `load()` corrects it back to the field's default (or a validator-supplied suggestion).

## What triggers a correction

The five constraint annotations, applied to the appropriate types:

| Annotation      | Applies to                              | Corrects to                                                       |
|-----------------|-----------------------------------------|-------------------------------------------------------------------|
| `@Range`        | Numeric fields (boxed or primitive)     | Field's declared default.                                         |
| `@Pattern`      | `String`                                | Field's declared default.                                         |
| `@OneOf`        | `String`                                | Field's declared default.                                         |
| `@Length`       | `String`, `List`, `Map`                 | Field's declared default.                                         |
| `@NotNull`      | Any                                     | Field's declared default.                                         |

The validator may **suggest** a specific correction via `ValidationResult.invalid(message, suggestion)`. When it does, the corrector uses the suggestion. When it doesn't, the corrector falls back to the field's declared default value.

## Inspecting corrections

`load()` returns a `LoadResult`:

```java
public record LoadResult(
        ConfigSnapshot snapshot,
        List<Correction> corrections,
        Optional<MigrationReport> migration
) {}
```

Each `Correction`:

```java
public record Correction(
        String path,        // "graphics.brightness"
        Object before,      // -0.5
        Object after,       // 0.7  (default or validator suggestion)
        String reason       // e.g. "Value out of range [0.0, 1.0]"
) {}
```

Note: `handle.reload()` calls `load()` under the hood but discards the returned `LoadResult`. To see corrections from a reload, you need to inspect via a `ReloadListener` (which gets `previous` and `current` states) and diff yourself, or wire your own logging inside the reload listener.

For the initial `Coal.register(...)` call, the provider handles the initial load internally — the returned handle doesn't hand you the initial `LoadResult` directly. If you need to log initial corrections, that's provider-specific behavior; the first-party adapters `INFO`-log every correction they apply. Check the provider's log output at mod-init to see them.

## Practical patterns

### Log at boot

Both first-party adapters log corrections at `INFO`. Nothing to add if that's enough.

### Notify the user in-game

The `coal-fabric` client mod already fires a toast when `coal-noop` is installed (a related but different concern). For corrections specifically, you'd hook `handle.manager().events()` and watch for `ChangeEvent`s where the sequence indicates a correction — or, simpler, wire your own `ReloadListener`:

```java
handle.manager().addReloadListener((previous, current) -> {
    if (previous.brightness != current.brightness) {
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("brightness reset — check the config file"));
    }
});
```

### Refuse to boot on corrections

Not recommended, but possible — call `handle.reload()`, catch the difference in the reload listener, and disable your mod's features if any correction shows up.

## Correction vs. exception

Corrector semantics are **repair, not reject**. A malformed persisted value gets replaced silently, not surfaced as an exception. This is deliberate:

- Users hand-editing config files can produce invalid input.
- Migration steps can leave invalid values behind.
- Old default values from a previous mod version may violate new constraints.

If you want a hard rejection instead of silent correction, don't rely on `@Range` / `@OneOf` / etc. — do the check yourself when you read the value, and throw / warn.

## What providers without `VALIDATION` do

If `Coal.getProvider().supports(Capability.VALIDATION)` returns `false`:

- The corrector is a no-op — `LoadResult.corrections` is always empty.
- `@Range` / `@Pattern` / etc. are **read into `EntryMetadata`** (available via `SchemaEntry.metadata().validators()`), but not run.
- Consumer mods that need enforcement have to walk the metadata themselves.

Neither `coal-noop` nor a hypothetical minimal provider advertises `VALIDATION`; both first-party adapters do.

## Related

- [Define a config](./defining-a-config) — the constraint annotations.
- [Lifecycle and events](../concepts/lifecycle-and-events) — where `LoadResult` fits in.
- [Capabilities](../concepts/capabilities) — the `VALIDATION` capability.
- [Specification §13](../spec/#13-provider-spi--validation-and-correction) — normative rules.
