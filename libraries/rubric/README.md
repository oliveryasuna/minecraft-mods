# Rubric

A multi-loader Minecraft mod configuration library.

## Philosophy

Rubric does what YetAnotherConfigLib ([YACL](https://modrinth.com/mod/yacl)) originally set out to do: be a *configuration library* — and nothing more. It defines your config, loads and saves it, validates and migrates it, and syncs it between server and client. It does not draw the settings screen itself.

Where YACL grew into a full configuration-plus-GUI framework, Rubric stops at the library layer and leaves the frontend to whichever config-screen library the user already has
installed. Today that means [YACL](https://modrinth.com/mod/yacl) and [Cloth Config](https://modrinth.com/mod/cloth-config), with more frontends possible via the `ScreenProvider` SPI. If neither is installed, Rubric still loads and edits your config on
disk — the in-game screen falls back to a placeholder that points the user at the config file.

## Scope

- Loader-agnostic core, with a Fabric integration module (NeoForge planned).
- Annotation-driven or programmatic config definition.
- Correct-and-log validation, versioned migrations, and an event bus for change notifications.
- Server-authoritative sync with per-entry scope control.
- Pluggable format adapters (TOML, JSON, JSON5) and IO backends.

## Status

Early. APIs may still shift.

## License

MIT.
