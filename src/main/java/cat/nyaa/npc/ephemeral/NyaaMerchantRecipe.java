package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.persistence.TradeData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.Material.AIR;

public class NyaaMerchantRecipe extends MerchantRecipe {
    private final String id;
    private final TradeData data;

    public NyaaMerchantRecipe(String id, TradeData data) {
        // public MerchantRecipe(ItemStack result, int uses, int maxUses, boolean experienceReward, int villagerExperience, float priceMultiplier)
        super(data.result, 0, 99999, false, 0, 1);
        if (data.result == null || data.result.getType() == AIR) throw new IllegalArgumentException();
        this.id = id;
        this.data = data;

        List<ItemStack> ingredients = new ArrayList<>();
        if (data.item1 == null || data.item1.getType() == AIR) {
            if (data.item2 == null || data.item2.getType() == AIR) {
                throw new IllegalArgumentException();
            } else {
                ingredients.add(data.item2.clone());
            }
        } else {
            ingredients.add(data.item1.clone());
            if (data.item2 != null && data.item2.getType() != AIR) {
                ingredients.add(data.item2.clone());
            }
        }
        setIngredients(ingredients);
    }

    public String getTradeId() {
        return id;
    }

    public TradeData getTradeData() {
        return data;
    }
}
