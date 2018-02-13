package cat.nyaa.npc;

import cat.nyaa.npc.persistance.NpcData;
import cat.nyaa.npc.persistance.TradeData;
import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.utils.RayTraceUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

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

    @SubCommand("debug")
    public void debugCommand(CommandSender sender, Arguments args) {
        Block b = getRayTraceBlock(sender);
        b.setType(Material.WOOL);
    }

    @SubCommand("debug2")
    public void debugCommand2(CommandSender sender, Arguments args) {
        int x = args.nextInt();
        int y = args.nextInt();
        int z = args.nextInt();
        boolean loaded = isChunkLoadedAtBlockCoordinate(asPlayer(sender).getWorld(), x, z);
        sender.sendMessage("isLoaded = " + loaded);
    }

    @SubCommand("newtrade")
    public void newTrade(CommandSender sender, Arguments args) {
        ItemStack itemStack1 = getItemStackInSlot(sender, 0, false);
        ItemStack itemStack2 = getItemStackInSlot(sender, 1, true);
        ItemStack result = getItemStackInSlot(sender, 2, false);
        TradeData data = new TradeData(itemStack1, itemStack2, result);
        String tradeId = plugin.cfg.tradeData.addTrade(data);
        msg(sender, "user.new_trade.id_created", tradeId);
    }

    @SubCommand("newnpc")
    public void newNpc(CommandSender sender, Arguments args) {
        Block b = getRayTraceBlock(sender);
        if (b == null || b.getType() == Material.AIR) {
            throw new BadCommandException("user.new_npc.not_block");
        }
        if (b.getRelative(BlockFace.UP).getType().isSolid() || b.getRelative(0,2,0).getType().isSolid()) {
            throw new BadCommandException("user.new_npc.not_enough_space");
        }
        NpcData data = new NpcData(b.getLocation().clone().add(.5,0,.5), "Demo NPC", EntityType.VILLAGER);
        String npcId = plugin.cfg.npcData.addNpc(data);
        msg(sender, "user.new_npc.id_created", npcId);
    }

    private Block getRayTraceBlock(CommandSender sender) {
        try {
            return RayTraceUtils.rayTraceBlock(asPlayer(sender));
        } catch (ReflectiveOperationException ex) {
            throw (BadCommandException)(new BadCommandException().initCause(ex));
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
}