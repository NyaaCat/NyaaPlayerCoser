# NyaaPlayerCoser [![Build Status](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/version%252F1.21.11/badge/icon)](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/version%252F1.21.11/)

NyaaPlayerCoser is an NPC plugin for NyaaCraft servers. It provides merchant trades, command execution, player-skin NPCs (rendered via packets), and a traveling-merchant behavior.

[中文文档](README_zh.md)

## Requirements
- Paper 1.21.11 (the plugin is built against `paper-api:1.21.11-R0.1-SNAPSHOT`)
- Java 21
- NyaaCore 9.10+
- ProtocolLib (dev-build — required for 1.21.11 packet support)
- Optional: HamsterEcoHelper — integration is currently disabled. `HEH_SELL_SHOP` NPCs are removed from `npcs.yml` on load, and any remaining `/npc hehshop` usage will no longer open a shop.

> Note: `plugin.yml` still declares `api-version: 1.13` for legacy compatibility, but the code uses modern Paper 1.21.11 APIs and cannot run on older servers.

## Build
```bash
./gradlew build
```
The build downloads a ProtocolLib dev-build jar to `libs/ProtocolLib.jar` automatically. Produced jar: `build/libs/NyaaPlayerCoser-mc1.21.11-8.2.*.jar`.

## Data Files
All files live in the plugin data folder (`plugins/NyaaPlayerCoser/`):
- `config.yml` — main plugin configuration (keys listed below; file is empty by default and uses defaults).
- `npcs.yml` — NPC definitions.
- `trades.yml` — trade definitions.
- `skins.yml` — skin definitions.
- `item-update.log` — audit log for trade-item normalizations (see Trade Behavior).
- `save.yml` / `bad-save.yml` — used by `/npc import` (Shopkeepers migration input / failed entries).

## Quick Start
1. Aim at a block with 2 blocks of free headroom above it.
2. Spawn the NPC:
   ```text
   /npc spawn VILLAGER TRADER_UNLIMITED "&aTrader"
   ```
3. Put `item1` into hotbar slot 1, `item2` into slot 2 (optional — leave empty for one-ingredient trades), `result` into slot 3.
4. Define the trade:
   ```text
   /npc edit <npcId> trade:+
   ```
5. Right-click the NPC to trade.

## NPC Types
- `TRADER_UNLIMITED` — unlimited merchant trades backed by `trades.yml`.
- `TRADER_BOX` — reserved for chest-backed trading. The enum and data fields exist, but no GUI flow is implemented (`/npc spawn` rejects it, and opening one shows "type not supported").
- `COMMAND` — executes a configured command when interacted with.
- `HEH_SELL_SHOP` — **deprecated and removed on load**. HamsterEcoHelper integration is disabled in the current build.

## NPC Entities
Spawned entities are marked with two scoreboard tags:
- `nyaa_npc_id:<npcId>` — identifies the entity as a Nyaa NPC and stores its id.
- `rpgitem_ignore` — prevents RPGItems-reloaded from applying damage/effects to the NPC.

NPCs are invulnerable, silent, cannot pick up items, have AI disabled and movement speed locked to 0 (via an attribute modifier named `npc:immobile_entity`). Player-skin NPCs are rendered entirely via ProtocolLib packets and require ProtocolLib to be installed.

## Trade Behavior
- Each trade is stored as `item1 (+ item2) -> result`. `item2` may be AIR to represent a single-ingredient trade.
- Item matching uses **type + plain-text display name + plain-text lore** (`ItemStackUtils.isSimilarPlainText`), ignoring other NBT.
- When a player opens a trader, the plugin inspects that player's inventory and normalizes any stack that matches a trade requirement (`item1` / `item2`) so its NBT format matches the current version's canonical form. Each update is recorded as a line in `item-update.log`.
  - The normalization runs off the main thread (snapshot on main thread, diff async, apply on main thread).
  - If two trades on the same NPC disagree on the canonical form of the same item (ambiguous baseline), that item is skipped.
  - **Container-like items are always skipped** to avoid overwriting their contents: shulker boxes (Tag.SHULKER_BOXES), bundles (`BundleMeta`), and any item whose `BlockStateMeta` is a `Container`.

## Commands
The top-level command is `/nyaaplayercoser` (alias: `/npc`).

### Core
```text
/npc reload
/npc list
/npc inspect nearby [range]
/npc inspect npc <npcId>
/npc inspect trade <tradeId>
/npc debug
```
- `inspect nearby` reports the NPC under the player's cursor (within ~6 blocks). If `range` is given, it also lists NPCs whose stored location falls inside the cube of that half-size centered on the player.
- `inspect trade` prints the trade data and drops copies of `item1`, `item2`, `result` on the ground for you to pick up and inspect.
- `debug` toggles verbose plugin logging (`NyaaPlayerCoser.debugEnabled`).

### Spawn / Edit
```text
/npc spawn <entityType> <npcType> <displayName> [nbt]
/npc remove <npcId>
/npc edit help
/npc edit <npcId> name:<displayName>
/npc edit <npcId> npctype:<TRADER_UNLIMITED|COMMAND|HEH_SELL_SHOP>
/npc edit <npcId> entitytype:<EntityType>
/npc edit <npcId> owner:<uuid>
/npc edit <npcId> hehowner:<uuid>
/npc edit <npcId> nbt:<nbtString>
/npc edit <npcId> location:me
/npc edit <npcId> skin:<skinId>
/npc edit <npcId> skin:default
/npc edit <npcId> trade:+
/npc edit <npcId> trade:+<tradeId>
/npc edit <npcId> trade:-<tradeId>
/npc edit <npcId> trade:<tradeId>,<tradeId>,...
/npc edit <npcId> command:<commandString>
/npc edit <npcId> command_permission:<permission>
```

Notes:
- `/npc spawn` ray-traces from the sender's eyes; the targeted block must exist and **must have 2 air blocks above it** (the NPC itself and one of clearance). The NPC is placed at the centre-top of that block.
- `displayName` supports `&` color codes and hex colors like `&#RRGGBB`.
- `entityType` must be a type that is both spawnable and alive, or `PLAYER`. For `PLAYER`, the display name is used as the player name and must be between 3 and 16 characters.
- `npctype` at spawn/edit time only accepts `TRADER_UNLIMITED`, `COMMAND`, and `HEH_SELL_SHOP` (deprecated — editing to this type is still permitted but the NPC won't function).
- `nbt` is a raw entity NBT string applied post-spawn (e.g. `{Profession:1}` for a villager).
- `trade:+` reads hotbar slots 1, 2, 3 (inventory indices 0, 1, 2). Slot 2 may be empty to omit `item2`.
- `trade:<id1>,<id2>,...` replaces the full trade list; every listed id must already exist in `trades.yml`.
- Changes trigger an `NpcRedefinedEvent` which closes all open merchant windows for the NPC.

### Trade Editing
```text
/npc edit_trade <tradeId>
```
Replaces the trade referenced by `<tradeId>` with the contents of hotbar slots 1-3. All NPCs that reference this trade have their open merchant windows closed (`TradeRedefinedEvent`).

### Command NPCs
`command_permission` controls how the command is run:
- `console` — run as the console.
- `*` — run as the player with a temporary OP grant (reverted in a `finally` block).
- otherwise — treated as a `;`-separated permission list (e.g. `perm.one;perm.two`). Each permission and all of its dot-prefixes are granted for a single-tick `PermissionAttachment` while the command runs.

Placeholders expanded in the command string:
- `{player}` — player name.
- `{player.x}`, `{player.y}`, `{player.z}` — player location.
- `{player.yaw}` — eye yaw + 90.
- `{player.pitch}` — negated eye pitch.
- `{yaw}` — body yaw + 90.
- `{pitch}` — negated body pitch.

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
- Texture values/signatures can be obtained from https://mineskin.org/ or the `textures` property of any online player.
- `skin pin <playerName>` copies that player's current textures into a new `SkinData`. If `follow` is `true`, the player's UUID is stored in `followPlayer` for downstream features. If `skinId` is omitted, the skin is saved under `<playerName>`.
- Skins only take effect on NPCs whose `entityType` is `PLAYER`.

### Traveling Merchants
```text
/npc travel enable <npcId> <presentMinSec> <presentMaxSec> <absentMinSec> <absentMaxSec> <tradeMin> <tradeMax> <rndXZMax> <rndYPosMax> <rndYNegMax> <rndTryMax>
/npc travel disable <npcId>
/npc travel force_move <npcId>
```

Behavior:
- On first enable, the NPC must have at least one trade. Its current location becomes the centre point of the travel plan, and its existing `trades` list becomes the "complete" trade pool (`completeTradeIdList`).
- Each cycle: the NPC is "present" for a uniformly random time in `[presentMinSec, presentMaxSec]`, then "absent" for a uniformly random time in `[absentMinSec, absentMaxSec]`.
- While present, it picks between `tradeMin` and `tradeMax` trades at random from the complete pool. When it arrives, a random legal ground position is chosen within `centralX ± rndXZMax`, `centralZ ± rndXZMax`, and `Y ∈ [centralY − rndYNegMax, centralY + rndYPosMax]`, with up to `rndTryMax` attempts; if none succeeds, the centre point is used.
- Arrival/departure fire portal/cloud particles and villager sounds, plus a broadcast (see config).
- `disable` restores the NPC to its centre point and makes all pool trades available.
- `force_move` schedules the next state transition as "now" (`nextMovementTime = now − 1ms`).
- While a traveller is due to move, interacting with it shows "its time to move" and does not open the merchant view.

### HEH Shop (Deprecated)
```text
/npc hehshop
/npc hehshop remove
/npc my
```
HEH integration is disabled in this build:
- `/npc hehshop` still attempts the flow but `ExternalPluginUtils.hehOpenPlayerShop` always throws and the interaction ends in "HEH not supported".
- `HEH_SELL_SHOP` NPCs are pruned from `npcs.yml` on every plugin load.
- `/npc my` lists NPCs owned by the calling player (still functional and useful for auditing any NPCs you own).

## Configuration (`config.yml`)
All fields are optional and fall back to the defaults shown below.

| Key | Default | Description |
|-----|---------|-------------|
| `language` | `en_US` | i18n language code; resources `lang/en_US.yml` and `lang/zh_CN.yml` are bundled. |
| `allowedEntityType` | auto-populated from all spawnable + alive entity types plus `PLAYER` | Whitelist enforced by `/npc spawn` and `entitytype:` edits. |
| `tabListDelay` | `15` | Ticks that player NPCs remain in the tab list after being sent. |
| `playerNpcLimit` | `6` | Maximum NPCs per player owner for `/npc hehshop` (still enforced even though the shop is disabled). |
| `travel_merchant.distance_check` | `50` | Radius (blocks) around a travelling NPC inside which no player may be for it to move. |
| `travel_merchant.broadcast_range` | `50` | Broadcast radius for arrival/departure messages. |
| `travel_merchant.message_arrival` | `&aTraveling merchant &r{merchant.name}&a has arrived.` | Arrival message template. Supports `&` colors and `&#RRGGBB` hex. |
| `travel_merchant.message_depart` | `&aTraveling merchant &r{merchant.name}&a has departed.` | Departure template. |

Both messages support the `{merchant.name}` placeholder.

## Importing from Shopkeepers
```text
/npc import
```
Place the Shopkeepers plugin's `save.yml` into `plugins/NyaaPlayerCoser/` first. Entries of type `admin` are imported into `npcs.yml` / `trades.yml`; unsupported or broken entries are copied into `bad-save.yml`. After import the plugin forces a full respawn of all NPCs.

## Permissions
From `plugin.yml`:
- `npc.admin` (default: `op`) — umbrella; children: `npc.player`, `npc.command.inspect`, `npc.command.import`, `npc.command.reload`, `npc.command.skin`, `npc.command.spawn`, `npc.command.remove`, `npc.command.edit`.
- `npc.player` (default: `op`) — children: `npc.command`, `npc.command.hehshop`.
- `npc.command` — base permission for using `/npc`.
- `npc.command.hehshop` — `/npc hehshop`, `/npc my`.
- `npc.interact` (default: `true`) — required to right-click any NPC. Without this permission the interaction silently no-ops.
- `npc.debug` (default: `false`) — required for `/npc debug`.

`/npc travel` uses `npc.command.edit`.

## Troubleshooting
- **Player NPCs invisible:** make sure ProtocolLib is installed and is the dev-build compatible with 1.21.11. Only player-typed NPCs require it; living-entity NPCs work without ProtocolLib but the HEH interaction packet listener will still be installed.
- **"invalid npc config, skipping" on load:** usually an unknown `entityType` value in `npcs.yml`. Fix the file or remove the entry.
- **Trade item normalization logs grow fast:** this is expected when plugins change item NBT formats across updates. You can rotate or truncate `item-update.log` safely at any time.

## Version History
- 8.2.x — Minecraft 1.21.11 (Paper 1.21.11, Java 21, NyaaCore 9.10). Introduced automatic trade-item NBT normalization with `item-update.log`, container-like items skipped during normalization, `rpgitem_ignore` scoreboard tag, stricter player-NPC packet handling for 1.21.x, removal of deprecated `HEH_SELL_SHOP` NPCs on config load, and a travel-plan null-world guard.
- 8.1.x — Minecraft 1.17.x (Java 16, NyaaCore 8.1, ProtocolLib 4.7). Added the `COMMAND` NPC type and Gradle 7 build.
- 7.1.x — Minecraft 1.15.1, since build 17.
- 7.0.x — Minecraft 1.14.4, since build 8.
