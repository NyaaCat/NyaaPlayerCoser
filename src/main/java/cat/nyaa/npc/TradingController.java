package cat.nyaa.npc;

import cat.nyaa.npc.ephemeral.NPCBase;
import cat.nyaa.npc.ephemeral.NPCPlayer;
import cat.nyaa.npc.ephemeral.NyaaMerchant;
import cat.nyaa.npc.ephemeral.NyaaMerchantRecipe;
import cat.nyaa.npc.events.NpcRedefinedEvent;
import cat.nyaa.npc.events.NpcUndefinedEvent;
import cat.nyaa.npc.events.TradeRedefinedEvent;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.TradeData;
import cat.nyaa.npc.utils.RunCommandUtils;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;
import com.comphenix.protocol.wrappers.EnumWrappers.Hand;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
import java.util.logging.Level;

import static cat.nyaa.npc.persistence.NpcType.TRADER_BOX;
import static cat.nyaa.npc.persistence.NpcType.TRADER_UNLIMITED;
import static com.comphenix.protocol.PacketType.Play.Client.USE_ENTITY;
import static org.bukkit.event.Event.Result.DENY;
import static org.bukkit.event.inventory.InventoryType.CRAFTING;
import static org.bukkit.event.inventory.InventoryType.CREATIVE;

public class TradingController implements Listener {

    private final NyaaPlayerCoser plugin;

    /**
     * TradingController keeps track of all the trading windows.
     * It's important to {@link TradingController#destructor}
     * when reloading the plugin.
     */
    public TradingController(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        ExternalPluginUtils.getPM().addPacketListener(onRightClickFakePlayer);
    }

    public void destructor() {
        ExternalPluginUtils.getPM().removePacketListener(onRightClickFakePlayer);
        for (Set<NyaaMerchant> s : activeMerchants.values()) {
            if (s == null) continue;
            for (NyaaMerchant m : s) {
                NyaaMerchant.removeLookup(m.lookupView().getTopInventory());
            }
        }
    }

    /* ************************************** */
    /*        inventory view events           */
    /* ************************************** */

    public final Map<String, Set<NyaaMerchant>> activeMerchants = new HashMap<>(); // npcid->merchant
    // NOTE states are also maintained in NyaaMerchant.merchantLookupMap

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractNPC(PlayerInteractEntityEvent ev) {
        Entity clickedEntity = ev.getRightClicked();
        if (plugin.entitiesManager != null)
            if (!plugin.entitiesManager.isValidNyaaNpcOrRemove(clickedEntity)) return;
        String npcId = NPCBase.getNyaaNpcId(clickedEntity);
        if (npcId == null) {
            return; // skip if not a nyaa npc
        }
        ev.setCancelled(true);

        if (!ev.getPlayer().hasPermission("npc.interact")) {
            return;
        }

        if (ev.getHand() == EquipmentSlot.HAND) { // trigger activation only when main hand interact event
            Player p = ev.getPlayer();
            InventoryType currentInvType = p.getOpenInventory().getType();
            if (currentInvType != CRAFTING && currentInvType != CREATIVE) {
                // sometimes the client will send multiple interact event in a row
                // skip if the player has another inventory opened
                return;
            }
            activateNpcForPlayer(npcId, ev.getPlayer());
        }
    }

    /**
     * Display a villager trading window for the player.
     * (or other type of windows such as HEH trade window)
     * <p>
     * For villager trading windows, the actual underlying "villager"
     * is {@link NyaaMerchant}. And one NM will be created for each player view.
     * Then one InventoryView will be created for the (player,NM).
     * / NyaaMerchant(instance1) -- InventoryView -- Player1
     * npcId - NyaaMerchant(instance2) -- InventoryView -- Player2
     * \ NyaaMerchant(instance3) -- InventoryView -- Player3
     * npcId2 - ...
     * \ ...
     * - given npcId find NM: use {@link this#activeMerchants}
     * - given NM find npcId: use {@link NyaaMerchant#getNpcId()}
     * - given NM find InventoryView: use {@link NyaaMerchant#lookupView()}
     * - given InventoryView find NM: use {@link NyaaMerchant#lookupMerchant(Inventory)}
     * <p>
     * So basically, {@link this#activeMerchants} holds all references to the NM instances
     * and {@link NyaaMerchant#merchantLookupMap} holds all references to the InventoryViews
     *
     * @param npcId
     * @param p
     */
    private void activateNpcForPlayer(String npcId, Player p) {
        NpcData data = plugin.cfg.npcData.npcList.get(npcId);
        if (data == null) {
            plugin.getLogger().warning(String.format("Cannot activate npc %s for player %s : Cannot find NPC definition", npcId, p.getName()));
            return;
        }
        // debug
//        {
//            if (data.travelPlan != null && data.travelPlan.isTraveller) {
//                p.sendMessage(String.format("time to travel: %.2fs", (data.travelPlan.nextMovementTime-System.currentTimeMillis())/1000.0));
//            }
//        }
        if (data.travelPlan != null && data.travelPlan.isTimeToMove()) {
            p.sendMessage(I18n.format("user.interact.its_time_to_move"));
            return;
        }

        switch (data.npcType) {
            case UNSPECIFIED: {
                p.sendMessage(I18n.format("user.interact.not_ready"));
                break;
            }
            case TRADER_BOX: {
                p.sendMessage(I18n.format("user.interact.type_not_support", TRADER_BOX)); // TODO
                break;
            }
            case TRADER_UNLIMITED: {
                if (data.trades.size() <= 0) {
                    p.sendMessage(I18n.format("user.interact.not_ready"));
                } else {
                    NyaaMerchant ephemeralMerchant = new NyaaMerchant(npcId, data);
                    InventoryView vi = p.openMerchant(ephemeralMerchant, false);
                    if (vi == null) {
                        p.sendMessage(I18n.format("user.interact.open_merchant_fail"));
                    } else {
                        ephemeralMerchant.registerLookup(vi);
                        if (!activeMerchants.containsKey(npcId)) {
                            activeMerchants.put(npcId, new HashSet<>());
                        }
                        activeMerchants.get(npcId).add(ephemeralMerchant);
                    }
                }
                break;
            }
            case HEH_SELL_SHOP: {
                try {
                    ExternalPluginUtils.hehOpenPlayerShop(data.ownerId, p, p.getLocation(), "npc-" + npcId);
                } catch (ExternalPluginUtils.OperationNotSupportedException ex) {
                    p.sendMessage(I18n.format("user.interact.heh_not_support"));
                }
                break;
            }
            case COMMAND: {
                if (data.npcCommand == null || data.npcCommand.equals("")) {
                    p.sendMessage(I18n.format("user.interact.not_ready"));
                    break;
                }
                RunCommandUtils.executeCommand(p, data.npcCommand, data.commandPermission);
                break;
            }
            default: {
                p.sendMessage(I18n.format("user.interact.type_not_support", data.npcType));
            }
        }
    }

    private void deactivateNpcViews(String npcId) {
        for (NyaaMerchant merchant : activeMerchants.getOrDefault(npcId, Collections.emptySet())) {
            Inventory registeredInventory = merchant.lookupView().getTopInventory();

            if (merchant.getTrader() instanceof Player) { // close player view only if opened view == registered view
                Player p = (Player) merchant.getTrader();
                if (p.getOpenInventory().getTopInventory() == registeredInventory) {
                    p.closeInventory();
                }
            }

            NyaaMerchant.removeLookup(registeredInventory);
        }
        activeMerchants.remove(npcId);
    }

    private void deactivateNpcViewForPlayer(Player p) {
        NyaaMerchant m = NyaaMerchant.removeLookup(p.getOpenInventory().getTopInventory());
        if (m != null) {
            String npcId = m.getNpcId();
            if (activeMerchants.get(npcId) != null) {
                activeMerchants.get(npcId).remove(m);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerCloseWindow(InventoryCloseEvent ev) {
        NyaaMerchant m = NyaaMerchant.removeLookup(ev.getInventory());
        if (m != null) {
            String npcId = m.getNpcId();
            if (activeMerchants.get(npcId) != null) {
                activeMerchants.get(npcId).remove(m);
            }
        }
    }

    @EventHandler
    public void onNpcUndefined(NpcUndefinedEvent ev) {
        deactivateNpcViews(ev.getNpcId());
    }

    @EventHandler
    public void onNpcModified(NpcRedefinedEvent ev) {
        deactivateNpcViews(ev.getNpcId());
    }

    @EventHandler
    public void onTradeModified(TradeRedefinedEvent ev) {
        for (String npcId : ev.getAffectedNpc()) {
            deactivateNpcViews(npcId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent ev) {
        deactivateNpcViewForPlayer(ev.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerKickEvent ev) {
        deactivateNpcViewForPlayer(ev.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractWithWindow(InventoryClickEvent ev) {
        NyaaMerchant m = NyaaMerchant.lookupMerchant(ev.getInventory());
        if (m == null) return;

        NpcData data = m.getNpcData();
        if (data == null) return;

        if (!ev.getWhoClicked().hasPermission("npc.interact")) {
            ev.setResult(DENY);
            return;
        }

        if (data.npcType == TRADER_UNLIMITED) {
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
                        plugin.getLogger().warning(String.format("NyaaNPC (%s) with non-NPC recipe: %s", m.getNpcId(), recipe));
                    }
                } catch (NullPointerException ex) {
                    plugin.getLogger().log(Level.WARNING, "Error trade with npc: " + m.getNpcId(), ex);
                    ev.getWhoClicked().sendMessage("Internal Error: please report this bug");
                    ev.setResult(DENY);
                }
            }
        } else {
            return;
            // TODO: chest inventory
        }
    }

    /**
     * onPlayerInteractNPC for FakePlayerNPCs, TODO: try to reuse the logic
     */
    private final PacketListener onRightClickFakePlayer = new PacketAdapter(NyaaPlayerCoser.instance, ListenerPriority.NORMAL,
            USE_ENTITY) {
        @Override
        public void onPacketReceiving(PacketEvent event) {
            if (event.getPacketType() == USE_ENTITY) {
                final int entityId = event.getPacket().getIntegers().read(0);
                final Player p = event.getPlayer();
                // begin sync task
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    if (!p.isOnline()) return;
                    NPCPlayer dummyNpc = NPCPlayer.spawnedDummyNPCs.get(entityId);
                    if (dummyNpc != null) {
                        event.setCancelled(true);
                        WrappedEnumEntityUseAction useAction = event.getPacket().getEnumEntityUseActions().read(0);
                        EntityUseAction action = useAction.getAction();

                        if (action == EntityUseAction.INTERACT) {
                            Hand hand = useAction.getHand();
                            if (hand == Hand.MAIN_HAND) {
                                if (!p.hasPermission("npc.interact")) {
                                    return;
                                }

                                InventoryType currentInvType = p.getOpenInventory().getType();
                                if (currentInvType != CRAFTING && currentInvType != CREATIVE) {
                                    return; // skip if the player has another inventory opened
                                }

                                if (p.getLocation().getWorld() != dummyNpc.getEyeLocation().getWorld()) return;
                                if (p.getLocation().distanceSquared(dummyNpc.getEyeLocation()) > 36)
                                    return; // skip if too far away from npc

                                activateNpcForPlayer(dummyNpc.id, p);
                            }
                        }
                    }
                });
                // end sync task
            }
        }
    };
}
