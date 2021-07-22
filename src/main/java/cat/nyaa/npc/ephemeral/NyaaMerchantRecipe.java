package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.persistence.TradeData;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftMerchantRecipe;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Material.AIR;

public class NyaaMerchantRecipe extends CraftMerchantRecipe {
    private final String id;
    private final TradeData data;

    public NyaaMerchantRecipe(String id, TradeData data) {
        // public CraftMerchantRecipe(ItemStack result, int uses, int maxUses, boolean experienceReward, int experience, float priceMultiplier)
        super(data.result, 0, 99999, false, 0, 1);
        if (data.result == null || data.result.getType() == AIR) throw new IllegalArgumentException();
        this.id = id;
        this.data = data;
        if (data.item1 == null || data.item1.getType() == AIR) {
            if (data.item2 == null || data.item2.getType() == AIR) {
                throw new IllegalArgumentException();
            } else {
                addIngredient(data.item2.clone());
                addIngredient(new ItemStack(AIR));
            }
        } else {
            addIngredient(data.item1.clone());
            if (data.item2 != null && data.item2.getType() != AIR) {
                addIngredient(data.item2.clone());
            } else {
                addIngredient(new ItemStack(AIR));
            }
        }
    }

    public String getTradeId() {
        return id;
    }

    public TradeData getTradeData() {
        return data;
    }
}
