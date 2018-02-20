package cat.nyaa.npc;

import cat.nyaa.npc.persistance.NpcData;
import cat.nyaa.npc.persistance.TradeData;
import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.utils.RayTraceUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;

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

    @SubCommand(value = "spawn", permission = "npc.admin.spawn")
    public void newNpcDefinition(CommandSender sender, Arguments args) {
        EntityType type = args.nextEnum(EntityType.class);
        if (!plugin.cfg.isAllowedType(type)) {
            throw new BadCommandException("user.spawn.type_disallow", type.name());
        }
        String name = args.nextString();
        Block b = getRayTraceBlock(sender);
        if (b == null || b.getType() == Material.AIR) {
            throw new BadCommandException("user.spawn.not_block");
        }
        if (b.getRelative(BlockFace.UP).getType().isSolid() || b.getRelative(0, 2, 0).getType().isSolid()) {
            throw new BadCommandException("user.spawn.not_enough_space");
        }
        NpcData data = new NpcData(b.getLocation().clone().add(.5, 0, .5), name, type);
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
                if (!EntitiesManager.isNyaaNPC(e)) continue;
                LivingEntity currentMobEntity = (LivingEntity) e;
                Vector eyeSight = p.getEyeLocation().getDirection();
                Vector mobVector = currentMobEntity.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
                double angle = getVectorAngle(eyeSight, mobVector);
                if (!Double.isFinite(angle)) continue;
                if (angle >= minAngle) continue;
                minAngle = angle;
                npcId = EntitiesManager.getNyaaNpcId(e);
            }
        }
        if (npcId == null) {
            throw new BadCommandException("user.info.no_selection");
        }
        NpcData data = asNpcData(npcId);
        msg(sender, "user.info.msg_id", npcId);
        msg(sender, "user.info.msg_name", data.displayName);
        msg(sender, "user.info.msg_type", data.type.name());
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

    @SubCommand(value = "list", permission = "npc.admin.list")
    public void listNpc(CommandSender sender, Arguments args) {
        if (plugin.cfg.npcData.npcList.size() == 0) {
            msg(sender, "user.list.no_npc");
        } else {
            for (Map.Entry<String, NpcData> e : plugin.cfg.npcData.npcList.entrySet()) {
                msg(sender, "user.list.msg",
                        e.getValue().displayName,
                        e.getKey(),
                        e.getValue().type);
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

    // helper functions

    private Block getRayTraceBlock(CommandSender sender) {
        try {
            return RayTraceUtils.rayTraceBlock(asPlayer(sender));
        } catch (ReflectiveOperationException ex) {
            throw (BadCommandException) (new BadCommandException().initCause(ex));
        }
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