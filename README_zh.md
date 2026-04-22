# NyaaPlayerCoser [![Build Status](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/version%252F1.21.11/badge/icon)](https://ci.nyaacat.com/job/NyaaPlayerCoser/job/version%252F1.21.11/)

NyaaPlayerCoser 是 NyaaCraft 服务器使用的 NPC 插件，提供：商人交易、指令执行、玩家皮肤 NPC（通过数据包渲染）以及旅行商人功能。

[English README](README.md)

## 依赖
- Paper 1.21.11（插件基于 `paper-api:1.21.11-R0.1-SNAPSHOT` 编译）
- Java 21
- NyaaCore 9.10+
- ProtocolLib（dev-build，支持 1.21.11 数据包所必需）
- 可选：HamsterEcoHelper —— 集成**当前已禁用**。加载时会从 `npcs.yml` 中删除 `HEH_SELL_SHOP` NPC，`/npc hehshop` 相关命令仍存在但不会打开商店。

> 说明：`plugin.yml` 中仍声明 `api-version: 1.13` 只是为兼容老旧校验；代码使用 Paper 1.21.11 API，无法运行在更低版本的服务端。

## 构建
```bash
./gradlew build
```
构建会自动下载 ProtocolLib dev-build 到 `libs/ProtocolLib.jar`。产物：`build/libs/NyaaPlayerCoser-mc1.21.11-8.2.*.jar`。

## 数据文件
全部位于 `plugins/NyaaPlayerCoser/` 下：
- `config.yml` —— 主配置（见下表；默认内容为空，使用默认值即可）。
- `npcs.yml` —— NPC 定义。
- `trades.yml` —— 交易定义。
- `skins.yml` —— 皮肤定义。
- `item-update.log` —— 交易材料规范化审计日志（详见下文）。
- `save.yml` / `bad-save.yml` —— 供 `/npc import`（Shopkeepers 导入源 / 失败项输出）。

## 快速开始
1. 瞄准一个方块，其上方至少有 2 格空气。
2. 生成 NPC：
   ```text
   /npc spawn VILLAGER TRADER_UNLIMITED "&aTrader"
   ```
3. 把 `item1` 放到快捷栏 1、`item2` 放到快捷栏 2（可留空，表示单材料交易）、`result` 放到快捷栏 3。
4. 定义交易：
   ```text
   /npc edit <npcId> trade:+
   ```
5. 右键点击 NPC 进行交易。

## NPC 类型
- `TRADER_UNLIMITED` —— 无限次数商人交易，数据来自 `trades.yml`。
- `TRADER_BOX` —— 预留给箱子绑定交易。enum 和数据字段存在，但没有实现 GUI 流程（`/npc spawn` 禁止使用；打开时会提示 "type not supported"）。
- `COMMAND` —— 交互时执行配置的指令。
- `HEH_SELL_SHOP` —— **已废弃，加载时会被移除**。当前版本已禁用 HamsterEcoHelper 集成。

## NPC 实体
生成出来的实体会带有两个记分板标签：
- `nyaa_npc_id:<npcId>` —— 标识该实体为本插件的 NPC 并保存其 id。
- `rpgitem_ignore` —— 让 RPGItems-reloaded 跳过对该 NPC 的伤害/效果处理。

NPC 实体不受伤害、保持静默、不拾取物品、关闭 AI，并通过名为 `npc:immobile_entity` 的属性修饰符把移动速度锁定为 0。玩家皮肤类型的 NPC 完全通过 ProtocolLib 数据包渲染，必须安装 ProtocolLib。

## 交易行为
- 每条交易保存为 `item1 (+ item2) -> result`。`item2` 可为 AIR，表示单材料交易。
- 物品匹配比较**类型 + 纯文本显示名 + 纯文本 Lore**（`ItemStackUtils.isSimilarPlainText`），忽略其他 NBT。
- 玩家与交易 NPC 交互打开交易界面时，插件会扫描玩家背包，把与交易材料（`item1` / `item2`）匹配的物品规范化为当前版本的标准 NBT 形式。每次更新都写入 `item-update.log` 一行。
  - 规范化流程：主线程抓快照 → 异步线程比对差异 → 主线程应用更新。
  - 若同一 NPC 的多条交易对同一物品的标准形式不一致（基线冲突），该物品会被跳过。
  - **容器类物品一律跳过**，避免覆盖玩家物品内容：潜影盒（Tag.SHULKER_BOXES）、束口袋（`BundleMeta`）、以及所有 `BlockStateMeta` 为 `Container` 的物品。

## 指令
主命令为 `/nyaaplayercoser`，别名 `/npc`。

### 基础
```text
/npc reload
/npc list
/npc inspect nearby [range]
/npc inspect npc <npcId>
/npc inspect trade <tradeId>
/npc debug
```
- `inspect nearby` 会返回玩家光标附近（约 6 格内）的 NPC；若给出 `range`，还会列出以玩家为中心、边长为 `range` 的立方体内所有配置中的 NPC。
- `inspect trade` 会打印交易数据，并在玩家脚下丢出 `item1`、`item2`、`result` 以便检查。
- `debug` 切换插件调试日志开关（`NyaaPlayerCoser.debugEnabled`）。

### 创建与编辑
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

说明：
- `/npc spawn` 通过眼部射线追踪选中方块；该方块必须存在且**上方至少有 2 格空气**。NPC 会生成在该方块的顶面中心。
- `displayName` 支持 `&` 颜色和 `&#RRGGBB` 十六进制颜色。
- `entityType` 必须是既 spawnable 又 alive 的类型，或为 `PLAYER`。若为 `PLAYER`，`displayName` 会作为玩家名，长度必须介于 3 到 16 之间。
- `npctype` 在 spawn/edit 时仅允许 `TRADER_UNLIMITED`、`COMMAND`、`HEH_SELL_SHOP`（`HEH_SELL_SHOP` 可设置但该 NPC 不会工作）。
- `nbt` 为原始实体 NBT 字符串，在 spawn 之后应用（例如 `{Profession:1}` 给村民设定职业）。
- `trade:+` 从快捷栏 1、2、3（背包索引 0、1、2）读取交易。快捷栏 2 可留空，表示省略 `item2`。
- `trade:<id1>,<id2>,...` 会整体替换交易列表；所有列出的 id 必须已存在于 `trades.yml`。
- 任何修改都会触发 `NpcRedefinedEvent`，关闭该 NPC 的所有正在打开的交易界面。

### 交易编辑
```text
/npc edit_trade <tradeId>
```
用快捷栏 1-3 的物品覆盖 `<tradeId>` 对应的交易。所有引用该交易的 NPC 的已打开交易界面都会被关闭（`TradeRedefinedEvent`）。

### 指令 NPC
`command_permission` 行为：
- `console` —— 以控制台身份执行。
- `*` —— 临时给玩家 OP 权限后以玩家身份执行（`finally` 中撤销）。
- 其他值 —— 视为 `;` 分隔的权限列表（如 `perm.one;perm.two`）。每个权限及其所有点号前缀都会通过一个仅一 tick 的 `PermissionAttachment` 临时授予，命令执行完成后立即撤销。

指令中可用的占位符：
- `{player}` —— 玩家名。
- `{player.x}`, `{player.y}`, `{player.z}` —— 玩家坐标。
- `{player.yaw}` —— 眼部 yaw + 90。
- `{player.pitch}` —— 眼部 pitch 取负。
- `{yaw}` —— 身体 yaw + 90。
- `{pitch}` —— 身体 pitch 取负。

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
- 纹理值 / 签名可从 https://mineskin.org/ 获取，或从任意在线玩家的 `textures` 属性读取。
- `skin pin <playerName>` 会把该玩家当前的纹理复制成新的 `SkinData`。若 `follow` 为 `true`，玩家 UUID 会写入 `followPlayer` 字段以供下游功能使用。若省略 `skinId`，皮肤保存为 `<playerName>`。
- 皮肤只对 `entityType == PLAYER` 的 NPC 生效。

### 旅行商人
```text
/npc travel enable <npcId> <presentMinSec> <presentMaxSec> <absentMinSec> <absentMaxSec> <tradeMin> <tradeMax> <rndXZMax> <rndYPosMax> <rndYNegMax> <rndTryMax>
/npc travel disable <npcId>
/npc travel force_move <npcId>
```

行为说明：
- 首次启用时，NPC 必须已经至少有一条交易。NPC 当前坐标会作为行程的中心点，其已有的 `trades` 列表会成为"完整"交易池（`completeTradeIdList`）。
- 每个循环：NPC "present（出现）"时长在 `[presentMinSec, presentMaxSec]` 内均匀随机；"absent（离开）"时长在 `[absentMinSec, absentMaxSec]` 内均匀随机。
- 出现期间，从完整交易池随机挑选 `tradeMin` 到 `tradeMax` 条交易。到达时会在 `centralX ± rndXZMax`、`centralZ ± rndXZMax`、`Y ∈ [centralY − rndYNegMax, centralY + rndYPosMax]` 范围内寻找一个合法的地面位置，最多尝试 `rndTryMax` 次；若全部失败则回退到中心点。
- 到达 / 离开时会播放传送门 / 云朵粒子与村民音效，并按配置向附近玩家广播消息。
- `disable` 会把 NPC 放回中心点，并把完整交易池设为可用交易。
- `force_move` 会把下一次状态切换时间设为"立刻"（`nextMovementTime = now − 1ms`）。
- 当旅行 NPC 即将进入切换期时，与之交互会提示 "its time to move"，不会打开交易界面。

### HEH 商店（已废弃）
```text
/npc hehshop
/npc hehshop remove
/npc my
```
当前版本已禁用 HEH 集成：
- `/npc hehshop` 仍然会走流程，但 `ExternalPluginUtils.hehOpenPlayerShop` 总是抛出异常，最终提示 "HEH not supported"。
- 每次插件加载都会从 `npcs.yml` 中清理 `HEH_SELL_SHOP` NPC。
- `/npc my` 列出调用者拥有的全部 NPC（仍可用，方便盘点自己所有的 NPC）。

## 配置（`config.yml`）
所有字段都可省略，缺省时使用下表中的默认值。

| 键 | 默认值 | 说明 |
|----|--------|------|
| `language` | `en_US` | 语言代码；插件自带 `lang/en_US.yml` 和 `lang/zh_CN.yml`。 |
| `allowedEntityType` | 启动时自动填入所有 spawnable + alive 的实体类型，外加 `PLAYER` | `/npc spawn` 与 `entitytype:` 编辑会强制校验。 |
| `tabListDelay` | `15` | 玩家 NPC 在 Tab 列表中保留的 ticks。 |
| `playerNpcLimit` | `6` | `/npc hehshop` 每位玩家的 NPC 数量上限（商店禁用后仍会校验）。 |
| `travel_merchant.distance_check` | `50` | 旅行 NPC 移动前，中心半径（方块）内不能有玩家。 |
| `travel_merchant.broadcast_range` | `50` | 到达 / 离开消息广播半径。 |
| `travel_merchant.message_arrival` | `&aTraveling merchant &r{merchant.name}&a has arrived.` | 到达消息模板。支持 `&` 颜色与 `&#RRGGBB` 十六进制。 |
| `travel_merchant.message_depart` | `&aTraveling merchant &r{merchant.name}&a has departed.` | 离开消息模板。 |

两个消息都支持 `{merchant.name}` 占位符。

## 从 Shopkeepers 导入
```text
/npc import
```
执行前先把 Shopkeepers 的 `save.yml` 放到 `plugins/NyaaPlayerCoser/` 目录。`type: admin` 的条目会被导入到 `npcs.yml` / `trades.yml`；不支持或损坏的条目会被写入 `bad-save.yml`。导入完成后插件会强制重新生成所有 NPC。

## 权限
来自 `plugin.yml`：
- `npc.admin`（默认 `op`）—— 管理员权限集合；子权限：`npc.player`、`npc.command.inspect`、`npc.command.import`、`npc.command.reload`、`npc.command.skin`、`npc.command.spawn`、`npc.command.remove`、`npc.command.edit`。
- `npc.player`（默认 `op`）—— 子权限：`npc.command`、`npc.command.hehshop`。
- `npc.command` —— 使用 `/npc` 的基础权限。
- `npc.command.hehshop` —— `/npc hehshop`、`/npc my`。
- `npc.interact`（默认 `true`）—— 右键 NPC 所需。缺少此权限时交互会被静默忽略。
- `npc.debug`（默认 `false`）—— 使用 `/npc debug` 所需。

`/npc travel` 使用 `npc.command.edit`。

## 常见问题
- **玩家 NPC 不可见**：确认已安装 ProtocolLib 且为兼容 1.21.11 的 dev-build。只有 `PLAYER` 类型的 NPC 需要 ProtocolLib；其它生物 NPC 不依赖它，但 HEH 交互数据包监听器仍会被注册。
- **启动时日志 "invalid npc config, skipping"**：通常是 `npcs.yml` 中出现了未知 `entityType`。修复或删除该条目即可。
- **`item-update.log` 增长很快**：当其他插件改变物品 NBT 格式时属于正常现象，可随时轮转或清空该文件。

## 版本历史
- 8.2.x —— Minecraft 1.21.11（Paper 1.21.11、Java 21、NyaaCore 9.10）。新增交易材料 NBT 自动规范化与 `item-update.log`、容器类物品跳过、`rpgitem_ignore` 记分板标签、针对 1.21.x 的玩家 NPC 数据包更严格的处理、加载时移除废弃的 `HEH_SELL_SHOP` NPC、旅行计划对空世界的空值保护。
- 8.1.x —— Minecraft 1.17.x（Java 16、NyaaCore 8.1、ProtocolLib 4.7）。新增 `COMMAND` NPC 类型，使用 Gradle 7 构建。
- 7.1.x —— Minecraft 1.15.1, since build 17。
- 7.0.x —— Minecraft 1.14.4, since build 8。
