---
title: GUI delegation
---

# GUI delegation

Settings-screen rendering is delegated to whichever GUI library the user installed. Consumer mods don't reference YACL or Cloth by name — they call `GuiRegistry.open(...)` and get a `Screen`.

The GUI SPI ships in a separate module from `coal-api`, in two variants: `coal-api-gui-fabric` (Loom-remapped intermediary bytecode) and `coal-api-gui-neoforge` (Mojmap bytecode). Same source, different bytecode targets — see [spec §17.1](../spec/#171-two-variant-story) for why.

## The three pieces

### `ScreenProvider`

A `ScreenProvider` ([`com.oliveryasuna.mc.coal.api.gui.ScreenProvider`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api-gui-fabric/src/main/java/com/oliveryasuna/mc/coal/api/gui/ScreenProvider.java)) knows how to build a `Screen` from a `ConfigManager`. Providers report:

| Method                                        | Purpose                                                                                    |
|-----------------------------------------------|--------------------------------------------------------------------------------------------|
| `String id()`                                 | Short identifier — `"yacl"`, `"cloth"`. Used in logging.                                   |
| `int priority()`                              | Higher wins. See [priority conventions](#priority-conventions) below.                      |
| `Screen create(Minecraft, Screen, ConfigManager<?>)` | Returns the built screen, or `null` to refuse this specific config.                 |

### `GuiRegistry`

`GuiRegistry` ([`com.oliveryasuna.mc.coal.api.gui.GuiRegistry`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/api-gui-fabric/src/main/java/com/oliveryasuna/mc/coal/api/gui/GuiRegistry.java)) is a static registry with two methods you care about:

- `registerProvider(ScreenProvider)` — called at client init by the GUI adapter mod.
- `open(Minecraft client, Screen parent, ConfigManager<?> manager)` — called by consumer mods to open a settings screen.

Backed by a `CopyOnWriteArrayList` so registration and iteration are safe from any thread.

### Selection

`GuiRegistry.open(...)` sorts registered providers by `priority()` descending, then walks them:

1. Call `provider.create(client, parent, manager)`.
2. If the return is non-`null`, use that `Screen`.
3. If `null` (the provider **refused** this specific config — e.g., an unsupported schema shape), fall through to the next-highest-priority provider.
4. If no provider is registered at all, throw `IllegalStateException` with `"no ScreenProvider registered"`.
5. If every provider returned `null`, throw `IllegalStateException` with the tried-providers list.

The fall-through-on-`null` design lets specialized providers coexist with general ones — a purpose-built provider might handle only its own mod's configs and refuse others.

## Priority conventions

Screen-provider priority is a **separate namespace** from the `ConfigProviderFactory` priority governing which backend does the persistence.

| Provider           | Screen-provider `priority()` |
|--------------------|------------------------------|
| YACL adapter       | `50`                         |
| Cloth adapter      | `40`                         |
| Consumer overrides | `100+`                       |

If both YACL and Cloth adapter mods are installed, YACL renders — but this rarely happens in practice because both adapter installations require exactly one factory to win at the data layer. In the multi-install case, whichever *factory* wins is decoupled from whichever *screen provider* wins, so a Cloth-persisted config could render in YACL. Not a problem; both adapters share `coal-adapter-common` and speak the same `ConfigManager`.

## Consumer flow

```java
// Client-side entry, at mod init:
KeyMapping openKey = // register 'K' with the loader's keybinding API...

// Every tick, check for the key press:
if (openKey.consumeClick()) {
    Minecraft client = Minecraft.getInstance();
    Screen next = GuiRegistry.open(
            client,
            client.screen,
            MyModMain.handle().manager()   // the ConfigManager<S> from your handle
    );
    client.setScreen(next);
}
```

Consumer mods never name the GUI library. If the user installs YACL, they get a YACL screen; Cloth, a Cloth screen.

## Provider flow

The adapter registers a screen provider at **client-init only** — usually in the loader's client entry class:

```java
public final class YaclAdapterFabricClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GuiRegistry.registerProvider(new YaclScreenProvider());
    }
}
```

The `YaclScreenProvider` implementation walks the `ConfigManager`'s `Schema`, translates each `SchemaEntry` to a YACL widget, and returns the assembled `Screen`.

## What "no GUI installed" looks like

If the user installed `coal.jar` but no adapter:

- Consumer mods still start (COAL still bootstraps; noop provider is installed).
- `Coal.isNoopProvider()` returns `true`.
- A toast shows on first title-screen (`coal-fabric` / `coal-neoforge` client mod handles this).
- A chat message on every world join.
- Calling `GuiRegistry.open(...)` throws `IllegalStateException` — no provider registered.

Guard the `open()` call or let the throw surface — that's a UX decision, not a COAL one.

## Related

- [Provider model](./provider-model) — the parallel factory-priority story for the data layer.
- [Capabilities](./capabilities) — providers advertise `GUI_DELEGATION` when they participate in this system.
- [Specification §17](../spec/#17-optional-module--gui-delegation-coal-api-gui-) — normative rules and the two-variant rationale.
