package cat.nyaa.npc.persistance;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NpcData implements ISerializable, Cloneable {
    public NpcData() {
    }

    public NpcData(UUID ownerId, Location loc, String displayName, EntityType type, String nbtTag) {
        if (!type.isAlive() || !type.isSpawnable()) throw new IllegalArgumentException();
        this.ownerId = ownerId;
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.displayName = displayName;
        this.type = type;
        this.nbtTag = nbtTag;
    }

    public void setChestLocation(Location l) {
        if (l == null) { // unset location
            chestWorldName = "";
            chestX = chestY = chestZ = 0;
        } else {
            chestWorldName = l.getWorld().getName();
            chestX = l.getBlockX();
            chestY = l.getBlockY();
            chestZ = l.getBlockZ();
        }
    }

    public Location getChestLocation() {
        if (!chestEnabled || chestWorldName == null || chestWorldName.length() <= 0) {
            return null;
        }
        World w = Bukkit.getWorld(chestWorldName);
        if (w == null) return null; // TODO warn
        Location l = new Location(w, chestX, chestY, chestZ);
        Material m = l.getBlock().getType();
        if (m != Material.CHEST && m != Material.TRAPPED_CHEST && m != Material.SHULKER_BOX) return null; // TODO warn
        return l;
    }

    @Serializable
    public UUID ownerId;
    @Serializable
    public String worldName;
    @Serializable
    public Double x;
    @Serializable
    public Double y;
    @Serializable
    public Double z;
    @Serializable
    public String displayName;
    @Serializable
    public String nbtTag;
    @Serializable
    public EntityType type;
    @Serializable
    public boolean enabled = true;
    @Serializable
    public List<String> trades = new ArrayList<>();


    @Serializable
    public String chestWorldName = "";
    @Serializable
    public int chestX = 0;
    @Serializable
    public int chestY = 0;
    @Serializable
    public int chestZ = 0;
    @Serializable
    public boolean chestEnabled; // when not using chest, trades are unlimited

    public int chunkX() {
        return ((int) Math.floor(x)) >> 4;
    }

    public int chunkZ() {
        return ((int) Math.floor(z)) >> 4;
    }

    /**
     * Get a dummy merchant that player can trade with
     */
    public Merchant getMerchant() {
        Merchant merchant = Bukkit.createMerchant(displayName);
        List<MerchantRecipe> recipes = new ArrayList<>();
        Map<String, TradeData> tradeList = NyaaPlayerCoser.instance.cfg.tradeData.tradeList;
        for (String i : trades) {
            // TODO warn
            if (tradeList.containsKey(i)) {
                recipes.add(tradeList.get(i).getRecipe());
            }
        }
        merchant.setRecipes(recipes);
        return merchant;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        NpcData cloned = (NpcData)(super.clone());
        // TODO: deep clone
        return cloned;
    }
}
