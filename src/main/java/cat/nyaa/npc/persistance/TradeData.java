package cat.nyaa.npc.persistance;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TradeData implements ISerializable {
    public TradeData() { }

    public TradeData(ItemStack item1, ItemStack result) {
        this(item1, null, result);
    }

    public TradeData(ItemStack item1, ItemStack item2, ItemStack result) {
        if (item1 == null || result == null || item1.getType() == Material.AIR || result.getType() == Material.AIR) {
            throw new IllegalArgumentException();
        }
        this.item1 = item1.clone();
        if (item2 == null) {
            this.item2 = new ItemStack(Material.AIR);
        } else {
            this.item2 = item2.clone();
        }
        this.result = result.clone();
    }

    @Serializable
    public ItemStack item1;
    @Serializable
    public ItemStack item2;
    @Serializable
    public ItemStack result;
}
