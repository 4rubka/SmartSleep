# SmartSleep

SmartSleep is an advanced sleep and rain voting plugin for Paper 1.21+ survival servers. It opens a configurable GUI when a player enters a bed, lets eligible players vote in real time, and skips the night or clears rain when the configured threshold is met.

## Requirements

- Paper 1.21+
- Java 21
- Maven 3.9+ for building from source

Optional hooks:

- PlaceholderAPI
- Vault economy
- Essentials AFK state
- Geyser/Floodgate players can use the inventory GUI through standard Bukkit inventories

## Features

- Main `/smartsleep` control menu with actions, status, stats, top voters, reload, and version info
- Automatic night votes when players enter beds
- Manual rain votes with `/smartsleep start rain`
- Dynamic vote percentages based on online eligible players
- World whitelist/blacklist and per-world overrides
- AFK exclusion through metadata and Essentials reflection hook
- Configurable inventory GUI with MiniMessage and HEX color support
- BossBar and ActionBar vote progress
- YAML or SQLite statistics storage
- Global stats, player stats, and leaderboards
- Optional participation and successful-vote rewards
- Optional Vault economy rewards
- PlaceholderAPI expansion
- Admin reload, manual start/stop, stats, top, and version commands

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/smartsleep` | `smartsleep.vote` for actions | Opens the SmartSleep control menu for players. |
| `/smartsleep reload` | `smartsleep.admin` | Reloads config and services. |
| `/smartsleep start [night\|rain]` | `smartsleep.start` | Starts a vote in the sender's world. |
| `/smartsleep stop` | `smartsleep.admin` | Stops the current vote in the sender's world. |
| `/smartsleep stats` | `smartsleep.stats` | Shows global and personal statistics. |
| `/smartsleep top [field]` | `smartsleep.top` | Shows leaderboards. |
| `/smartsleep version` | none | Shows the plugin version. |

## Permissions

All permission nodes can be changed in `config.yml`.

- `smartsleep.vote`
- `smartsleep.admin`
- `smartsleep.start`
- `smartsleep.stats`
- `smartsleep.top`

## PlaceholderAPI

Available placeholders:

- `%smartsleep_nights_skipped%`
- `%smartsleep_rain_skipped%`
- `%smartsleep_vote_yes%`
- `%smartsleep_vote_no%`
- `%smartsleep_vote_timer%`
- `%smartsleep_player_votes%`
- `%smartsleep_top_voter%`

## Storage

Set `settings.storage` in `config.yml`:

- `YAML`: simple file-based stats in `plugins/SmartSleep/stats.yml`
- `SQLITE`: local SQLite database in `plugins/SmartSleep/stats.db`

Saving is performed asynchronously to reduce main-thread impact.

## Building

```bash
mvn package
```

The compiled jar is created at:

```text
target/SmartSleep-1.0.0.jar
```

## Notes For Large Servers

- Use `SQLITE` for busy servers with heavy stats usage.
- Keep GUI size at 27 or 36 slots unless you need more decoration.
- Use the main menu for staff-facing controls instead of forcing admins to memorize every subcommand.
- Exclude AFK players to avoid stalled votes.
- Use stricter thresholds for large player ranges.

## Updating Visuals

SmartSleep includes `settings.force-modern-visuals`. When enabled, the plugin refreshes `gui`, `main-menu`, `messages`, `bossbar`, and `actionbar` from the bundled premium defaults on startup/reload. This prevents old server configs from keeping outdated or broken menu layouts after an update.

Set it to `false` only after you finish your own custom menu/message design.

## Current Scope

SmartSleep v1 implements the core premium experience with extensible service classes. Deep third-party plugin APIs beyond Vault, PlaceholderAPI, and Essentials AFK are intentionally soft-hooked so the plugin remains stable even when optional plugins are missing.
