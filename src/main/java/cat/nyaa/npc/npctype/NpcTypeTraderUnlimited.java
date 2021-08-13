package cat.nyaa.npc.npctype;

import cat.nyaa.npc.I18n;
import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.ephemeral.NyaaMerchant;
import cat.nyaa.npc.ephemeral.NyaaMerchantRecipe;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.TradeData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.HashSet;
import java.util.logging.Level;

import static org.bukkit.event.Event.Result.DENY;

public class NpcTypeTraderUnlimited extends AbstractNpcType{
    NpcTypeTraderUnlimited(String id) {
        super(id);
    }

    @Override
    public void activateNpcForPlayer(NpcData npcData, String npcId, Player player) {

        if (npcData.trades.size() <= 0) {
            player.sendMessage(I18n.format("user.interact.not_ready"));
        } else {
            NyaaMerchant ephemeralMerchant = new NyaaMerchant(npcId, npcData);
            InventoryView vi = player.openMerchant(ephemeralMerchant, false);
            if (vi == null) {
                player.sendMessage(I18n.format("user.interact.open_merchant_fail"));
            } else {
                ephemeralMerchant.registerLookup(vi);
                if (!plugin.tradingController.activeMerchants.containsKey(npcId)) {
                    plugin.tradingController.activeMerchants.put(npcId, new HashSet<>());
                }
                plugin.tradingController.activeMerchants.get(npcId).add(ephemeralMerchant);
            }
        }
    }

    @Override
    public boolean canSpawn(EntityType entityType, CommandSender sender) {
        return true;
    }

    @Override
    public void playerInteractWithWindow(InventoryClickEvent ev, NpcData data, NyaaMerchant m) {
        //super.playerInteractWithWindow(ev, data,m);

        if (ev.getClickedInventory() instanceof MerchantInventory && ev.getSlotType() == InventoryType.SlotType.RESULT) {
            // player try to fetch the result item
            MerchantInventory mInv = (MerchantInventory) ev.getClickedInventory();

            // FIXME debug only
            NyaaPlayerCoser.debug(log -> {
                if (ev.getWhoClicked().isOp()) {
                    ev.getWhoClicked().sendMessage("supplied item1: " + TradeData.itemDesc(mInv.getItem(0)));
                    ev.getWhoClicked().sendMessage("supplied item2: " + TradeData.itemDesc(mInv.getItem(1)));
                }
            });

            try {
                MerchantRecipe recipe = m.getRecipe(mInv.getSelectedRecipeIndex());
                if (recipe instanceof NyaaMerchantRecipe) {
                    NyaaMerchantRecipe nyaaRecipe = (NyaaMerchantRecipe) recipe;
                    TradeData d = nyaaRecipe.getTradeData();

                    // FIXME debug only
                    if (ev.getWhoClicked().isOp() && NyaaPlayerCoser.debugEnabled) {
                        ev.getWhoClicked().sendMessage("selected recipe: " + d.toString());
                    }

                    if (d.allowedTradeCount(mInv.getItem(0), mInv.getItem(1)) <= 0) {
                        ev.setResult(DENY); // mismatch item, deny exchange
                    }
                } else {
                    ev.setResult(DENY);
                    this.plugin.getLogger().warning(String.format("NyaaNPC (%s) with non-NPC recipe: %s", m.getNpcId(), recipe));
                }
            } catch (NullPointerException ex) {
                plugin.getLogger().log(Level.WARNING, "Error trade with npc: " + m.getNpcId(), ex);
                ev.getWhoClicked().sendMessage("Internal Error: please report this bug");
                ev.setResult(DENY);
            }
        }
    }

    @Override
    public boolean canBeSet(NpcData data) {
        return true;
    }
}
