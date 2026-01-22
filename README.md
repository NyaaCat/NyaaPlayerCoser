# NyaaPlayerCoser [![Build Status](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/1.17/badge/icon)](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/1.17/)

NyaaPlayerCoser is an NPC plugin for NyaaCraft servers. It spawns NPCs with merchant trades, command execution, player skins, and traveling merchant behavior.

## Requirements
- Paper/Spigot 1.13+ (api-version 1.13)
- NyaaCore
- ProtocolLib
- Optional: HamsterEcoHelper (integration disabled; HEH shop commands remain but will not open shops)

## Data Files
- `config.yml` - main configuration
- `npcs.yml` - NPC definitions
- `trades.yml` - trade definitions
- `skins.yml` - skin definitions
- `item-update.log` - inventory normalization log for trade requirements

## Quick Start
1) Create an NPC.
2) Define trades using hotbar slots 1-3.
3) Interact with the NPC to trade.

```text
/npc spawn VILLAGER TRADER_UNLIMITED "&aTrader"
# Put item1 in hotbar slot 1, item2 in slot 2 (optional), result in slot 3
/npc edit <npcId> trade:+
```

## NPC Types
- `TRADER_UNLIMITED`: Unlimited merchant trades backed by `trades.yml`.
- `TRADER_BOX`: Reserved for chest-backed trading (not implemented in GUI).
- `COMMAND`: Executes a configured command when interacted with.
- `HEH_SELL_SHOP`: Deprecated; entries are removed on load and integration is disabled.

## Trade Behavior
- Trades are stored as `item1 + item2 -> result`.
- Matching uses item type plus plain-text display name and lore only.
- When opening a trader, the plugin may normalize player inventory items that match trade requirements (item1/item2) to current data formats and logs updates to `item-update.log`.
- Container-like items (shulker boxes, bundles, or items with embedded inventories) are skipped to avoid overwriting player contents.

## Commands
### Core
```text
/npc reload
/npc list
/npc inspect nearby [range]
/npc inspect npc <npcId>
/npc inspect trade <tradeId>
/npc debug
```

### Spawn / Edit
```text
/npc spawn <entityType> <npcType> <displayName> [nbt]
/npc remove <npcId>
/npc edit help
/npc edit <npcId> name:<displayName>
/npc edit <npcId> npctype:<TRADER_UNLIMITED|COMMAND|HEH_SELL_SHOP>
/npc edit <npcId> entitytype:<EntityType>
/npc edit <npcId> owner:<uuid>
/npc edit <npcId> nbt:<nbtString>
/npc edit <npcId> location:me
/npc edit <npcId> trade:+
/npc edit <npcId> trade:+<tradeId>
/npc edit <npcId> trade:-<tradeId>
/npc edit <npcId> trade:<tradeId>,<tradeId>,...
/npc edit <npcId> command:<commandString>
/npc edit <npcId> command_permission:<permission>
```

Notes:
- `displayName` supports `&` colors and hex codes like `&#RRGGBB`.
- `entityType` must be spawnable/alive or `PLAYER`.
- `npcType` supports `TRADER_UNLIMITED`, `COMMAND`, and `HEH_SELL_SHOP` (deprecated).
- `nbt` is a raw entity NBT string for additional entity customization.
- `trade:+` reads hotbar slots 1-3 (indices 0-2). Slot 2 may be empty to omit `item2`.

### Trade Editing
```text
/npc edit_trade <tradeId>
```
`edit_trade` replaces the trade using hotbar slots 1-3.

### Command NPCs
- `command_permission` behavior:
  - `console` runs as console
  - `*` runs as player with temporary op
  - otherwise treat as semicolon-separated permission list (e.g. `perm.one;perm.two`)
- Placeholders in commands:
  - `{player}`, `{player.x}`, `{player.y}`, `{player.z}`
  - `{player.yaw}`, `{player.pitch}`, `{yaw}`, `{pitch}`

### Skins
```text
/npc skin add <skinId> <texture_value> <texture_signature> [description]
/npc skin pin <playerName> [follow] [skinId]
/npc skin list
/npc skin setdefault <skinId>
/npc edit <npcId> skin:<skinId>
/npc edit <npcId> skin:default
```

Notes:
- You can obtain texture values/signatures from https://mineskin.org/.
- `skin pin` stores a player's current textures; `follow` records a UUID for compatibility.

### Traveling Merchants
```text
/npc travel enable <npcId> <presentMinSec> <presentMaxSec> <absentMinSec> <absentMaxSec> <tradeMin> <tradeMax> <rndXZMax> <rndYPosMax> <rndYNegMax> <rndTryMax>
/npc travel disable <npcId>
/npc travel force_move <npcId>
```

Behavior:
- Travel plan uses the NPC's current location as the center point on first enable.
- Trades are randomized from the complete trade list while the NPC is present.
- Arrival/departure messages and broadcast ranges come from `config.yml`.

### HEH Shop (Deprecated)
```text
/npc hehshop
/npc hehshop remove
/npc my
```

Notes:
- HamsterEcoHelper integration is disabled. HEH shops will not open and HEH NPCs are removed from data on load.

## Configuration
`config.yml` fields:
- `language`: i18n language code (default `en_US`)
- `allowedEntityType`: list of allowed `EntityType` names
- `tabListDelay`: ticks to keep player NPCs in the tab list
- `playerNpcLimit`: limit for `/npc hehshop`
- `travel_merchant.distance_check`: range for travel-merchant presence checks
- `travel_merchant.broadcast_range`: radius for arrival/departure messages
- `travel_merchant.message_arrival`: message template
- `travel_merchant.message_depart`: message template

## Importing from Shopkeepers
```text
/npc import
```
Place `save.yml` from the Shopkeepers plugin into the NyaaPlayerCoser data folder. Imported data is saved to `npcs.yml` and `trades.yml`, and any failures are written to `bad-save.yml`.

## Permissions
- `npc.command`: base command permission
- `npc.command.spawn`, `npc.command.remove`, `npc.command.edit`, `npc.command.inspect`, `npc.command.reload`, `npc.command.import`, `npc.command.skin`
- `npc.command.hehshop`: player HEH shop commands
- `npc.interact`: required to interact with NPCs
- `npc.admin`: umbrella permission for admin actions
- `npc.player`: umbrella permission for player actions
- `npc.debug`: toggle debug logging

## Notes
- Player NPCs require ProtocolLib to render for clients.
- TRADER_BOX is reserved and currently not usable through the trading GUI.
- HEH shop integration is present for compatibility but not functional.

## Version History
- 7.1.x: for Minecraft 1.15.1, since build 17
- 7.0.x: for Minecraft 1.14.4, since build 8
