package cat.nyaa.npc;

import cat.nyaa.npc.persistance.NpcData;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * A NPC entity should never be stored in disk files.
 * i.e. Entities should be removed when chunk unloads
 *      and respawn on chunk load.
 * Note the methods here should never call AIController directly.
 * TODO : Inventory Window Sync (by events maybe)
 */
public class EntitiesManager implements Listener {
    public static final String METADATA_KEY = "NyaaNPC";
    public static final String NPC_NAME_PREFIX = ChatColor.translateAlternateColorCodes('&',"&a&5&4&e&r");
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
    private final BukkitRunnable npcRespawnTask = new BukkitRunnable() {
        @Override
        public void run() {
            resetAllNpcStatus();
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
        npcRespawnTask.runTaskTimer(plugin, 300L, 300L);
    }

    public void destructor() {
        TICK_LISTENER.cancel();
        pendingEntityCreation.clear();
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

            // check spawning conditions
            NpcData data = plugin.cfg.npcData.npcList.get(npcId);
            if (data == null) continue;
            World w = Bukkit.getWorld(data.worldName);
            if (w == null) continue;
            if (!w.isChunkLoaded(data.chunkX(), data.chunkZ())) continue;
            Location loc = new Location(w, data.x, data.y, data.z);

            // spawn
            LivingEntity e = (LivingEntity) w.spawnEntity(loc, data.type);
            if (data.nbtTag != null && data.nbtTag.length() > 0) {
                NmsUtils.setEntityTag(e, data.nbtTag);
            }

            // post spawn customization
            e.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, npcId));
            e.setCustomName(NPC_NAME_PREFIX + data.displayName);
            e.setCustomNameVisible(true);
            e.setAI(false);
            e.setCollidable(false);
            e.setRemoveWhenFarAway(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            if (e instanceof Ageable) {
                ((Ageable) e).setAdult();
            }
            if (e instanceof Zombie) {
                ((Zombie) e).setBaby(false);
            }
            tracedEntities.put(npcId, e);
            return;
        }
    }


    /**
     * Scan all worlds and remove npc entities regardless they are traced or not.
     */
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
     * Remove then respawn one NPC
     */
    public void forceRespawnNpc(String npcId) {
        if (tracedEntities.containsKey(npcId)) {
            tracedEntities.remove(npcId).remove();
        }
        pendingEntityCreation.add(npcId);
    }

    public void resetAllNpcStatus() {
        ArrayList<String> diedNpcEntities = new ArrayList<String>();
        for (String key : tracedEntities.keySet()) {
            LivingEntity entity = tracedEntities.get(key);
            if (entity != null && entity.isValid()) {
                entity.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(entity::removePotionEffect);
                entity.setHealth(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                entity.teleport(plugin.cfg.npcData.npcList.get(key).getLocation());
            } else {
                diedNpcEntities.add(key);
            }
        }
        diedNpcEntities.forEach(this::forceRespawnNpc);
    }

    /**
     * When an new NPC is created, first save to config then spawn it.
     *
     * @param data npcData
     * @return npcId
     */
    public String createNpcDefinition(NpcData data) {
        String npcId = plugin.cfg.npcData.addNpc(data);
        pendingEntityCreation.add(npcId);
        return npcId;
    }

    /**
     * Update then respawn the NPC.
     * Exception thrown if not exists
     */
    public void replaceNpcDefinition(String npcId, NpcData data) {
        plugin.cfg.npcData.replaceNpc(npcId, data);
        forceRespawnNpc(npcId);
    }

    /**
     * Remove the NPC config and the npc entity.
     */
    public void removeNpcDefinition(String npcId) {
        plugin.cfg.npcData.removeNpc(npcId);
        if (tracedEntities.containsKey(npcId)) {
            tracedEntities.remove(npcId).remove();
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
            if (e.isInvulnerable() && e.isCustomNameVisible() && e.getCustomName() != null) {
                if (e.hasMetadata(METADATA_KEY) || e.getCustomName().startsWith(NPC_NAME_PREFIX)) {
                    e.remove();
                }
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
