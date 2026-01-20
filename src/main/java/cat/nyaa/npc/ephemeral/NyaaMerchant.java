package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.TradeData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NyaaMerchant implements Merchant {
    private final String id;
    private final NpcData data;
    private final Merchant delegate;
    private InventoryView openedInventoryView = null;

    private static final Map<Inventory, NyaaMerchant> merchantLookupMap = new HashMap<>();

    public NyaaMerchant(String npcId, NpcData data) {
        this.id = npcId;
        this.data = data;
        this.delegate = Bukkit.createMerchant(Component.text(data.displayName));

        List<MerchantRecipe> recipes = new ArrayList<>();
        Map<String, TradeData> tradeList = NyaaPlayerCoser.instance.cfg.tradeData.tradeList;

        for (String tradeId : data.trades) {
            if (tradeList.containsKey(tradeId)) {
                recipes.add(new NyaaMerchantRecipe(tradeId, tradeList.get(tradeId)));
            } else {
                throw new RuntimeException(); // FIXME
            }
        }
        delegate.setRecipes(recipes);
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

    // Merchant interface delegation
    @Override
    public List<MerchantRecipe> getRecipes() {
        return delegate.getRecipes();
    }

    @Override
    public void setRecipes(List<MerchantRecipe> recipes) {
        delegate.setRecipes(recipes);
    }

    @Override
    public MerchantRecipe getRecipe(int i) throws IndexOutOfBoundsException {
        return delegate.getRecipe(i);
    }

    @Override
    public void setRecipe(int i, MerchantRecipe recipe) throws IndexOutOfBoundsException {
        delegate.setRecipe(i, recipe);
    }

    @Override
    public int getRecipeCount() {
        return delegate.getRecipeCount();
    }

    @Override
    public boolean isTrading() {
        return delegate.isTrading();
    }

    @Override
    public HumanEntity getTrader() {
        return delegate.getTrader();
    }
}
