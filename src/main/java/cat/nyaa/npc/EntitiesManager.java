package cat.nyaa.npc;

import cat.nyaa.npc.persistance.NpcData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EntitiesManager implements Listener {
    private final BukkitRunnable TICK_LISTENER = new BukkitRunnable() {
        boolean enabled = true;
        int counter = 0;
        @Override
        public void run() {
            if (enabled) {
                if (counter++ >= 10) {
                    counter = 0;
                    onTick(true);
                } else {
                    onTick(false);
                }
            } else {
                super.cancel();
            }
        }

        @Override
        public synchronized void cancel() throws IllegalStateException {
            super.cancel();
            enabled = false; // in case the buggy cancel() not works.
        }
    };

    private final NyaaPlayerCoser plugin;
    private final Queue<String> pendingEntityCreation = new LinkedList<>();
    private final Map<String, LivingEntity> tracedEntities = new HashMap<>(); // Map<NpcId, Entity>

    /**
     * EntitiesManager will register its own TICK_LISTENER.
     * It's important to {@link EntitiesManager#stopTickListener}
     * when reloading the plugin.
     */
    public EntitiesManager(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
        TICK_LISTENER.runTaskTimer(plugin, 20L, 2L);
    }

    public void stopTickListener() {
        TICK_LISTENER.cancel();
    }

    /**
     * Update only one entity each tick.
     */
    public void onTick(boolean isMajorTick) {
        if (isMajorTick) {
            // remove deleted npc
            // or spawn newly added npc
            Set<String> livingSet = tracedEntities.keySet();
            Set<String> definitionSet = plugin.cfg.npcData.npcList.keySet();
            for (String npcId : livingSet) {
                if (!definitionSet.contains(npcId)) {
                    tracedEntities.remove(npcId).remove();
                }
            }
            for (String npcId : definitionSet) {
                if (!livingSet.contains(npcId)) {
                    pendingEntityCreation.add(npcId);
                }
            }
        }

        while (pendingEntityCreation.size() > 0) {
            String npcId = pendingEntityCreation.remove();
            NpcData data = plugin.cfg.npcData.npcList.get(npcId);
            if (data == null) continue;
            LivingEntity e = (LivingEntity)data.location.getWorld().spawnEntity(data.location, data.type);
            e.setCustomName(data.displayName);
            e.setCustomNameVisible(true);
            tracedEntities.put(npcId, e);
            return;
        }
    }
}
