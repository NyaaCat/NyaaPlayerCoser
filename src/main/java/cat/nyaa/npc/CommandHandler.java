package cat.nyaa.npc;

import cat.nyaa.npc.ephemeral.NPCBase;
import cat.nyaa.npc.ephemeral.NyaaMerchant;
import cat.nyaa.npc.persistence.*;
import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.ClickSelectionUtils;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import cat.nyaa.nyaacore.utils.RayTraceUtils;
import com.google.common.collect.Iterables;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.entity.EntityType.PLAYER;

public class CommandHandler extends CommandReceiver {
    private final NyaaPlayerCoser plugin;

    public CommandHandler(NyaaPlayerCoser plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }

    @SubCommand(value = "skin", permission = "npc.command.skin")
    public CommandHandlerSkin commandSkin;

    @SubCommand(value = "reload", permission = "npc.command.reload")
    public void reloadCommand(CommandSender sender, Arguments args) {
        plugin.onReload();
    }

    @SubCommand(value = "import", permission = "npc.command.import")
    public void importFromShopkeeper(CommandSender sender, Arguments args) {
        File file = new File(plugin.getDataFolder(), "save.yml");
        if (file.isFile()) {
            DataImporter.importShopkeeper(plugin, sender);
        } else {
            sender.sendMessage("save.yml not found, please put in plugin data folder.");
        }
    }

    @SubCommand(value = "spawn", permission = "npc.command.spawn")
    public void newNpcDefinition(CommandSender sender, Arguments args) {
        EntityType entityType = args.nextEnum(EntityType.class);
        if (!plugin.cfg.isAllowedType(entityType)) {
            throw new BadCommandException("user.spawn.type_disallow", entityType.name());
        }
        NpcType npctype = args.nextEnum(NpcType.class);
        if (npctype != NpcType.TRADER_UNLIMITED && npctype != NpcType.HEH_SELL_SHOP) {
            throw new BadCommandException("user.spawn.npctype_disallow", npctype.name());
        }

        String name = ChatColor.translateAlternateColorCodes('&', args.nextString());
        String entitydataTag = args.next();
        if (entitydataTag == null) entitydataTag = "";

        Block b = getRayTraceBlock(sender);
        if (b == null || b.getType() == Material.AIR) {
            throw new BadCommandException("user.spawn.not_block");
        }
        if (b.getRelative(BlockFace.UP).getType().isSolid() || b.getRelative(0, 2, 0).getType().isSolid()) {
            throw new BadCommandException("user.spawn.not_enough_space");
        }

        if (entityType == PLAYER && (name.length() > 16 || name.length() < 3)) {
            msg(sender, "user.playername_length_error");
            return;
        }

        NpcData data = new NpcData(
                asPlayer(sender).getUniqueId(),
                b.getLocation().clone().add(.5, /* TODO: NmsUtils.getBlockHeight(b) */ 1, .5),
                name, entityType, npctype, entitydataTag);
        data.hehShopOwnerUUID = data.ownerId;
        String npcId = plugin.entitiesManager.createNpcDefinition(data);
        msg(sender, "user.spawn.id_created", npcId);
    }

    @SubCommand(value = "remove", permission = "npc.command.remove")
    public void removeNpcDefinition(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        asNpcData(npcId);
        plugin.entitiesManager.removeNpcDefinition(npcId);
    }

    @SubCommand(value = "edit", permission = "npc.command.edit")
    public void editNpc(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        if ("help".equals(npcId)) {
            msg(sender, "user.edit.help_msg");
            return;
        }

        NpcData data = asNpcData(npcId).clone();
        boolean modified = false;

        String newName = args.argString("name", null);
        if (newName != null) {
            if (data.entityType == PLAYER && (newName.length() > 16 || newName.length() < 3)) {
                msg(sender, "user.playername_length_error");
                return;
            }

            data.displayName = ChatColor.translateAlternateColorCodes('&', newName);
            modified = true;
        }

        String newNpcType = args.argString("npctype", null);
        if (newNpcType != null) {
            NpcType newType = Arguments.parseEnum(NpcType.class, newNpcType);
            if (newType != NpcType.HEH_SELL_SHOP && newType != NpcType.TRADER_UNLIMITED) {
                msg(sender, "user.spawn.npctype_disallow", newType.name());
                return;
            } else {
                if (newType == NpcType.HEH_SELL_SHOP && data.hehShopOwnerUUID == null) {
                    data.hehShopOwnerUUID = data.ownerId;
                }
                data.npcType = newType;
                modified = true;
            }
        }

        String newEntityType = args.argString("entitytype", null);
        if (newEntityType != null) {
            EntityType et = Arguments.parseEnum(EntityType.class, newEntityType);
            if (!plugin.cfg.isAllowedType(et)) {
                msg(sender, "user.spawn.type_disallow", et.name());
            } else {

                if (et == PLAYER && (data.displayName.length() > 16 || data.displayName.length() < 3)) {
                    msg(sender, "user.playername_length_error");
                    return;
                }

                data.entityType = et;
                modified = true;
            }
        }

        String owner = args.argString("owner", null);
        if (owner != null) {
            try {
                data.ownerId = UUID.fromString(owner);
                modified = true;
            } catch (IllegalArgumentException ex) {
                msg(sender, "user.edit.invalid_uuid", owner);
                return;
            }
        }

        String hehowner = args.argString("hehowner", null);
        if (owner != null) {
            try {
                data.hehShopOwnerUUID = UUID.fromString(hehowner);
                modified = true;
            } catch (IllegalArgumentException ex) {
                msg(sender, "user.edit.invalid_uuid", hehowner);
                return;
            }
        }

        String nbt = args.argString("nbt", null);
        if (nbt != null) {
            data.nbtTag = nbt;
            modified = true;
        }

        String skinId = args.argString("skin", null);
        if (skinId != null) {
            if (data.entityType != PLAYER) {
                msg(sender, "user.edit.skin_entity_not_player");
            }
            if (skinId.equals("")) {
                data.playerSkin = "default";
            } else if (plugin.cfg.skinData.hasSkinData(skinId)) {
                data.playerSkin = skinId;
            } else {
                msg(sender, "user.edit.skin_notfound", skinId);
                return;
            }
            modified = true;
        }

        String loc = args.argString("location", null);
        if (loc != null) {
            if ("me".equals(loc)) {
                Location playerLoc = asPlayer(sender).getLocation();
                String w = playerLoc.getWorld().getName();
                double x = Math.floor(playerLoc.getX()) + 0.5;
                double y = playerLoc.getY();
                double z = Math.floor(playerLoc.getZ()) + 0.5;
                data.worldName = w;
                data.x = x;
                data.y = y;
                data.z = z;
                modified = true;
            } else {
                msg(sender, "user.edit.invalid_location");
                return;
            }
        }

        String trade_op = args.argString("trade", null);
        if (trade_op != null) {
            if ("+".equals(trade_op)) {
                ItemStack itemStack1 = getItemStackInSlot(sender, 0, false);
                ItemStack itemStack2 = getItemStackInSlot(sender, 1, true);
                ItemStack result = getItemStackInSlot(sender, 2, false);
                TradeData td = new TradeData(itemStack1, itemStack2, result);
                String tradeId = plugin.cfg.tradeData.addTrade(td);
                msg(sender, "user.edit.trade_id_created", tradeId);
                data.trades.add(tradeId);
            } else if (trade_op.startsWith("+")) {
                String trade_id = trade_op.substring(1);
                if (plugin.cfg.tradeData.tradeList.containsKey(trade_id)) {
                    data.trades.add(trade_id);
                } else {
                    msg(sender, "user.edit.trade_id_notfound", trade_id);
                    return;
                }
            } else if (trade_op.startsWith("-")) {
                String trade_id = trade_op.substring(1);
                if (data.trades.contains(trade_id)) {
                    data.trades.remove(trade_id);
                } else {
                    msg(sender, "user.edit.trade_id_notfound", trade_id);
                    return;
                }
            } else {
                msg(sender, "user.edit.trade_op_invalid");
                return;
            }
            modified = true;
        }

        if (modified) {
            plugin.entitiesManager.replaceNpcDefinition(npcId, data);
            msg(sender, "user.edit.updated");
        } else {
            msg(sender, "user.edit.not_updated");
        }
    }

    private static LivingEntity _selectCursorEntity(Player p) {
        double minAngle = Math.PI / 2D;
        LivingEntity ret = null;
        for (Entity e : p.getNearbyEntities(6, 6, 6)) {
            if (!NPCBase.isNyaaNPC(e)) continue;
            LivingEntity currentMobEntity = (LivingEntity) e;
            Vector eyeSight = p.getEyeLocation().getDirection();
            Vector mobVector = currentMobEntity.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
            double angle = getVectorAngle(eyeSight, mobVector);
            if (!Double.isFinite(angle)) continue;
            if (angle >= minAngle) continue;
            minAngle = angle;
            ret = currentMobEntity;
        }
        return ret;
    }

    private NPCBase getLookatNPC(Location eyeLocation) {
        NPCBase candidate = null;
        double minAngle = Math.PI/4D; // 45 degrees
        Vector eyeSight = eyeLocation.getDirection();

        for (NPCBase npc : plugin.entitiesManager.idNpcMapping.values()) {
            Location npcHeadLocation = npc.getEyeLocation();
            if (npcHeadLocation == null) continue;
            Vector mobVector = npcHeadLocation.toVector().subtract(eyeLocation.toVector());
            double angle = getVectorAngle(eyeSight, mobVector);
            if (!Double.isFinite(angle)) continue;
            if (angle >= minAngle) continue;
            minAngle = angle;
            candidate = npc;
        }
        return candidate;
    }

    private String _shortNpcDescription(String npcId) {
        if (npcId == null) return "{id=<null>}";
        NpcData data = plugin.cfg.npcData.npcList.get(npcId);
        if (data == null) return String.format("{id=%s, data=<null>}", npcId);
        return String.format("{id=%s, name=%s, npctype=%s, entitytype=%s}", npcId, data.displayName, data.npcType, data.entityType);
    }

    @SubCommand(value = "inspect", permission = "npc.command.inspect")
    public void inspectData(CommandSender sender, Arguments args) {
        String subcommand = args.nextString();

        if ("nearby".equalsIgnoreCase(subcommand)) {
            int scanRange = -1;
            if (args.top() != null) {
                scanRange = args.nextInt();
            }

            LivingEntity cursorEntity = _selectCursorEntity(asPlayer(sender));
            String id = NPCBase.getNyaaNpcId(cursorEntity);
            if (id != null) {
                msg(sender, "user.inspect.nearby.cursor", _shortNpcDescription(id));
            } else {
                msg(sender, "user.inspect.nearby.no_cursor");
            }

            if (scanRange > 0) {
                BoundingBox box = BoundingBox.of(asPlayer(sender).getLocation(), scanRange, scanRange, scanRange);
                String w = asPlayer(sender).getLocation().getWorld().getName();
                boolean found = false;
                for (Map.Entry<String, NpcData> entry : plugin.cfg.npcData.npcList.entrySet()) {
                    NpcData d = entry.getValue();
                    if (w.equals(d.worldName) && box.contains(d.x, d.y, d.z)) {
                        msg(sender, "user.inspect.nearby.found", _shortNpcDescription(entry.getKey()));
                        found = true;
                    }
                }
                if (!found) {
                    msg(sender, "user.inspect.nearby.not_found");
                }
            }
        } else if ("npc".equalsIgnoreCase(subcommand)) {
            String npcId = args.nextString();
            NpcData data = asNpcData(npcId);

            // basic info
            msg(sender, "user.inspect.npc.msg_id", npcId);
            msg(sender, "user.inspect.npc.msg_name", data.displayName);
            msg(sender, "user.inspect.npc.msg_entitytype", data.entityType);
            msg(sender, "user.inspect.npc.msg_npctype", data.npcType);
            msg(sender, "user.inspect.npc.msg_loc",
                    String.format("[world=%s, x=%.2f, y=%.2f, z=%.2f]", data.worldName, data.x, data.y, data.z));
            msg(sender, "user.inspect.npc.msg_chest_loc",
                    String.format("[world=%s, x=%d, y=%d, z=%d]", data.chestWorldName, data.chestX, data.chestY, data.chestZ));
            msg(sender, "user.inspect.npc.msg_nbt", data.nbtTag);

            String ownerName = data.ownerId == null ? null : Bukkit.getOfflinePlayer(data.ownerId).getName();
            String hehOwnerName = data.hehShopOwnerUUID == null ? null : Bukkit.getOfflinePlayer(data.hehShopOwnerUUID).getName();
            msg(sender, "user.inspect.npc.msg_owner", data.ownerId, ownerName);
            msg(sender, "user.inspect.npc.msg_hehowner", data.hehShopOwnerUUID, hehOwnerName);

            // trade data
            if (data.trades.size() > 0) {
                msg(sender, "user.inspect.npc.msg_trade_head");
                for (String tradeId : data.trades) {
                    TradeData td = plugin.cfg.tradeData.tradeList.get(tradeId);
                    if (td == null) {
                        msg(sender, "user.inspect.npc.msg_trade_notfound", tradeId);
                    } else {
                        td.appendDescription(new Message("  " + tradeId + ": ")).send(sender);
                    }
                }
            } else {
                msg(sender, "user.inspect.npc.msg_no_trade");
            }

            // ephemeral data
            NPCBase base = plugin.entitiesManager.idNpcMapping.get(npcId);
            if (base == null) {
                msg(sender, "user.inspect.npc.eph_notfound");
            } else {
                msg(sender, "user.inspect.npc.eph_entity", base.getUnderlyingSpawnedEntity());
                Set<NyaaMerchant> activeMerchants = plugin.tradingController.activeMerchants.get(npcId);
                if (activeMerchants != null && activeMerchants.size() > 0) {
                    msg(sender, "user.inspect.npc.eph_merchant_header");
                    for (NyaaMerchant m : activeMerchants) {
                        if (m == null) {
                            msg(sender, "user.inspect.npc.eph_merchant_item", null, null, null);
                        } else {
                            String traderName = m.getTrader() == null ? null : m.getTrader().getName();
                            msg(sender, "user.inspect.npc.eph_merchant_item", m, m.getTrader(), traderName);
                        }
                    }
                }
            }
        } else if ("trade".equalsIgnoreCase(subcommand)) {
            String id = args.nextString();
            TradeData tradeData = plugin.cfg.tradeData.tradeList.get(id);
            if (tradeData == null) {
                throw new BadCommandException("user.inspect.trade.no_trade", id);
            }

            if (tradeData.item1 != null)
                new Message(I18n.format("user.inspect.trade.item1")).append(tradeData.item1.clone()).send(sender);
            if (tradeData.item2 != null)
                new Message(I18n.format("user.inspect.trade.item2")).append(tradeData.item2.clone()).send(sender);
            if (tradeData.result != null)
                new Message(I18n.format("user.inspect.trade.result")).append(tradeData.result.clone()).send(sender);

            if (sender instanceof Player) {
                Player p = asPlayer(sender);
                if (tradeData.item1 != null && tradeData.item1.getType() != Material.AIR)
                    p.getLocation().getWorld().dropItem(p.getLocation(), tradeData.item1.clone());
                if (tradeData.item2 != null && tradeData.item2.getType() != Material.AIR)
                    p.getLocation().getWorld().dropItem(p.getLocation(), tradeData.item2.clone());
                if (tradeData.result != null && tradeData.result.getType() != Material.AIR)
                    p.getLocation().getWorld().dropItem(p.getLocation(), tradeData.result.clone());
                msg(p, "user.inspect.trade.item_given");
            }
        } else {
            throw new BadCommandException("user.inspect.invalid_command", subcommand);
        }
    }

    @SubCommand(value = "list", permission = "npc.command.inspect")
    public void listNpc(CommandSender sender, Arguments args) {
        if (plugin.cfg.npcData.npcList.size() == 0) {
            sender.sendMessage("No npc");
        } else {
            for (Map.Entry<String, NpcData> e : plugin.cfg.npcData.npcList.entrySet()) {
                sender.sendMessage(_shortNpcDescription(e.getKey()));
            }
        }
    }


    //@SubCommand(value = "chest", permission = "npc.admin.edit")
    public void editChestInfo(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        NpcData npc = asNpcData(npcId);
        if (npc.npcType != NpcType.TRADER_UNLIMITED && npc.npcType != NpcType.TRADER_BOX) {
            msg(sender, "user.chest.require_trader_type");
            return;
        }

        String action = args.next();
        if (action == null) { // print info
            Location chestLocation = npc.getChestLocation();
            msg(sender, "user.chest.status", chestLocation != null);
            msg(sender, "user.chest.unlimited", npc.npcType == NpcType.TRADER_UNLIMITED);
            if (chestLocation != null) {
                msg(sender, "user.chest.chest_pos", chestLocation.getWorld().getName(), chestLocation.getBlockX(), chestLocation.getBlockY(), chestLocation.getBlockZ());
                msg(sender, "user.chest.hint_unlink");
            } else {
                msg(sender, "user.chest.hint_link");
            }
        } else if ("unlink".equalsIgnoreCase(action)) {
            npc.setChestLocation(null);
            npc.npcType = NpcType.TRADER_UNLIMITED;
            plugin.entitiesManager.replaceNpcDefinition(npcId, npc);
            msg(sender, "user.chest.msg_unlinked");
        } else if ("link".equalsIgnoreCase(action)) {
            msg(sender, "user.chest.prompt_click");
            UUID playerId = asPlayer(sender).getUniqueId();
            ClickSelectionUtils.registerRightClickBlock(playerId, 15, (Location l) -> {
                // we are not in the same tick, re-validate everything
                Player p = Bukkit.getPlayer(playerId);
                if (p == null || !p.isOnline()) return;
                if (l == null) {
                    msg(p, "user.chest.timeout");
                    return;
                }
                NpcData npc2 = asNpcData(npcId);
                if (npc != npc2) {
                    msg(sender, "user.chest.npc_changed");
                    return;
                }
                Block b = l.getBlock();
                if (b == null) {
                    msg(sender, "user.chest.invalid_chest_block");
                    return;
                }
                Material m = b.getType();
                if (m != Material.CHEST && m != Material.TRAPPED_CHEST && m != Material.SHULKER_BOX) {
                    msg(sender, "user.chest.invalid_chest_block");
                    return;
                }
                npc.setChestLocation(l);
                npc.npcType = NpcType.TRADER_BOX;
                plugin.entitiesManager.replaceNpcDefinition(npcId, npc);
                msg(sender, "user.chest.msg_linked");
            }, plugin);
        }
    }

    //@SubCommand(value = "adjust_location", permission = "npc.admin.edit")
    public void adjustLocation(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        asNpcData(npcId);
        plugin.entitiesManager.adjustNpcLocation(npcId, sender);
    }

    //@SubCommand(value = "test", permission = "npc.admin")
    public void testCmd(CommandSender sender, Arguments args) {
        Player p = asPlayer(sender);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "small.yml"));
        ItemStack it = cfg.getItemStack("item");
        p.getLocation().getWorld().dropItem(p.getLocation(), it);
        p.sendMessage(ItemStackUtils.itemToJson(it));
        p.sendMessage(ItemStackUtils.itemToBase64(it));

        ItemMeta m = it.getItemMeta();
        m.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("name", 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        it.setItemMeta(m);
        p.sendMessage(ItemStackUtils.itemToJson(it));
        cfg.set("item", it);
        p.sendMessage(cfg.saveToString());


        // ItemStack sec = cfg.getItemStack("190.recipes.2.resultItem");
        // p.sendMessage(sec.toString());

        //DummyPlayer d = new DummyPlayer();
        //plugin.dummyController.sendPlayerInfoList(d, p);
    }


    private Block getRayTraceBlock(CommandSender sender) {
        return RayTraceUtils.rayTraceBlock(asPlayer(sender));
    }

    private boolean isChunkLoadedAtBlockCoordinate(World w, int x, int z) {
        return w.isChunkLoaded(x >> 4, z >> 4);
    }

    private ItemStack getItemStackInSlot(CommandSender sender, int slot, boolean allowEmpty) {
        Player p = asPlayer(sender);
        ItemStack s = p.getInventory().getItem(slot);
        if (s != null && s.getType() != Material.AIR) {
            return s;
        } else if (allowEmpty) {
            return new ItemStack(Material.AIR);
        } else {
            throw new BadCommandException("user.empty_slot", slot);
        }
    }

    private static double getVectorAngle(Vector v1, Vector v2) {
        double dot = v1.dot(v2);
        double normalProduct = v1.length() * v2.length();
        double cos = dot / normalProduct;
        return Math.acos(cos);
    }

    private NpcData asNpcData(String npcId) {
        if (npcId == null) {
            throw new BadCommandException("user.bad_id");
        }
        NpcData data = plugin.cfg.npcData.npcList.get(npcId);
        if (data == null) {
            throw new BadCommandException("user.bad_id");
        }
        return data;
    }


    /* ******************************************* */
    /* Specialized commands for unprivileged users */
    /* ******************************************* */
    @SubCommand(value = "hehshop", permission = "npc.command.hehshop")
    public void hehShopCommand(CommandSender sender, Arguments args) {
        if ("remove".equalsIgnoreCase(args.next())) {
            NPCBase npc = getLookatNPC(asPlayer(sender).getEyeLocation());
            if (npc == null) {
                msg(sender, "user.hehshop.remove_need_cursor");
                return;
            }

            String id = npc.id;
            NpcData data =npc.data;
            Player p = asPlayer(sender);
            UUID pid = p.getUniqueId();
            if (pid.equals(data.ownerId) && pid.equals(data.hehShopOwnerUUID) && data.npcType == NpcType.HEH_SELL_SHOP) {
                plugin.entitiesManager.removeNpcDefinition(id);
                msg(sender, "user.hehshop.remove_success");
            } else {
                msg(sender, "user.hehshop.remove_unprivileged");
            }
        } else {
            Player p = asPlayer(sender);
            Block b = getRayTraceBlock(sender);
            if (b == null || b.getType() == Material.AIR) {
                throw new BadCommandException("user.spawn.not_block");
            }

            Location spawnLocation = b.getLocation().clone().add(.5, /* TODO: NmsUtils.getBlockHeight(b) */ 1, .5);
            for (Entity e : spawnLocation.getWorld().getNearbyEntities(spawnLocation, 0.25, 0.25, 0.25)) {
                if (NPCBase.isNyaaNPC(e)) {
                    msg(sender, "user.hehshop.add_too_close");
                    return;
                }
            }

            CraftPlayer cp = (CraftPlayer) p;
            Property texture = Iterables.getFirst(cp.getProfile().getProperties().get("textures"), null);
            String skinDataId = "default";
            if (texture != null) {
                skinDataId = "hehshop:" + p.getUniqueId().toString();
                SkinData sd = new SkinData(skinDataId, p.getName() + "'s HEH NPC shop", texture.getValue(), texture.getSignature());
                sd.followPlayer = p.getUniqueId();
                plugin.cfg.skinData.updateSkinData(skinDataId, sd);
            }

            NpcData data = new NpcData(p.getUniqueId(), spawnLocation, p.getName(), PLAYER, NpcType.HEH_SELL_SHOP, "");
            data.hehShopOwnerUUID = data.ownerId;
            data.playerSkin = skinDataId;
            String npcId = plugin.entitiesManager.createNpcDefinition(data);
            msg(sender, "user.hehshop.add_success");
        }
    }
}