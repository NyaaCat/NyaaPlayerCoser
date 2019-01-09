package cat.nyaa.npc;

import cat.nyaa.npc.ephemeral.NPCBase;
import cat.nyaa.npc.persistence.DataImporter;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.NpcType;
import cat.nyaa.npc.persistence.TradeData;
import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.utils.ClickSelectionUtils;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import cat.nyaa.nyaacore.utils.RayTraceUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Map;
import java.util.UUID;

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

    @SubCommand(value = "reload", permission = "npc.admin.reload")
    public void reloadCommand(CommandSender sender, Arguments args) {
        plugin.onReload();
    }

    @SubCommand(value = "import", permission = "npc.admin.import")
    public void importFromShopkeeper(CommandSender sender, Arguments args) {
        DataImporter.importShopkeeper(plugin, sender);
    }

    @SubCommand(value = "spawn", permission = "npc.admin.spawn")
    public void newNpcDefinition(CommandSender sender, Arguments args) {
        EntityType type = args.nextEnum(EntityType.class);
        if (!plugin.cfg.isAllowedType(type)) {
            throw new BadCommandException("user.spawn.type_disallow", type.name());
        }
        String name = args.nextString();
        String entitydataTag = args.next();
        if (entitydataTag == null) entitydataTag = "";
        Block b = getRayTraceBlock(sender);
        if (b == null || b.getType() == Material.AIR) {
            throw new BadCommandException("user.spawn.not_block");
        }
        if (b.getRelative(BlockFace.UP).getType().isSolid() || b.getRelative(0, 2, 0).getType().isSolid()) {
            throw new BadCommandException("user.spawn.not_enough_space");
        }
        NpcData data = new NpcData(asPlayer(sender).getUniqueId(), b.getLocation().clone().add(.5, /* TODO: NmsUtils.getBlockHeight(b) */ 1, .5), name, type, entitydataTag);
        String npcId = plugin.entitiesManager.createNpcDefinition(data);
        msg(sender, "user.spawn.id_created", npcId);
    }

    @SubCommand(value = "info", permission = "npc.admin.info")
    public void inspectNpcInfo(CommandSender sender, Arguments args) {
        String npcId = null;
        if (args.top() != null) {
            npcId = args.nextString();
        } else {
            // select cursor entity
            Player p = asPlayer(sender);
            double minAngle = Math.PI / 2D;
            for (Entity e : p.getNearbyEntities(3, 3, 3)) {
                if (!NPCBase.isNyaaNPC(e)) continue;
                LivingEntity currentMobEntity = (LivingEntity) e;
                Vector eyeSight = p.getEyeLocation().getDirection();
                Vector mobVector = currentMobEntity.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
                double angle = getVectorAngle(eyeSight, mobVector);
                if (!Double.isFinite(angle)) continue;
                if (angle >= minAngle) continue;
                minAngle = angle;
                npcId = NPCBase.getNyaaNpcId(e);
            }
        }
        if (npcId == null) {
            throw new BadCommandException("user.info.no_selection");
        }
        NpcData data = asNpcData(npcId);
        msg(sender, "user.info.msg_id", npcId);
        msg(sender, "user.info.msg_name", data.displayName);
        msg(sender, "user.info.msg_type", data.type.name());
        msg(sender, "user.info.msg_npctype", data.npcType.name());
        msg(sender, "user.info.msg_loc",
                String.format("[world=%s, x=%.2f, y=%.2f, z=%.2f]", data.worldName, data.x, data.y, data.z));
        if (data.trades.size() > 0) {
            for (String trade : data.trades) {
                msg(sender, "user.info.msg_trade", trade);
            }
        } else {
            msg(sender, "user.info.msg_no_trade");
        }
    }

    @SubCommand(value = "inspect", permission = "npc.admin.inspect")
    public void inspectData(CommandSender sender, Arguments args) {
        String type = args.nextString();
        String id = args.nextString();
        if (type.equalsIgnoreCase("trade")) {
            TradeData tradeData = plugin.cfg.tradeData.tradeList.get(id);
            if (tradeData == null) {
                throw new BadCommandException("user.inspect.trade.no_trade", id);
            }

            Player p = asPlayer(sender);
            if (tradeData.item1 != null && tradeData.item1.getType() != Material.AIR)
                p.getLocation().getWorld().dropItem(p.getLocation(), tradeData.item1.clone());
            if (tradeData.item2 != null && tradeData.item2.getType() != Material.AIR)
                p.getLocation().getWorld().dropItem(p.getLocation(), tradeData.item2.clone());
            if (tradeData.result != null && tradeData.result.getType() != Material.AIR)
                p.getLocation().getWorld().dropItem(p.getLocation(), tradeData.result.clone());
        } else {
            throw new BadCommandException("user.inspect.invalid_command", type);
        }
    }

    @SubCommand(value = "remove", permission = "npc.admin.remove")
    public void removeNpcDefinition(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        asNpcData(npcId);
        plugin.entitiesManager.removeNpcDefinition(npcId);
    }

    @SubCommand(value = "rename", permission = "npc.admin.edit")
    public void renameNpc(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        NpcData data = asNpcData(npcId);
        String newName = args.nextString();
        data.displayName = newName;
        plugin.entitiesManager.replaceNpcDefinition(npcId, data);
    }

    @SubCommand(value = "modify", permission = "npc.admin.edit")
    public void modifyNpc(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        NpcData data = asNpcData(npcId);
        boolean modified = false;

        String newName = args.argString("name", null);
        if (newName != null) {
            data.displayName = newName;
            modified = true;
        }

        String newNpcType = args.argString("npctype", null);
        if (newNpcType != null) {
            NpcType newType = Arguments.parseEnum(NpcType.class, newNpcType);
            data.npcType = newType;
            modified = true;
        }

        if (modified) {
            plugin.entitiesManager.replaceNpcDefinition(npcId, data);
        }
    }

    @SubCommand(value = "list", permission = "npc.admin.list")
    public void listNpc(CommandSender sender, Arguments args) {
        if (plugin.cfg.npcData.npcList.size() == 0) {
            msg(sender, "user.list.no_npc");
        } else {
            for (Map.Entry<String, NpcData> e : plugin.cfg.npcData.npcList.entrySet()) {
                msg(sender, "user.list.msg",
                        e.getValue().displayName,
                        e.getKey(),
                        e.getValue().type.name(),
                        e.getValue().npcType.name());
            }
        }
    }

    @SubCommand(value = "newtrade", permission = "npc.admin.edit")
    public void newTradeForNPC(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        NpcData npc = asNpcData(npcId);

        ItemStack itemStack1 = getItemStackInSlot(sender, 0, false);
        ItemStack itemStack2 = getItemStackInSlot(sender, 1, true);
        ItemStack result = getItemStackInSlot(sender, 2, false);
        TradeData data = new TradeData(itemStack1, itemStack2, result);
        String tradeId = plugin.cfg.tradeData.addTrade(data);
        msg(sender, "user.newtrade.id_created", tradeId);
        npc.trades.add(tradeId);
        plugin.entitiesManager.replaceNpcDefinition(npcId, npc);
    }

    @SubCommand(value = "chest", permission = "npc.admin.edit")
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

    @SubCommand(value = "adjust_location", permission = "npc.admin.edit")
    public void adjustLocation(CommandSender sender, Arguments args) {
        String npcId = args.nextString();
        asNpcData(npcId);
        plugin.entitiesManager.adjustNpcLocation(npcId, sender);
    }

    @SubCommand(value = "test")
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
}