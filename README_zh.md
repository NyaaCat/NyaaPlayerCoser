# NyaaPlayerCoser [![Build Status](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/1.17/badge/icon)](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/1.17/)

NyaaPlayerCoser 是 NyaaCraft 服务器使用的 NPC 插件，支持交易商人、指令执行、玩家皮肤 NPC 以及旅行商人功能。

## 依赖
- Paper/Spigot 1.13+（api-version 1.13）
- NyaaCore
- ProtocolLib
- 可选：HamsterEcoHelper（集成已禁用，HEH 相关命令保留但不会打开商店）

## 数据文件
- `config.yml` - 主配置
- `npcs.yml` - NPC 定义
- `trades.yml` - 交易定义
- `skins.yml` - 皮肤定义
- `item-update.log` - 交易材料自动规范化的更新记录

## 快速开始
1) 创建 NPC。
2) 用快捷栏 1-3 定义交易。
3) 与 NPC 交互进行交易。

```text
/npc spawn VILLAGER TRADER_UNLIMITED "&aTrader"
# 把 item1 放在快捷栏 1、item2 放在快捷栏 2（可空）、result 放在快捷栏 3
/npc edit <npcId> trade:+
```

## NPC 类型
- `TRADER_UNLIMITED`：无限次数商人交易，数据来自 `trades.yml`。
- `TRADER_BOX`：预留给箱子绑定交易（GUI 未实现）。
- `COMMAND`：交互时执行配置的指令。
- `HEH_SELL_SHOP`：已废弃；加载时会被移除，且集成已禁用。

## 交易行为
- 交易定义为 `item1 + item2 -> result`。
- 匹配只比较物品类型、纯文本名称和 Lore。
- 打开商人时，插件可能会对玩家背包内与交易材料匹配的物品进行格式规范化，并记录到 `item-update.log`。
- 容器类物品（潜影盒、束口袋、或带内部库存的物品）会被跳过，避免覆盖玩家内容。

## 指令
### 基础
```text
/npc reload
/npc list
/npc inspect nearby [range]
/npc inspect npc <npcId>
/npc inspect trade <tradeId>
/npc debug
```

### 创建与编辑
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

说明：
- `displayName` 支持 `&` 颜色和 `&#RRGGBB` 十六进制颜色。
- `entityType` 必须可生成/存活，或为 `PLAYER`。
- `nbt` 为原始实体 NBT 字符串。
- `trade:+` 从快捷栏 1-3 读取交易（索引 0-2）。快捷栏 2 可空以省略 `item2`。

### 交易编辑
```text
/npc edit_trade <tradeId>
```
`edit_trade` 会用快捷栏 1-3 覆盖该交易。

### 指令 NPC
- `command_permission` 行为：
  - `console` 以控制台执行
  - `*` 以玩家临时 OP 执行
  - 其他值视为 `;` 分隔的权限列表，例如 `perm.one;perm.two`
- 指令占位符：
  - `{player}`, `{player.x}`, `{player.y}`, `{player.z}`
  - `{player.yaw}`, `{player.pitch}`, `{yaw}`, `{pitch}`

### 皮肤
```text
/npc skin add <skinId> <texture_value> <texture_signature> [description]
/npc skin pin <playerName> [follow] [skinId]
/npc skin list
/npc skin setdefault <skinId>
/npc edit <npcId> skin:<skinId>
/npc edit <npcId> skin:default
```

说明：
- 纹理值/签名可从 https://mineskin.org/ 获取。
- `skin pin` 会保存玩家当前皮肤；`follow` 记录 UUID 以兼容使用。

### 旅行商人
```text
/npc travel enable <npcId> <presentMinSec> <presentMaxSec> <absentMinSec> <absentMaxSec> <tradeMin> <tradeMax> <rndXZMax> <rndYPosMax> <rndYNegMax> <rndTryMax>
/npc travel disable <npcId>
/npc travel force_move <npcId>
```

行为说明：
- 首次启用时以 NPC 当前坐标作为中心点。
- NPC 出现时会从完整交易列表中随机选取一部分交易。
- 到达/离开广播消息与范围由 `config.yml` 控制。

### HEH 商店（已废弃）
```text
/npc hehshop
/npc hehshop remove
/npc my
```

说明：
- HamsterEcoHelper 集成已禁用，HEH 商店不会打开，且 HEH NPC 会在加载时移除。

## 配置
`config.yml` 字段：
- `language`：语言代码（默认 `en_US`）
- `allowedEntityType`：允许的 `EntityType` 列表
- `tabListDelay`：玩家 NPC 在 Tab 列表中停留的 ticks
- `playerNpcLimit`：`/npc hehshop` 的 NPC 数量上限
- `travel_merchant.distance_check`：旅行商人存在检查范围
- `travel_merchant.broadcast_range`：到达/离开广播范围
- `travel_merchant.message_arrival`：到达提示消息模板
- `travel_merchant.message_depart`：离开提示消息模板

## 从 Shopkeepers 导入
```text
/npc import
```
将 Shopkeepers 的 `save.yml` 放到 NyaaPlayerCoser 数据目录。导入数据保存到 `npcs.yml` 与 `trades.yml`，失败项会写入 `bad-save.yml`。

## 权限
- `npc.command`：基础命令权限
- `npc.command.spawn`, `npc.command.remove`, `npc.command.edit`, `npc.command.inspect`, `npc.command.reload`, `npc.command.import`, `npc.command.skin`
- `npc.command.hehshop`：玩家 HEH 商店命令
- `npc.interact`：交互 NPC 所需权限
- `npc.admin`：管理员权限集合
- `npc.player`：玩家权限集合
- `npc.debug`：调试开关

## 备注
- 玩家 NPC 需要 ProtocolLib 才能在客户端正常显示。
- TRADER_BOX 目前只保留数据结构，交易 GUI 未实现。
- HEH 商店功能保留命令但不可用。

## 版本历史
- 7.1.x: for Minecraft 1.15.1, since build 17
- 7.0.x: for Minecraft 1.14.4, since build 8
