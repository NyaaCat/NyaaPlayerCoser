package cat.nyaa.npc;

import cat.nyaa.npc.events.NpcRedefinedEvent;
import cat.nyaa.npc.events.NpcUndefinedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Merchant;

import java.util.HashSet;
import java.util.Set;

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
    }

    public void destructor() {
        for (InventoryView iv : openedNPCWindow) {
            iv.close();
        }
    }

    private final Set<InventoryView> openedNPCWindow = new HashSet<>();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractNPC(PlayerInteractEntityEvent ev) {
        if (EntitiesManager.isNyaaNPC(ev.getRightClicked())) {
            ev.setCancelled(true);
            String npcId = EntitiesManager.getNyaaNpcId(ev.getRightClicked());
            Merchant m = plugin.cfg.npcData.npcList.get(npcId).getMerchant();
            InventoryView iv = ev.getPlayer().openMerchant(m, false);
            openedNPCWindow.add(iv);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCloseWindow(InventoryCloseEvent ev) {
        openedNPCWindow.remove(ev.getView());
    }

    @EventHandler
    public void onNpcUndefined(NpcUndefinedEvent ev) {

    }

    @EventHandler
    public void onNpcModified(NpcRedefinedEvent ev) {

    }

    public void onPlayerInteractWithWindow(InventoryClickEvent ev) {
        if (openedNPCWindow.contains(ev.getView())) {
            ev.getWhoClicked().sendMessage("Clicked on: " + ev.getClickedInventory().toString());
        }
    }

    // TODO: chest inventory
}
