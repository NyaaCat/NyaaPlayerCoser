package cat.nyaa.npc.persistence;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Material.AIR;

public class TradeData implements ISerializable {
    public TradeData() {
    }

    public TradeData(ItemStack item1, ItemStack result) {
        this(item1, null, result);
    }

    public TradeData(ItemStack item1, ItemStack item2, ItemStack result) {
        if (item1 == null || result == null || item1.getType() == AIR || result.getType() == AIR) {
            throw new IllegalArgumentException();
        }
        this.item1 = item1.clone();
        if (item2 == null) {
            this.item2 = new ItemStack(AIR);
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

    private boolean exactMatch(ItemStack s1, ItemStack s2) {
        ItemStack d1 = s1 == null ? new ItemStack(AIR) : s1.clone();
        ItemStack d2 = s2 == null ? new ItemStack(AIR) : s2.clone();
        d1.setAmount(1);
        d2.setAmount(1);
        return d1.equals(d2);
    }

    /**
     * How many times this trade can be done.
     * This function does not consider the inventory if npctype is TRADER_BOX
     *
     * @param slot1 content of left trade slot
     * @param slot2 content or right trade slot
     * @return -1 item type mismatch; 0 not enough materials; x>0 this trade can be done at most x times.
     */
    public int allowedTradeCount(ItemStack slot1, ItemStack slot2) {
        if (!exactMatch(slot1, item1) || !exactMatch(slot2, item2)) return -1;
        int c1 = slot1.getAmount() / item1.getAmount();
        int c2 = (slot2 == null || slot2.getType() == AIR) ? Integer.MAX_VALUE : (slot2.getAmount() / item2.getAmount());
        return Math.min(c1, c2);
    }
}
