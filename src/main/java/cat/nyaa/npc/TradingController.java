package cat.nyaa.npc;

import cat.nyaa.npc.ephemeral.NPCBase;
import cat.nyaa.npc.ephemeral.NPCPlayer;
import cat.nyaa.npc.ephemeral.NyaaMerchant;
import cat.nyaa.npc.ephemeral.NyaaMerchantRecipe;
import cat.nyaa.npc.events.NpcRedefinedEvent;
import cat.nyaa.npc.events.NpcUndefinedEvent;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.TradeData;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import net.minecraft.server.v1_13_R2.EnumHand;
import net.minecraft.server.v1_13_R2.PacketPlayInUseEntity;
import org.bukkit.Bukkit;
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
        String npcId = NPCBase.getNyaaNpcId(ev.getRightClicked());
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

    private void activateNpcForPlayer(String npcId, Player p) {
        NpcData data = plugin.cfg.npcData.npcList.get(npcId);
        if (data == null) {
            plugin.getLogger().warning(String.format("Cannot activate npc %s for player %s : Cannot find NPC definition", npcId, p.getName()));
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

    @EventHandler(ignoreCancelled = true)
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
                if (ev.getWhoClicked().isOp()) {
                    ev.getWhoClicked().sendMessage("supplied item1: " + TradeData.itemDesc(mInv.getItem(0)));
                    ev.getWhoClicked().sendMessage("supplied item2: " + TradeData.itemDesc(mInv.getItem(1)));
                }

                try {
                    MerchantRecipe recipe = m.getRecipe(mInv.getSelectedRecipeIndex());
                    if (recipe instanceof NyaaMerchantRecipe) {
                        NyaaMerchantRecipe nyaaRecipe = (NyaaMerchantRecipe) recipe;
                        TradeData d = nyaaRecipe.getTradeData();

                        // FIXME debug only
                        if (ev.getWhoClicked().isOp()) {
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

    private final PacketListener onRightClickFakePlayer = new PacketAdapter(NyaaPlayerCoser.instance, ListenerPriority.NORMAL,
            USE_ENTITY) {
        @Override
        public void onPacketReceiving(PacketEvent event) {
            if (event.getPacketType() == USE_ENTITY) {
                int entityId = event.getPacket().getIntegers().read(0);

                NPCPlayer dummyNpc = NPCPlayer.spawnedDummyNPCs.get(entityId);
                if (dummyNpc != null) {
                    event.setCancelled(true);
                    PacketPlayInUseEntity.EnumEntityUseAction action = event.getPacket().getEnumModifier(PacketPlayInUseEntity.EnumEntityUseAction.class, 1).read(0);
                    if (action == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
                        EnumHand hand = event.getPacket().getEnumModifier(EnumHand.class, 3).read(0);

                        if (hand == EnumHand.MAIN_HAND) {
                            Player p = event.getPlayer();
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
            }
        }
    };
}
