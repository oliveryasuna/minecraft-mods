# Minecraft Mods

Minecraft mods and mod libraries — a multi-module Gradle repository that grows one mod at a time.

## What's here

| Mod / Library | Description                                            | Directory                |
|---------------|--------------------------------------------------------|--------------------------|
| **Rubric**    | Multi-loader Minecraft mod configuration library. Defines, loads, validates, migrates, and syncs configs — and delegates the settings screen to whichever GUI library the user has installed. | `libraries/rubric/`      |

More mods will land under `libraries/` and (eventually) top-level directories as they get built.

## Documentation

The wiki is the reference:

- [Wiki home](https://github.com/oliveryasuna/minecraft-mods/wiki)
- [Rubric — Home](https://github.com/oliveryasuna/minecraft-mods/wiki/Rubric-Home)
- [Rubric — Quick Start](https://github.com/oliveryasuna/minecraft-mods/wiki/Rubric-Quick-Start)

For mod-author consumers, artifacts are published to Maven Central under group `com.oliveryasuna.mc`. See the per-mod installation page in the wiki.

## Structure

```
build-logic/                Convention plugins (published-library, licensed-library, etc.)
gradle/                     Wrapper + version catalog
libraries/
    util/                   Shared utility jar (com.oliveryasuna.mc:util)
    rubric/                 Rubric mod + library modules (com.oliveryasuna.mc:rubric-*)
        api/                    Annotations + Format enum
        core/                   ConfigManager, lifecycle, events, Platform SPI
        schema/                 Schema model + reader
        value/                  Format-neutral ValueTree + codecs
        validation/             Validators
        migration/              Versioned migration steps
        io-spi/                 ConfigIO / FormatAdapter interfaces
        io-file/                NIO file backend
        format-toml/            TOML adapter
        format-nightconfig/     Shared NightConfig bridge
        format-json/            JSON adapter
        format-json5/           JSON5 adapter (Jankson)
        sync-protocol/          Wire format for sync
        sync/                   Server/client sync logic
        mojang-codec/           Mojang Codec<T> bridge
        fabric/                 Fabric loader integration (published as a mod, not as a library)
```

Each module under `libraries/<family>/` is a leaf Gradle subproject; families share a `LICENSE` and `LICENSE.spdx` file at the family root.

## Building

Java 21 toolchain. All commands are run from the repository root.

```bash
# Compile everything
./gradlew assemble

# Run every module's check task (unit tests, lint, spotless, etc.)
./gradlew checkAll

# Publish every library to your local Maven cache — signs artifacts via GPG
./gradlew publishToMavenLocal --no-parallel
```

`--no-parallel` on publish is a workaround for GPG-agent contention during signing. See [Rubric — Installation](https://github.com/oliveryasuna/minecraft-mods/wiki/Rubric-Installation) for consumer setup.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Each library / mod carries its own `LICENSE` file at its family root under `libraries/<family>/`.
