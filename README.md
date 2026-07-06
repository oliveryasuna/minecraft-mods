# Minecraft Mods

Minecraft mods and mod libraries — a multi-module Gradle repository that grows one mod at a time.

## What's here

| Mod / Library         | Description                                                                                                                                                                                                                                                                                         | Directory                                               |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|
| **Rubric**            | Multi-loader Minecraft mod configuration library. Defines, loads, validates, migrates, and syncs configs — and delegates the settings screen to whichever GUI library the user has installed.                                                                                                       | `libraries/rubric/`                                     |
| **COAL**              | Config Options Abstraction Layer. SLF4J-style spec + API + provider SPI for Minecraft mod configuration. Mods depend on `coal-api`; pluggable providers implement it. Ships as a Minecraft mod (Fabric + NeoForge) that bundles the API + coal-noop.                                                | `libraries/coal/`                                       |
| **coal-yacl-adapter**  | COAL provider adapter backed by YetAnotherConfigLib (JSON persistence + YACL GUI). First non-noop implementation of the COAL SPI. Ships as loader-specific `fabric` and `neoforge` variants; the MC-free core lives in `libraries/coal/adapter-common/` and is shared with the Cloth adapter. | `libraries/coal/yacl-adapter-{fabric,neoforge}/`  |
| **coal-cloth-adapter** | COAL provider adapter backed by Cloth Config (JSON persistence + Cloth GUI). Second non-noop provider — shares the MC-free `adapter-common` core with the YACL adapter and adds the Cloth-typed screen provider + loader entry classes. Ships as `fabric` and `neoforge` variants.        | `libraries/coal/cloth-adapter-{fabric,neoforge}/` |

More mods will land under `libraries/` and (eventually) top-level directories as they get built.

## Documentation

**Hosted docs site: [mc.oliveryasuna.com](https://mc.oliveryasuna.com/)** — engineer-facing docs, guides, and the COAL specification, all under one nav.

Source files (browse on GitHub):

- [COAL documentation](docs/coal/index.md) — engineer-facing docs: introduction, getting-started, concepts, guides, and reference.
- [COAL Specification](docs/coal/spec/index.md) — the normative document for the COAL API and provider SPI. RFC-2119 keywords; Appendix A rationale; Appendix B glossary.
- Wiki (Rubric only for now):
    - [Wiki home](https://github.com/oliveryasuna/minecraft-mods/wiki)
    - [Rubric — Home](https://github.com/oliveryasuna/minecraft-mods/wiki/Rubric-Home)
    - [Rubric — Quick Start](https://github.com/oliveryasuna/minecraft-mods/wiki/Rubric-Quick-Start)

For mod-author consumers, artifacts are published to Maven Central under group `com.oliveryasuna.mc`. See the per-mod installation page in the wiki.

## Structure

```
build-logic/                Convention plugins (published-library, licensed-library, etc.)
gradle/                     Wrapper + version catalog
docs/                       Vitepress docs site — engineer-facing docs + spec
    coal/                       COAL docs (see Documentation above)
        spec/                       Normative spec for COAL
libraries/
    util/                   Shared utility jar (com.oliveryasuna.mc:util)
    rubric/                 Rubric mod + library modules (com.oliveryasuna.mc:rubric-*)
        api/                    Leaf module: annotations + Format enum. Everyone depends on it.
        core/                   Runtime: Rubric entry point, ConfigManager, ConfigHandle, ConfigValue, lifecycle, events, Platform SPI
        model/                  Config data model: schema tree, format-neutral ValueTree + codecs, validators, ConfigSpec builder
        io/                     ConfigIO / FormatAdapter interfaces + NIO file backend
        format/                 Format adapters: TOML, JSON, JSON5 (NightConfig + Jankson backends)
        migration/              Versioned migration steps
        sync/                   Server/client sync — wire protocol + runtime service
        mojang-codec/           Mojang Codec<T> bridge
        loader-common/          Loader-agnostic bits (self-config POJO, RubricSelf, RubricSerialization, ScreenBuildContext)
        fabric/                 Fabric loader integration (published as a mod, not as a library)
        neoforge/               NeoForge loader integration (published as a mod, not as a library)
    coal/                   COAL API + reference impl + adapters (com.oliveryasuna.mc:coal-*)
        api/                    Spec jar: annotations, ConfigHandle, Schema, Platform, ConfigProvider SPI. MC-free.
        api-gui-fabric/         GUI-delegation SPI, Fabric variant (Loom-remapped intermediary bytecode)
        api-gui-neoforge/       GUI-delegation SPI, NeoForge variant (Mojmap bytecode). Mirrored source with the Fabric variant.
        api-sync/               Optional sync SPI: SyncService, NetworkTransport, SyncPayload, ProtocolVersion
        noop/                   Deep-noop provider (like slf4j-nop). Last-resort fallback, priority 0
        testkit/                TCK-style abstract JUnit classes providers extend for conformance; ConformanceReport JSON
        fabric/                 Fabric mod project. Ships as coal.jar; JiJ-bundles the API modules + coal-noop
        neoforge/               NeoForge mod project. Same idea via jarJar
        adapter-common/         Shared adapter core (MC-free): schema reader, JSON I/O, AdapterConfigProvider, validators, event bus, config manager, AdapterScreenSupport helpers. Consumed by both the YACL and Cloth adapters
        yacl-adapter-fabric/    coal-yacl-adapter Fabric variant: YACL-backed ScreenProvider + loader-local ConfigProviderFactory + Fabric mod entry classes
        yacl-adapter-neoforge/  coal-yacl-adapter NeoForge variant: YACL-backed ScreenProvider + loader-local ConfigProviderFactory + NG mod entry classes
        cloth-adapter-fabric/   coal-cloth-adapter Fabric variant: Cloth-backed ScreenProvider + loader-local ConfigProviderFactory + Fabric mod entry classes
        cloth-adapter-neoforge/ coal-cloth-adapter NeoForge variant: Cloth-backed ScreenProvider + loader-local ConfigProviderFactory + NG mod entry classes
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

`--no-parallel` on publish is a workaround for GPG-agent contention during signing.
See [Rubric — Installation](https://github.com/oliveryasuna/minecraft-mods/wiki/Rubric-Installation) for consumer setup.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Each library / mod carries its own `LICENSE` file at its family root under `libraries/<family>/`.
