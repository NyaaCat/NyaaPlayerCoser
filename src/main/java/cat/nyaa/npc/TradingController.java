package cat.nyaa.npc;

import cat.nyaa.npc.events.NpcRedefinedEvent;
import cat.nyaa.npc.events.NpcUndefinedEvent;
import cat.nyaa.npc.persistance.NpcData;
import cat.nyaa.npc.persistance.NpcType;
import cat.nyaa.npc.persistance.TradeData;
import cat.nyaa.nyaacore.Pair;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;

import java.util.*;

public class TradingController implements Listener {
    private static class StructInProgressTrading {
        final String npcId;
        final InventoryView inventoryView;
        final UUID playerId;
        final Merchant merchant;

        public StructInProgressTrading(String npcId, InventoryView inventoryView, UUID playerId, Merchant merchant) {
            if (npcId == null || inventoryView == null || playerId == null || merchant == null)
                throw new IllegalArgumentException();
            this.npcId = npcId;
            this.inventoryView = inventoryView;
            this.playerId = playerId;
            this.merchant = merchant;
        }
    }

    private final NyaaPlayerCoser plugin;

    /**
     * TradingController keeps track of all the trading windows.
     * It's important to {@link TradingController#destructor}
     * when reloading the plugin.
     */
    public TradingController(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void destructor() {
        for (StructInProgressTrading t : _nmp_db) {
            t.inventoryView.close();
        }
        _nmp_db.clear();
        _merchant_recipes.clear();
    }

    /* ************************************** */
    /* NPC-MerchantView-Player relation logic */
    /* ************************************** */
    private final List<StructInProgressTrading> _nmp_db = new LinkedList<>();

    private StructInProgressTrading _getByUser(UUID uuid) {
        for (StructInProgressTrading t : _nmp_db) {
            if (t.playerId.equals(uuid)) return t;
        }
        return null;
    }

    private StructInProgressTrading _getByInv(InventoryView iv) {
        for (StructInProgressTrading t : _nmp_db) {
            if (t.inventoryView == iv) return t;
        }
        return null;
    }

    private void _newView(String npcId, InventoryView view, UUID playerUUID, Merchant merchant) {
        _nmp_db.add(new StructInProgressTrading(npcId, view, playerUUID, merchant));
    }

    private Pair<String, UUID> _closeView(InventoryView view) {
        for (Iterator<StructInProgressTrading> iter = _nmp_db.iterator(); iter.hasNext();) {
            StructInProgressTrading t = iter.next();
            if (t.inventoryView == view) {
                _deleteMerchant(t.merchant);
                iter.remove();
                return Pair.of(t.npcId, t.playerId);
            }
        }
        return null;
    }

    private List<UUID> haltNpcTradings(String npcId) {
        List<UUID> affectedPlayers = new LinkedList<>();
        for (Iterator<StructInProgressTrading> iter = _nmp_db.iterator(); iter.hasNext();) {
            StructInProgressTrading t = iter.next();
            if (t.npcId.equals(npcId)) {
                t.inventoryView.close();
                _deleteMerchant(t.merchant);
                affectedPlayers.add(t.playerId);
                iter.remove();
            }
        }
        return affectedPlayers;
    }

    private String haltPlayerTrading(UUID playerUUID) {
        String npcId = null;
        for (Iterator<StructInProgressTrading> iter = _nmp_db.iterator(); iter.hasNext();) {
            StructInProgressTrading t = iter.next();
            if (t.playerId.equals(playerUUID)) {
                t.inventoryView.close();
                _deleteMerchant(t.merchant);
                iter.remove();
                if (npcId != null) {
                    plugin.getLogger().warning(String.format("unexpected concurrent trading: %s with %s and %s", t.playerId.toString(), npcId, t.npcId));
                }
                npcId = t.npcId;
            }
        }
        return npcId;
    }

    /* ************************************** */
    /*               Merchants                */
    /* ************************************** */
    private final Map<Merchant, List<String>> _merchant_recipes = new HashMap<>();
    private Merchant _newMerchant(NpcData npc) {
        if (npc.npcType != NpcType.TRADER_UNLIMITED && npc.npcType != NpcType.TRADER_BOX)
            throw new IllegalStateException("this method is meaningless for a non-trader");
        Merchant merchant = Bukkit.createMerchant(npc.displayName);

        List<String> index_mapping = new ArrayList<>(npc.trades.size());
        List<MerchantRecipe> recipes = new ArrayList<>();
        Map<String, TradeData> tradeList = plugin.cfg.tradeData.tradeList;
        for (String i : npc.trades) {
            // TODO warn
            if (tradeList.containsKey(i)) {
                recipes.add(tradeList.get(i).getRecipe());
                index_mapping.add(i);
            }
        }
        merchant.setRecipes(recipes);
        _merchant_recipes.put(merchant, index_mapping);
        return merchant;
    }

    private void _deleteMerchant(Merchant m) {
        _merchant_recipes.remove(m);
    }

    private TradeData _merchantIndex(Merchant m, int idx) {
        List<String> s = _merchant_recipes.get(m);
        if (s == null || s.size() <= idx) return null;
        return plugin.cfg.tradeData.tradeList.get(s.get(idx));
    }



    /* ************************************** */
    /*             player events              */
    /* ************************************** */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractNPC(PlayerInteractEntityEvent ev) {
        if (_getByUser(ev.getPlayer().getUniqueId()) != null) return; // TODO msg to user
        if (EntitiesManager.isNyaaNPC(ev.getRightClicked())) {
            ev.setCancelled(true);
            String npcId = EntitiesManager.getNyaaNpcId(ev.getRightClicked());
            final NpcData npcData = plugin.cfg.npcData.npcList.get(npcId);

            if (npcData.npcType == NpcType.TRADER_BOX || npcData.npcType == NpcType.TRADER_UNLIMITED) {
                if (npcData.trades.size() <= 0) {
                    ev.getPlayer().sendMessage(I18n.format("user.interact.not_ready"));
                    return;
                }
                Merchant m = _newMerchant(npcData);
                InventoryView iv = ev.getPlayer().openMerchant(m, false);
                if (iv != null) {
                    _newView(npcId, iv, ev.getPlayer().getUniqueId(), m);
                } else {
                    _deleteMerchant(m);
                    ev.getPlayer().sendMessage(I18n.format("user.interact.open_merchant_fail"));
                }
            } else if (npcData.npcType == NpcType.HEH_SELL_SHOP) {
                try {
                    ExternalPluginUtils.hehOpenPlayerShop(npcData.ownerId, ev.getPlayer(), ev.getRightClicked().getLocation(), "npc-"+npcId);
                } catch (ExternalPluginUtils.OperationNotSupportedException ex) {
                    ev.getPlayer().sendMessage(I18n.format("user.interact.heh_not_support"));
                }
            } else {
                ev.getPlayer().sendMessage(I18n.format("user.interact.type_not_support", npcData.npcType));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCloseWindow(InventoryCloseEvent ev) {
        _closeView(ev.getView());
    }

    @EventHandler
    public void onNpcUndefined(NpcUndefinedEvent ev) {
        haltNpcTradings(ev.getNpcId());
    }

    @EventHandler
    public void onNpcModified(NpcRedefinedEvent ev) {
        haltNpcTradings(ev.getNpcId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent ev) {
        haltPlayerTrading(ev.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerKickEvent ev) {
        haltPlayerTrading(ev.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractWithWindow(InventoryClickEvent ev) {
        final StructInProgressTrading t = _getByUser(ev.getWhoClicked().getUniqueId());
        if (t == null) return;
        NpcData data = plugin.cfg.npcData.npcList.get(t.npcId);

        if (data != null && (data.npcType == NpcType.TRADER_BOX || data.npcType == NpcType.TRADER_UNLIMITED) &&
                ev.getClickedInventory() instanceof MerchantInventory && ev.getView() == t.inventoryView &&
                ev.getSlotType() == InventoryType.SlotType.RESULT) {
            MerchantInventory inv = (MerchantInventory) ev.getClickedInventory();
            if (data.npcType == NpcType.TRADER_UNLIMITED) {
                TradeData td = _merchantIndex(t.merchant, inv.getSelectedRecipeIndex());
                ev.getWhoClicked().sendMessage(""+inv.getSelectedRecipeIndex());
                ev.getWhoClicked().sendMessage(""+inv.getItem(0));
                ev.getWhoClicked().sendMessage(""+inv.getItem(1));
                ev.getWhoClicked().sendMessage(""+inv.getItem(2));
                if (td.allowedTradeCount(inv.getItem(0), inv.getItem(1)) <= 0) ev.setResult(Event.Result.DENY);
            } else {
                ev.getWhoClicked().sendMessage(I18n.format("user.interact.type_not_support", data.npcType));
                ev.setResult(Event.Result.DENY);
            }
        }

        ev.getWhoClicked().sendMessage("Clicked inv:" + ev.getClickedInventory());
        ev.getWhoClicked().sendMessage("Clicked view:" + ev.getView().toString());
        ev.getWhoClicked().sendMessage("NPCID:" + t.npcId);
        ev.getWhoClicked().sendMessage("InvView:" + t.inventoryView);
        ev.getWhoClicked().sendMessage("UUID:" + t.playerId);

        ev.getWhoClicked().sendMessage("DUMP:" +
                ev.getWhoClicked() + " " +
                ev.getCurrentItem() + " " +
                ev.getCursor() + " " +
                ev.getClick() + " " +
                ev.getAction() + " " +
                ev.getEventName() + " " +
                ev.getHotbarButton() + " " +
                ev.getRawSlot() + " " +
                ev.getSlot() + " " +
                ev.getSlotType() + " " +
                ev.getResult() + " " +
                ev.isLeftClick() + " " +
                ev.isRightClick() + " " +
                ev.isShiftClick());
    }

    // TODO: chest inventory
}
