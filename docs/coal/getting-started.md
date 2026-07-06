---
title: Getting started
---

# Getting started

Add COAL to an existing mod, register a config, and open the settings screen.

Assumes you already have a working Fabric or NeoForge mod project on **MC 1.21.8, Java 21**. If you don't, follow your loader's official mod-setup guide first.

## 1. Add the dependencies

Consumer mods depend on **`coal-api`** (the annotation + SPI surface) and, if they want to open a settings screen, **`coal-api-gui-<loader>`** (the loader-specific GUI SPI).

The provider itself is **installed at runtime as a mod**, not consumed at compile time — your code never references YACL, Cloth Config, or any concrete provider by type. That's the whole point.

### Fabric

```kotlin
// build.gradle.kts

repositories {
    mavenCentral()
}

dependencies {
    modImplementation("com.oliveryasuna.mc:coal-api:<version>")
    modImplementation("com.oliveryasuna.mc:coal-api-gui-fabric:<version>")
}
```

### NeoForge

```kotlin
// build.gradle.kts

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.oliveryasuna.mc:coal-api:<version>")
    implementation("com.oliveryasuna.mc:coal-api-gui-neoforge:<version>")
}
```

## 2. Write a config POJO

Annotate a class with `@Config`. Its public fields become configuration entries. Field types drive the widget choice; constraint annotations (`@Range`, `@OneOf`, ...) tighten validation and steer the GUI.

```java
package com.example.mymod;

import com.oliveryasuna.mc.coal.api.annotation.*;

@Config(id = "mymod", name = "mymod", format = "json", version = 1)
public final class MyConfig {

    @Comment("Master toggle for the feature.")
    public boolean enabled = true;

    @Comment("How many things the mod does per tick.")
    @Range(min = 1, max = 100)
    public int thingsPerTick = 10;

    @Comment("Color of the demo particle (hex #RRGGBB).")
    @Widget(Widget.Type.COLOR)
    public String particleColor = "#ff8800";

    @Comment("Log-level for mymod's internal logger.")
    @OneOf({"debug", "info", "warn", "error"})
    public String logLevel = "info";
}
```

Rules of thumb:

- **Public non-final fields only.** Static, transient, and private fields are ignored.
- **Give every field a sensible default.** COAL uses field defaults when materializing the config file for a first-launch user, and as the fallback when a validator rejects a persisted value.
- **`@Config#name`** is the on-disk base name — `<config-dir>/mymod.json` for the example above.
- **`@Config#version`** starts at `1`; bump it when you need [migrations](./guides/migrating-configs).
- **`@Config#format`** is a hint. Providers that don't support the requested format warn once at registration and fall back to their supported default (JSON in the first-party providers).

Every annotation is enumerated in the [annotation reference](./reference/annotations).

## 3. Register at mod init

Call `Coal.register(YourConfig.class)` once, at mod-init time. It returns a `ConfigHandle<YourConfig>` — hold on to it; that's how you read and write values.

### Fabric

```java
package com.example.mymod;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import net.fabricmc.api.ModInitializer;

public final class MyModFabricMain implements ModInitializer {

    private static volatile ConfigHandle<MyConfig> HANDLE;

    public static ConfigHandle<MyConfig> handle() {
        return HANDLE;
    }

    @Override
    public void onInitialize() {
        HANDLE = Coal.register(MyConfig.class);
    }
}
```

### NeoForge

```java
package com.example.mymod;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("mymod")
public final class MyModNeoForgeMain {

    private static volatile ConfigHandle<MyConfig> HANDLE;

    public static ConfigHandle<MyConfig> handle() {
        return HANDLE;
    }

    public MyModNeoForgeMain(final IEventBus modEventBus) {
        HANDLE = Coal.register(MyConfig.class);
    }
}
```

## 4. Read and write values

Read the whole POJO via `handle.get()`:

```java
if(MyModFabricMain.handle().get().enabled) {
    // ...
}
```

Write individual values via `handle.set(dottedPath, value)`:

```java
MyModFabricMain.handle().set("thingsPerTick", 25);
MyModFabricMain.handle().save();
```

The path is dotted for nested POJOs and categories. A field named `particleColor` at the root is `"particleColor"`; a field `foo` on a nested POJO reached via `graphics` would be `"graphics.foo"`.

For a snapshot of the entire load result (including any corrections the provider made), call `handle.snapshot()`. For lifecycle events (config reloaded, saved, changed), see [Lifecycle and events](./concepts/lifecycle-and-events).

## 5. Open the settings screen

The GUI SPI lives in `coal-api-gui-<loader>`. Call `GuiRegistry.open(client, parent, manager)` on the client to get a `Screen` you can hand to `Minecraft#setScreen`.

The example below binds `K` to open the config screen:

```java
package com.example.mymod;

import com.mojang.blaze3d.platform.InputConstants;
import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public final class MyModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        final KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mymod.openConfig",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                "mymod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(openKey.consumeClick()) {
                if(MyModFabricMain.handle() == null) return;

                client.setScreen(GuiRegistry.open(
                        client,
                        client.screen,
                        MyModFabricMain.handle().manager()
                ));
            }
        });
    }
}
```

`GuiRegistry.open` picks the highest-priority registered `ScreenProvider` (the one shipped by whichever GUI adapter the user installed) and delegates rendering. Your mod never mentions YACL or Cloth by name.

If **no** screen provider is registered — which happens when the user has installed `coal` but no adapter — `open(...)` throws `IllegalStateException`. Guard the call or let it surface, up to you.

## 6. Install a provider at runtime

Your mod is now COAL-ready but *invisible* to the user until they install a provider. Two first-party options:

- **[YACL adapter](./adapters/yacl)** — `coal-yacl-adapter-<loader>.jar` + YetAnotherConfigLib.
- **[Cloth Config adapter](./adapters/cloth)** — `coal-cloth-adapter-<loader>.jar` + Cloth Config.

Users drop `coal.jar` + one adapter jar + the underlying GUI mod into their `mods/` folder. Your consumer mod picks up the provider on next boot with no changes.

If they install nothing but `coal.jar`, the bundled `coal-noop` provider takes over: everything registers cleanly, `handle.get()` returns your defaults every time, `save()` and `set()` are no-ops, and a toast fires on the title screen telling the user no real backend is installed. You can detect this in code with `Coal.isNoopProvider()`.

## Next

- [Define a config](./guides/defining-a-config) — the full annotation vocabulary.
- [Handle load corrections](./guides/handling-load-corrections) — what happens when a persisted value violates a constraint.
- [Provider model](./concepts/provider-model) — how factory discovery and priority work.
- [GUI delegation](./concepts/gui-delegation) — priority ordering when multiple screen providers are installed.
