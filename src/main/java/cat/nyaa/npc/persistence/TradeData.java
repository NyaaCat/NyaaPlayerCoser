package cat.nyaa.npc.persistence;

import cat.nyaa.nyaacore.Message;
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

    /**
     * How many times this trade can be done.
     * This function does not consider the inventory if npctype is TRADER_BOX
     *
     * @param slot1 content of left trade slot
     * @param slot2 content or right trade slot
     * @return -1 item type mismatch; 0 not enough materials; x>0 this trade can be done at most x times.
     */
    public int allowedTradeCount(ItemStack slot1, ItemStack slot2) {
        if (slot1 == null || slot1.getType() == AIR) return -1;
        if (!slot1.isSimilar(item1)) return -1;
        int c1 = slot1.getAmount() / item1.getAmount();

        if (item2 != null && item2.getType() != AIR) {
            if (slot2 == null || slot2.getType() == AIR) return -1;
            if (!slot2.isSimilar(item2)) return -1;
            int c2 = slot2.getAmount() / item2.getAmount();
            return c1 < c2 ? c1 : c2;
        } else {
            return c1;
        }
    }

    public Message appendDescription(Message msg) {
        msg.append(item1 == null ? new ItemStack(AIR) : item1.clone());
        msg.append(" + ");
        msg.append(item2 == null ? new ItemStack(AIR) : item2.clone());
        msg.append(" => ");
        msg.append(result == null ? new ItemStack(AIR) : result.clone());
        return msg;
    }
}
