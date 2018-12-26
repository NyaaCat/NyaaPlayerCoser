package cat.nyaa.npc;

import cat.nyaa.npc.events.NpcDefinedEvent;
import cat.nyaa.npc.events.NpcRedefinedEvent;
import cat.nyaa.npc.events.NpcUndefinedEvent;
import cat.nyaa.npc.persistance.NpcData;
import cat.nyaa.nyaacore.utils.NmsUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * A NPC entity should never be stored in disk files.
 * i.e. Entities should be removed when chunk unloads
 * and respawn on chunk load.
 * Note the methods here should never call TradingController directly.
 */
public class EntitiesManager implements Listener {
    public static final String SCOREBOARD_TAG_PREFIX = "nyaa_npc_id:";
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
    private final Queue<LivingEntity> pendingUpdateLookDirection = new LinkedList<>();
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
            e.addScoreboardTag(SCOREBOARD_TAG_PREFIX + npcId);
            e.setCustomName(data.displayName);
            e.setCustomNameVisible(true);
            e.setAI(false);
            e.setCollidable(false);
            e.setRemoveWhenFarAway(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setCanPickupItems(false);
            NmsUtils.setEntityOnGround(e, true);
            e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(new AttributeModifier("immobile_entity", -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            tracedEntities.put(npcId, e);
            return;
        }

        if (pendingUpdateLookDirection.isEmpty()) {
            pendingUpdateLookDirection.addAll(tracedEntities.values());
        }
        while (!pendingUpdateLookDirection.isEmpty()) {
            LivingEntity e = pendingUpdateLookDirection.poll();
            if (e == null || !tracedEntities.containsValue(e))
                continue;
            Vector vec = getNearestPlayerDirection(e, 3, 3, 3);
            if (vec == null) {
                NmsUtils.updateEntityYawPitch(e, null, 0F);
            } else {
                Location loc = e.getLocation().setDirection(vec);
                NmsUtils.updateEntityYawPitch(e, loc.getYaw(), loc.getPitch());
            }
            return;
        }

    }

    private Vector getNearestPlayerDirection(LivingEntity e, double x, double y, double z) {
        List<Entity> l = e.getNearbyEntities(x, y, z);
        LivingEntity le = (LivingEntity) l.stream().filter((t) -> t instanceof Player)
                .sorted((a, b) -> (int) Math.signum(e.getLocation().distanceSquared(a.getLocation()) - e.getLocation().distanceSquared(b.getLocation())))
                .findFirst().orElse(null);
        if (le == null) return null;
        return le.getEyeLocation().subtract(e.getEyeLocation()).toVector().normalize();
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




    /* *************************** */
    /* NPC definition modification */
    /* *************************** */

    /**
     * When an new NPC is created, first save to config then spawn it.
     *
     * @param data npcData
     * @return npcId
     */
    public String createNpcDefinition(NpcData data) {
        String npcId = plugin.cfg.npcData.addNpc(data);
        pendingEntityCreation.add(npcId);
        Bukkit.getServer().getPluginManager().callEvent(new NpcDefinedEvent(npcId, data));
        return npcId;
    }

    /**
     * Update then respawn the NPC.
     * Exception thrown if not exists
     */
    public void replaceNpcDefinition(String npcId, NpcData data) {
        NpcData oldData = plugin.cfg.npcData.replaceNpc(npcId, data);
        forceRespawnNpc(npcId);
        Bukkit.getServer().getPluginManager().callEvent(new NpcRedefinedEvent(npcId, oldData, data));
    }

    /**
     * Remove the NPC config and the npc entity.
     */
    public void removeNpcDefinition(String npcId) {
        NpcData oldData = plugin.cfg.npcData.removeNpc(npcId);
        if (tracedEntities.containsKey(npcId)) {
            tracedEntities.remove(npcId).remove();
        }
        Bukkit.getServer().getPluginManager().callEvent(new NpcUndefinedEvent(npcId, oldData));
    }

    /**
     * Automatically adjust npc location
     * @param npcId
     * @param operator
     */
    public void adjustNpcLocation(String npcId, CommandSender operator) {
        if (!tracedEntities.containsKey(npcId)) {
            operator.sendMessage(I18n.format("user.adjust.not_spawned"));
        } else {
            final LivingEntity le = tracedEntities.get(npcId);
            le.setAI(true);
            new BukkitRunnable() {
                static final int SUCCESS_COUNT_REQ = 2;
                static final int FAILURE_COUNT_REQ = 5;
                int success_counter = SUCCESS_COUNT_REQ;
                int failing_counter = FAILURE_COUNT_REQ;
                Vector previous_location = null;

                @Override
                public void run() {
                    if (!tracedEntities.containsValue(le)) {
                        operator.sendMessage(I18n.format("user.adjust.despawned"));
                        cancel();
                    } else {
                        Vector newLocation = le.getLocation().toVector();
                        if (previous_location == null) {
                            operator.sendMessage(I18n.format("user.adjust.sampling", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                        } else if (newLocation.equals(previous_location)) {
                            operator.sendMessage(I18n.format("user.adjust.sampling_ok", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                            success_counter--;
                        } else {
                            operator.sendMessage(I18n.format("user.adjust.sampling_fail", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                            success_counter = SUCCESS_COUNT_REQ;
                            failing_counter--;
                        }
                        if (success_counter <= 0) {
                            operator.sendMessage(I18n.format("user.adjust.success", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                            NpcData data = plugin.cfg.npcData.npcList.get(npcId);
                            if (data != null) {
                                data.x = newLocation.getX();
                                data.y = newLocation.getY();
                                data.z = newLocation.getZ();
                                replaceNpcDefinition(npcId, data);
                            }
                            cancel();
                        } else if (failing_counter <= 0) {
                            operator.sendMessage(I18n.format("user.adjust.fail"));
                            forceRespawnNpc(npcId);
                            cancel();
                        }
                        previous_location = newLocation;
                    }
                }
            }.runTaskTimer(plugin, 10L, 20L);
        }
    }

    /* ************************** */
    /*       Event handlers       */
    /* ************************** */

    /**
     * When a chunk loads, scan for (possible) NPC entities and remove them.
     * Then add NPCs that are in the chunk into creationPendingList.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent ev) {
        if (ev.isNewChunk()) return;
        for (Entity e : ev.getChunk().getEntities()) {
            if (isNyaaNPC(e)) {
                e.remove();
            }
        }
        Map<String, NpcData> t = plugin.cfg.npcData.getNpcInChunk(ev.getWorld().getName(), ev.getChunk().getX(), ev.getChunk().getZ());
//        for (String s : t.keySet()) {
//            plugin.getLogger().info("ENQUEUE: " + s);
//        }
        pendingEntityCreation.addAll(t.keySet());
    }

    /**
     * When a chunk unloads, remove all NPC entities from both the chunk and tracking map.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnLoad(ChunkUnloadEvent ev) {
        for (Entity e : ev.getChunk().getEntities()) {
            if (isNyaaNPC(e)) {
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
     * NPCs immune to any damages
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNPCAttacked(EntityDamageEvent ev) {
        if (isNyaaNPC(ev.getEntity())) {
            ev.setCancelled(true);
        }
    }





    /* ************************** */
    /* Auxiliary static functions */
    /* ************************** */

    /**
     * Check if the entity is a NyaaNPC
     */
    public static boolean isNyaaNPC(Entity e) {
        return getNyaaNpcId(e) != null;
    }

    /**
     * @param e the entity
     * @return npcid if it is a NyaaNPC, null otherwise
     */
    public static String getNyaaNpcId(Entity e) {
        for (String s : e.getScoreboardTags()) {
            if (s.startsWith(SCOREBOARD_TAG_PREFIX)) {
                return s.substring(SCOREBOARD_TAG_PREFIX.length());
            }
        }
        return null;
    }
}
