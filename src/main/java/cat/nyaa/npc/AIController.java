package cat.nyaa.npc;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Merchant;

import java.util.HashSet;
import java.util.Set;

public class AIController implements Listener {
    private final NyaaPlayerCoser plugin;

    /**
     * AIController keeps track of all the trading windows.
     * It's important to {@link AIController#destructor}
     * when reloading the plugin.
     */
    public AIController(NyaaPlayerCoser plugin) {
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

    // TODO: chest inventory
}
