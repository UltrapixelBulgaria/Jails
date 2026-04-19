# 🔒 UPBGJail

A feature-rich, fully configurable Minecraft jail plugin built for Spigot/Paper servers. Supports per-cell management, Discord webhooks, PlaceholderAPI integration, CMI `/whois` interception and complete message customization via `config.yml`.

---

## 📋 Table of Contents

- [Features](#-features)
- [Dependencies](#-dependencies)
- [Installation](#-installation)
- [Configuration](#-configuration)
  - [Database](#database-setup)
  - [Jail Region & Cells](#jail-region--cells)
  - [Spawn Location](#spawn-location)
  - [Messages](#messages)
  - [Discord Webhook](#discord-webhook)
- [In-Game Setup Guide](#-in-game-setup-guide)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Placeholders](#-placeholders)
- [Building from Source](#-building-from-source)

---

## ✨ Features

- 🏛️ **Multi-cell jail system** — configure as many cells as you need, each with its own location
- ⏱️ **Flexible time parsing** — supports `10s`, `5m`, `2h`, `1d` formats
- 🔕 **Silent jailing** — use `-s` flag to jail/unjail without a global broadcast
- 📜 **Full jail history** — per-player history with active/expired status, unjail info and total time served
- 💬 **100% configurable messages** — every message, color and placeholder lives in `config.yml`
- 🎨 **HEX color support** — use `#rrggbb` colors anywhere in messages
- 🔗 **Discord webhook integration** — rich embeds sent to Discord on jail, unjail and auto-release, each individually toggleable
- 📊 **PlaceholderAPI support** — cell-based and player-based placeholders for holograms, scoreboards and more
- 🔍 **CMI `/whois` injection** — jail status appended automatically to CMI's whois output
- 🗄️ **MySQL backend** — all jail data persisted with full history
- 🔄 **Auto-release scheduler** — players are automatically released and teleported when their sentence expires
- ♻️ **Live reload** — reload config and reconnect the database without restarting

---

## 📦 Dependencies

| Dependency | Type | Version | Link |
|---|---|---|---|
| [Spigot / Paper](https://papermc.io/) | Required | 26.1+ | https://papermc.io |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Soft depend | 2.11+ | https://www.spigotmc.org/resources/6245 |
| [WorldGuard](https://enginehub.org/worldguard) | Soft depend | 7.x | https://enginehub.org/worldguard |
| [WorldEdit](https://enginehub.org/worldedit) | Soft depend | 7.x | https://enginehub.org/worldedit |
| MySQL | Required | 5.7+ / 8.x | — |

> **Note:** PlaceholderAPI, WorldEdit and WorldGuard are soft dependencies — the plugin will function without them, but their respective features (whois injection, placeholders, region detection) will be unavailable.

---

## 🚀 Installation

1. Download the latest `UPBGJail.jar` from the [Releases](../../releases) page
2. Place it in your server's `plugins/` folder
3. Install all required dependencies listed above
4. Start the server once to generate `plugins/UPBGJail/config.yml`
5. Stop the server and configure `config.yml` (see [Configuration](#-configuration))
6. Start the server again
7. Follow the [In-Game Setup Guide](#-in-game-setup-guide) to create your jail cells

---

## ⚙️ Configuration

Below is a full annotated `config.yml`. Every option is explained.

### Database Setup

```yaml
# Master enable switch — set to false to fully disable the plugin
enable: true

database:
  host: "localhost"
  port: 3306
  name: "your_database_name"
  username: "your_db_user"
  password: "your_db_password"
  table: "jails"
```

---

### Jail Region & Cells

Cells are saved automatically via the `/upjail setcell <number>` command (see [In-Game Setup](#-in-game-setup-guide)).  
They are stored in config under this structure:

```yaml
jail:
  region: "your_worldguard_region_name"   # Set via /upjail setregion <name>

  cells:
    1:
      world: "world"
      x: 100.5
      y: 64.0
      z: 200.5
      yaw: 90.0
      pitch: 0.0
    2:
      world: "world"
      x: 110.5
      y: 64.0
      z: 200.5
      yaw: 90.0
      pitch: 0.0
    # Add as many cells as needed
```

> ⚠️ You do not need to edit this manually — use `/upjail setcell <number>` in-game.

---

### Spawn Location

The location players are teleported to when released from jail. Set automatically via `/upjail setspawn`.

```yaml
  spawn:
    world: "world"
    x: 0.5
    y: 64.0
    z: 0.5
    yaw: 0.0
    pitch: 0.0
```

---

### Messages

Every player-facing message is defined here. Supports `&` color codes, `&l &o &k` formatting codes and `#rrggbb` HEX colors.

**Available placeholders per message are noted in the comments.**

```yaml
messages:
  # General
  no_permission:     "&cNo permission."
  player_not_found:  "&cPlayer not found."
  player_only:       "&cThis command can only be run by a player."

  # /jail
  usage_jail:        "&cUsage: /jail <username> <time> [reason] [-s]"
  already_jailed:    "&cThis player is already jailed."
  invalid_time:      "&cInvalid time format. Use: 10s, 5m, 2h, 1d"
  no_cells:          "&cNo cells are available."
  invalid_cell:      "&cCell location is invalid or world not found."

  # %player% %staff% %time% %reason% %cell%
  broadcast:         "&c&l[JAIL] &f%player% &7has been jailed by &f%staff% &7for &f%time%&7. Reason: &f%reason%"
  sender_silent:     "&7[Silent] &aPlayer &f%player% &ahas been jailed.\n&7Cell: &f%cell% &7| Time: &f%time% &7| Reason: &f%reason%"

  # Sent to the jailed player
  notify_jailed_title:    "&c&lYou have been jailed!"
  notify_jailed_by:       "&7Jailed by: &f%staff%"     # %staff%
  notify_jailed_reason:   "&7Reason: &f%reason%"        # %reason%
  notify_jailed_duration: "&7Duration: &f%time%"        # %time%

  # /unjail
  usage_unjail:      "&cUsage: /unjail <username> [reason] [-s]"
  not_jailed:        "&cThis player is not jailed."

  # %player% %staff% %reason%
  broadcast_unjail:      "&a&l[UNJAIL] &f%player% &7has been unjailed by &f%staff%&7. Reason: &f%reason%"
  sender_silent_unjail:  "&7[Silent] &aPlayer &f%player% &ahas been unjailed.\n&7Reason: &f%reason%"

  # Sent to the unjailed player
  notify_unjailed_title:  "&a&lYou have been unjailed!"
  notify_unjailed_by:     "&7Unjailed by: &f%staff%"   # %staff%
  notify_unjailed_reason: "&7Reason: &f%reason%"        # %reason%

  # Auto-release (sentence expired)
  released:                "&aYou have been released from jail. Sentence expired."
  release_system_reason:   "Sentence expired"

  # /upjail setspawn
  spawn_set: "&aJail spawn location has been set to your current position."

  # /upjail setregion
  region_set: "&aJail region set to: %region%"          # %region%

  # /upjail setcell
  cell_set: "&aCell %cell% set to your current location." # %cell%

  # /jailinfo
  usage_info: "&cUsage: /jailinfo <username>"
```

---

### Discord Webhook

Each event type can be independently enabled or disabled. Set `enabled: false` on any block to silence that event.

```yaml
discord:
  # Master switch — disables all webhooks when false
  enabled: true
  webhook_url: "https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN"

  jail:
    enabled: true          # Set false to disable jail webhooks
    color: 15158332        # Decimal color (red). Pick at https://www.spycolor.com
    title: "🔒 Player Jailed"
    footer: "UPBGJail"
    thumbnail: "https://mc-heads.net/avatar/%player%"
    fields:
      - name: "Player"
        value: "%player%"
        inline: true
      - name: "Jailed By"
        value: "%staff%"
        inline: true
      - name: "Cell"
        value: "%cell%"
        inline: true
      - name: "Duration"
        value: "%time%"
        inline: true
      - name: "Reason"
        value: "%reason%"
        inline: false

  unjail:
    enabled: true          # Set false to disable unjail webhooks
    color: 3066993         # Green
    title: "🔓 Player Unjailed"
    footer: "UPBGJail"
    thumbnail: "https://mc-heads.net/avatar/%player%"
    fields:
      - name: "Player"
        value: "%player%"
        inline: true
      - name: "Unjailed By"
        value: "%staff%"
        inline: true
      - name: "Reason"
        value: "%reason%"
        inline: false

  auto_release:
    enabled: true          # Set false to disable auto-release webhooks
    color: 3066993
    title: "⏰ Player Auto-Released"
    footer: "UPBGJail"
    thumbnail: "https://mc-heads.net/avatar/%player%"
    fields:
      - name: "Player"
        value: "%player%"
        inline: true
      - name: "Reason"
        value: "Sentence expired"
        inline: false
```

**Discord webhook toggle reference:**

| Goal | Config change |
|---|---|
| Disable all Discord messages | `discord.enabled: false` |
| Disable jail notifications only | `discord.jail.enabled: false` |
| Disable unjail notifications only | `discord.unjail.enabled: false` |
| Disable auto-release notifications only | `discord.auto_release.enabled: false` |

---

## 🏗️ In-Game Setup Guide

Follow these steps after installing the plugin and configuring your database.

### Step 1 — Set the jail spawn

Stand at the location you want players teleported to when they are released from jail, then run:

```
/upjail setspawn
```

This is the location outside the jail they will be sent to after their sentence ends or they are manually unjailed.

---

### Step 2 — Set the jail region *(requires WorldGuard)*

Create a WorldGuard region covering your entire jail area using WorldEdit's selection tool, then run:

```
/rg define <regionName>
/upjail setregion <regionName>
```

This links the plugin to your jail region for boundary enforcement.

---

### Step 3 — Set up cells

Walk into each cell one by one and run the command for each:

```
/upjail setcell 1
/upjail setcell 2
/upjail setcell 3
# ... repeat for as many cells as you have
```

The plugin saves your exact position (including yaw/pitch facing direction) as that cell's teleport point. When a player is jailed into cell 2, they will be placed exactly where you were standing when you ran `/upjail setcell 2`.

> 💡 **Tip:** Face the direction you want the jailed player to look when they arrive before running the command.

---

### Step 4 — Test it

```
/jail <yourname> 5m Test jail
```

You should be teleported to the first available cell. After 5 minutes (or `/unjail <yourname>`) you will be sent back to the spawn location you set in Step 1.

---

### Step 5 — Set up holograms *(optional, requires a hologram plugin + PlaceholderAPI)*

In front of each cell, create a hologram with lines like these (replace `1` with the cell number):

```
%jail_occupied_1%
%jail_player_1%
%jail_reason_1%
%jail_time_left_1%
```

These update automatically as players are jailed and released.

---

## 📌 Commands

| Command | Description | Permission |
|---|---|---|
| `/jail <player> <time> [reason] [-s]` | Jail a player. Add `-s` anywhere in the reason for a silent jail (no broadcast) | `jails.jail` |
| `/unjail <player> [reason] [-s]` | Unjail a player. Add `-s` for silent unjail | `jails.unjail` |
| `/jailinfo <player>` | View full jail history for a player | `jails.info` |
| `/upjail setcell <number>` | Save your current location as a jail cell | `jails.admin` |
| `/upjail setspawn` | Save your current location as the release spawn | `jails.admin` |
| `/upjail setregion <name>` | Link a WorldGuard region as the jail region | `jails.admin` |
| `/upjail reload` | Reload config and reconnect to the database | `jails.admin` |

### Time Format Examples

| Input | Duration |
|---|---|
| `30s` | 30 seconds |
| `10m` | 10 minutes |
| `2h` | 2 hours |
| `1d` | 1 day |
| `1d12h` | 1 day and 12 hours |

### Silent Flag `-s`

The `-s` flag can be placed anywhere after the time argument:

```
/jail Steve 1h griefing -s
/jail Steve 1h -s
/unjail Steve appeal accepted -s
```

When silent, **no global broadcast is sent**, but the jailed/unjailed player still receives their personal notification message.

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `jails.jail` | Allows use of `/jail` | op |
| `jails.unjail` | Allows use of `/unjail` | op |
| `jails.info` | Allows use of `/jailinfo` | op |
| `jails.admin` | Allows all `/upjail` subcommands (setcell, setspawn, setregion, reload) | op |

---

## 📊 Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to be installed.

### Cell-Based Placeholders

These do not require a player context — they reflect the current occupant of a specific cell. Replace `N` with the cell number (e.g. `%jail_player_1%`).

| Placeholder | Description | Example Output |
|---|---|---|
| `%jail_occupied_N%` | Whether cell N currently has a prisoner | `Yes` / `No` |
| `%jail_player_N%` | Username of the player in cell N | `Steve` / `Empty` |
| `%jail_reason_N%` | Jail reason for the player in cell N | `Griefing` / `Empty` |
| `%jail_time_left_N%` | Formatted time remaining for cell N | `2h 14m 10s` / `Empty` |
| `%jail_jailed_by_N%` | Who jailed the player currently in cell N | `Admin` / `Empty` |

### Player-Based Placeholders

These require a player context (e.g. for scoreboards, tab lists, or per-player holograms).

| Placeholder | Description | Example Output |
|---|---|---|
| `%jail_is_jailed%` | Whether this player is currently jailed | `Yes` / `No` |
| `%jail_my_cell%` | The cell number this player is in | `3` / `None` |
| `%jail_my_reason%` | This player's current jail reason | `Hacking` / `None` |
| `%jail_my_time_left%` | This player's remaining sentence | `45m 2s` / `None` |
| `%jail_my_jailed_by%` | Who jailed this player | `Admin` / `None` |

### Hologram Example

Using [HolographicDisplays](https://dev.bukkit.org/projects/holographic-displays) or [DecentHolograms](https://www.spigotmc.org/resources/decentholograms.96927/) in front of cell 1:

```
&6&lCell #1
&7Occupied: %jail_occupied_1%
&7Prisoner: &f%jail_player_1%
&7Reason: &f%jail_reason_1%
&7Time Left: &c%jail_time_left_1%
```

---

## 🔨 Building from Source

Requirements: Java 25+, Gradle 9+

```bash
git clone https://github.com/UltrapixelBulgaria/Jails.git
cd Jails
./gradlew build
```

The compiled jar will be output to `build/libs/Jails.jar`.

### build.gradle

```groovy
plugins {
    id 'java'
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = 'org.proto68'
version = '1.0'

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = "https://repo.papermc.io/repository/maven-public/"
    }
    maven {
        name = "enginehub"
        url = "https://maven.enginehub.org/repo/"
    }
    maven {
        url = 'https://repo.extendedclip.com/releases/'
    }
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.5-alpha")

    // WorldEdit
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.0")

    // WorldGuard
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.16")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.12.2")

    // WorldGuard Events
    compileOnly("net.raidstone:WorldGuardEvents:1.18.1")
}

def targetJavaVersion = 25

java {
    def javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}
```
---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <sub>Built with ☕ for the UPBG Minecraft network</sub>
</div>
