package cat.nyaa.npc;

import cat.nyaa.npc.persistance.NpcData;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class EntitiesManager implements Listener {
    public static final String METADATA_KEY = "NyaaNPC";
    public static final long TICK_FREQUENCY = 2; // onTick() will be called every 2 ticks

    private final BukkitRunnable TICK_LISTENER = new BukkitRunnable() {
        boolean enabled = true;

        @Override
        public void run() {
            if (enabled) {
                onTick();
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
    private final BiMap<String, LivingEntity> tracedEntities = HashBiMap.create(); // Map<NpcId, Entity>

    /**
     * EntitiesManager will register its own TICK_LISTENER.
     * It's important to {@link EntitiesManager#destructor}
     * when reloading the plugin.
     */
    public EntitiesManager(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
        forceRespawnAllNpc();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        TICK_LISTENER.runTaskTimer(plugin, 20L, TICK_FREQUENCY);
    }

    public void destructor() {
        TICK_LISTENER.cancel();
        for (LivingEntity e : tracedEntities.values()) {
            e.remove();
        }
        tracedEntities.clear();
        HandlerList.unregisterAll(this);
    }

    /**
     * Scan through the pendingEntityCreation queue.
     * At most one entity spawned per tick.
     * NPCs not in a loaded chunk will not be spawned and will be removed from the queue
     */
    public void onTick() {
        while (pendingEntityCreation.size() > 0) {
            String npcId = pendingEntityCreation.remove();
            NpcData data = plugin.cfg.npcData.npcList.get(npcId);
            if (data == null) continue;
            World w = Bukkit.getWorld(data.worldName);
            if (w == null) continue;
            if (!w.isChunkLoaded(data.chunkX(), data.chunkZ())) continue;
            Location loc = new Location(w, data.x, data.y, data.z);

            LivingEntity e = (LivingEntity) w.spawnEntity(loc, data.type);
            e.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, npcId));
            e.setCustomName(data.displayName);
            e.setCustomNameVisible(true);
            e.setAI(false);
            e.setCollidable(false);
            e.setRemoveWhenFarAway(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            tracedEntities.put(npcId, e);
            return;
        }
    }

    /**
     * When an new NPC is created, first save to config then spawn it.
     *
     * @param data npcData
     * @return npcId
     */
    public String createNPC(NpcData data) {
        String npcId = plugin.cfg.npcData.addNpc(data);
        pendingEntityCreation.add(npcId);
        return npcId;
    }

    public void forceRespawnAllNpc() {
        pendingEntityCreation.clear();
        tracedEntities.clear();
        for (World w : Bukkit.getServer().getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (isNyaaNPC(e)) {
                    e.remove();
                }
            }
        }

        for (Map.Entry<String, NpcData> e : plugin.cfg.npcData.npcList.entrySet()) {
            NpcData data = e.getValue();
            World w = Bukkit.getWorld(data.worldName);
            if (w == null) continue;
            if (!w.isChunkLoaded(data.chunkX(), data.chunkZ())) continue;
            pendingEntityCreation.add(e.getKey());
        }
    }

    /**
     * When a chunk loads, scan for (possible) NPC entities and remove them.
     * Then add NPCs that are in the chunk into creationPendingList.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent ev) {
        if (ev.isNewChunk()) return;
        for (Entity e : ev.getChunk().getEntities()) {
            if (e.hasMetadata(METADATA_KEY)) {
                e.remove();
            }
        }
        pendingEntityCreation.addAll(plugin.cfg.npcData.getNpcInChunk(ev.getWorld().getName(), ev.getChunk().getX(), ev.getChunk().getZ()).keySet());
    }

    /**
     * When a chunk unloads, remove all NPC entities from both the chunk and tracking map.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnLoad(ChunkUnloadEvent ev) {
        for (Entity e : ev.getChunk().getEntities()) {
            if (e.hasMetadata(METADATA_KEY)) {
                tracedEntities.inverse().remove(e);
                e.remove();
            }
        }
    }

    /**
     * Respawn NPC in case they are dead
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityRemoval(EntityDeathEvent ev) {
        if (isNyaaNPC(ev.getEntity())) {
            ev.setDroppedExp(0);
            ev.getDrops().clear();
            String id = getNyaaNpcId(ev.getEntity());
            tracedEntities.remove(id);
            pendingEntityCreation.add(id);
        }
    }

    /**
     * Check if the entity is an NyaaNPC
     */
    public static boolean isNyaaNPC(Entity e) {
        return (e instanceof LivingEntity) && (e.hasMetadata(METADATA_KEY));
    }

    public static String getNyaaNpcId(Entity e) {
        return e.getMetadata(METADATA_KEY).get(0).asString();
    }
}
