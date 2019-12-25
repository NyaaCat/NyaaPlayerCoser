package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.TradeData;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftMerchantCustom;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NyaaMerchant extends CraftMerchantCustom {
    private final String id;
    private final NpcData data;
    private InventoryView openedInventoryView = null;

    private static final Map<Inventory, NyaaMerchant> merchantLookupMap = new HashMap<>();

    public NyaaMerchant(String npcId, NpcData data) {
        super(data.displayName);
        this.id = npcId;
        this.data = data;

        List<MerchantRecipe> recipes = new ArrayList<>();
        Map<String, TradeData> tradeList = NyaaPlayerCoser.instance.cfg.tradeData.tradeList;

        for (String tradeId : data.trades) {
            if (tradeList.containsKey(tradeId)) {
                recipes.add(new NyaaMerchantRecipe(tradeId, tradeList.get(tradeId)));
            } else {
                throw new RuntimeException(); // FIXME
            }
        }
        setRecipes(recipes);
    }

    public String getNpcId() {
        return id;
    }

    public NpcData getNpcData() {
        return data;
    }


    public void registerLookup(InventoryView inv) {
        if (openedInventoryView != null) throw new IllegalArgumentException("inv view double set.");
        if (!(inv.getTopInventory() instanceof MerchantInventory))
            throw new IllegalArgumentException("not merchant inv");
        openedInventoryView = inv;
        merchantLookupMap.put(inv.getTopInventory(), this);
    }

    public static NyaaMerchant removeLookup(Inventory inv) {
        return merchantLookupMap.remove(inv);
    }

    public InventoryView lookupView() {
        return openedInventoryView;
    }

    public static NyaaMerchant lookupMerchant(Inventory inv) {
        return merchantLookupMap.get(inv);
    }


}
