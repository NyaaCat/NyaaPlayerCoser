package cat.nyaa.npc.persistance;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcData implements ISerializable {
    public NpcData() {
    }

    public NpcData(Location loc, String displayName, EntityType type) {
        if (!type.isAlive() || !type.isSpawnable()) throw new IllegalArgumentException();
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.displayName = displayName;
        this.type = type;
    }

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
    public EntityType type;
    @Serializable
    public boolean enabled = true;
    @Serializable
    public List<String> trades = new ArrayList<>();

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
}
