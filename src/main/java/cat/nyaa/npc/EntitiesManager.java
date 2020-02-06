package cat.nyaa.npc;

import cat.nyaa.npc.ephemeral.NPCBase;
import cat.nyaa.npc.events.NpcDefinedEvent;
import cat.nyaa.npc.events.NpcRedefinedEvent;
import cat.nyaa.npc.events.NpcUndefinedEvent;
import cat.nyaa.npc.persistence.NpcData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static cat.nyaa.npc.ephemeral.NPCBase.*;

/**
 * In charge of entity events: spawn, despawn, etc
 * TODO: maybe CommandHandler should be in charge of calling TradingController to refresh player view
 *       rather than create/replace/removeNpcDefinition
 *
 * A NPC entity should never be stored in disk files.
 * i.e. Entities should be removed when chunk unloads
 * and respawn on chunk load.
 * Note the methods here should never call TradingController directly.
 */
public class EntitiesManager implements Listener {
    /* ********************* */
    /*        Init           */
    /* ********************* */
    public static final long TICK_FREQUENCY = 2;         // onTick() will be called every 2 ticks
    public static final long PER_TICK_UPDATE_LIMIT = 5; // how many NPCs can be updated every tick, at most

    private final NyaaPlayerCoser plugin;

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
        for (NPCBase npc : idNpcMapping.values()) {
            npc.despawn(null);
        }
        idNpcMapping.clear();
        HandlerList.unregisterAll(this);
    }


    /* ********************* */
    /* Ephemeral-Persistence */
    /* ********************* */

    // npc id to npcbase
    // Should be the same as config data ideally
    // Contains all the runtime info for NPCs besides config
    public Map<String, NPCBase> idNpcMapping = new HashMap<>();

    /**
     * When an new NPC is created, first save to config then spawn it.
     *
     * @param data npcData
     * @return npcId
     */
    public String createNpcDefinition(NpcData data) {
        String npcId = plugin.cfg.npcData.addNpc(data);
        idNpcMapping.put(npcId, NPCBase.fromNpcData(npcId, data));
        pendingEntityCreation.add(npcId);
        Bukkit.getServer().getPluginManager().callEvent(new NpcDefinedEvent(npcId, data));
        return npcId;
    }

    /**
     * Update then respawn the NPC.
     * Exception thrown if not exists
     */
    public void replaceNpcDefinition(String npcId, NpcData data) {
        idNpcMapping.get(npcId).despawn(null);
        NpcData oldData = plugin.cfg.npcData.replaceNpc(npcId, data);
        idNpcMapping.put(npcId, NPCBase.fromNpcData(npcId, data));
        forceRespawnNpc(npcId);
        Bukkit.getServer().getPluginManager().callEvent(new NpcRedefinedEvent(npcId, oldData, data));
    }

    /**
     * Remove the NPC config and the npc entity.
     */
    public void removeNpcDefinition(String npcId) {
        idNpcMapping.remove(npcId).despawn(null);
        NpcData oldData = plugin.cfg.npcData.removeNpc(npcId);
        Bukkit.getServer().getPluginManager().callEvent(new NpcUndefinedEvent(npcId, oldData));
    }

    /* ********************** */
    /* Ephemeral Manipulation */
    /* ********************** */

    /**
     * Forcefully sync idNpcMapping with config file,
     * also used for loading config.
     * Scan all worlds and remove npc entities regardless they are traced or not.
     */
    public void forceRespawnAllNpc() {
        pendingEntityCreation.clear();
        for (NPCBase npc : idNpcMapping.values()) {
            npc.despawn(null);
        }

        idNpcMapping.clear();

        for (World w : Bukkit.getServer().getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (isNyaaNPC(e)) {
                    e.remove();
                }
            }
        }

        for (Map.Entry<String, NpcData> e : plugin.cfg.npcData.npcList.entrySet()) {
            idNpcMapping.put(e.getKey(), NPCBase.fromNpcData(e.getKey(), e.getValue()));
        }

        pendingEntityCreation.addAll(idNpcMapping.keySet());
    }

    /**
     * Remove then respawn one NPC
     */
    public void forceRespawnNpc(String npcId) {
        if (idNpcMapping.containsKey(npcId)) {
            idNpcMapping.remove(npcId).despawn(null);
        }
        idNpcMapping.put(npcId, NPCBase.fromNpcData(npcId, plugin.cfg.npcData.npcList.get(npcId)));
        pendingEntityCreation.add(npcId);
    }

    /**
     * Automatically adjust npc location TODO
     *
     * @param npcId
     * @param operator
     */
    public void adjustNpcLocation(String npcId, CommandSender operator) {
//        if (!tracedEntities.containsKey(npcId)) {
//            operator.sendMessage(I18n.format("user.adjust.not_spawned"));
//        } else {
//            final LivingEntity le = tracedEntities.get(npcId);
//            le.setAI(true);
//            new BukkitRunnable() {
//                static final int SUCCESS_COUNT_REQ = 2;
//                static final int FAILURE_COUNT_REQ = 5;
//                int success_counter = SUCCESS_COUNT_REQ;
//                int failing_counter = FAILURE_COUNT_REQ;
//                Vector previous_location = null;
//
//                @Override
//                public void run() {
//                    if (!tracedEntities.containsValue(le)) {
//                        operator.sendMessage(I18n.format("user.adjust.despawned"));
//                        cancel();
//                    } else {
//                        Vector newLocation = le.getLocation().toVector();
//                        if (previous_location == null) {
//                            operator.sendMessage(I18n.format("user.adjust.sampling", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
//                        } else if (newLocation.equals(previous_location)) {
//                            operator.sendMessage(I18n.format("user.adjust.sampling_ok", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
//                            success_counter--;
//                        } else {
//                            operator.sendMessage(I18n.format("user.adjust.sampling_fail", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
//                            success_counter = SUCCESS_COUNT_REQ;
//                            failing_counter--;
//                        }
//                        if (success_counter <= 0) {
//                            operator.sendMessage(I18n.format("user.adjust.success", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
//                            NpcData data = plugin.cfg.npcData.npcList.get(npcId);
//                            if (data != null) {
//                                data.x = newLocation.getX();
//                                data.y = newLocation.getY();
//                                data.z = newLocation.getZ();
//                                replaceNpcDefinition(npcId, data);
//                            }
//                            cancel();
//                        } else if (failing_counter <= 0) {
//                            operator.sendMessage(I18n.format("user.adjust.fail"));
//                            forceRespawnNpc(npcId);
//                            cancel();
//                        }
//                        previous_location = newLocation;
//                    }
//                }
//            }.runTaskTimer(plugin, 10L, 20L);
//        }
    }


    /* ********************* */
    /*  Looping scheduler    */
    /* ********************* */
    private final BukkitRunnable TICK_LISTENER = new BukkitRunnable() {
        boolean enabled = true;

        @Override
        public void run() {
            if (enabled) {
                sanityCheckTick();
                entitySpawnTick();
                updateNpcDirectionTick();
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

    /* ********************* */
    /*  Entity Spawn Loop    */
    /* ********************* */
    private final Queue<String> pendingEntityCreation = new LinkedList<>();

    void entitySpawnTick() {
        int updated_counter = 0;
        while (pendingEntityCreation.size() > 0 && updated_counter < PER_TICK_UPDATE_LIMIT) {
            String npcId = pendingEntityCreation.remove();

            // check spawning conditions
            NPCBase npc = idNpcMapping.get(npcId);
            NpcData data = npc.data;
            if (data == null) continue;

            // self modification npc data
            NpcData.SelfModificationResponse rsp = data.requestSelfModificationBeforeSpawn();
            if (rsp != null) {
                if (rsp.newDefinition != null) {
                    replaceNpcDefinition(npcId, rsp.newDefinition);
                    continue;
                } else {
                    startTravelTimer(npcId, rsp.nextModTime);
                }
            }
            if (!data.shouldSpawn()) {
                npc.despawn(null);
                continue;
            }

            World w = Bukkit.getWorld(data.worldName);
            if (w == null) continue;
            if (!w.isChunkLoaded(data.chunkX(), data.chunkZ())) continue;

            npc.despawn(null);
            npc.spawn();
            updated_counter++;
        }
    }

    /* ********************* */
    /* Update direction loop */
    /* ********************* */

    private final Queue<NPCBase> pendingUpdateLookDirection = new LinkedList<>();
    private Set<NPCBase> recentlyObservedNpcs = new HashSet<>();

    void updateNpcDirectionTick() {
        Set<NPCBase> observedNpcs = new HashSet<>();

        for (NPCBase npc : recentlyObservedNpcs) {
            Location eyeLoc = npc.getEyeLocation();
            if (eyeLoc == null) continue;
            Vector newDirection = getNearestPlayerDirection(npc.getEyeLocation(), 3, 2, 3);
            if (newDirection == null) {
                npc.setPitchYaw(0F, null);
            } else {
                eyeLoc.setDirection(newDirection);
                npc.setPitchYaw(eyeLoc.getPitch(), eyeLoc.getYaw());
                observedNpcs.add(npc);
            }
        }

        if (pendingUpdateLookDirection.isEmpty()) {
            pendingUpdateLookDirection.addAll(idNpcMapping.values());
        }

        int updated_counter = 0;
        while (!pendingUpdateLookDirection.isEmpty() && updated_counter < PER_TICK_UPDATE_LIMIT) {
            NPCBase npc = pendingUpdateLookDirection.poll();
            Location eyeLoc = npc.getEyeLocation();
            if (eyeLoc == null) continue;
            Vector vec = getNearestPlayerDirection(npc.getEyeLocation(), 3, 2, 3);
            if (vec == null) {
                npc.setPitchYaw(0F, null);
            } else {
                eyeLoc.setDirection(vec);
                npc.setPitchYaw(eyeLoc.getPitch(), eyeLoc.getYaw());
                observedNpcs.add(npc);
            }
            updated_counter++;
        }
        recentlyObservedNpcs = observedNpcs;
    }

    /* ********************* */
    /* Sanity check loop     */
    /* ********************* */
    private final Queue<NPCBase> pendingSanityCheckLocation = new LinkedList<>();

    void sanityCheckTick() {
        if (pendingSanityCheckLocation.isEmpty()) {
            pendingSanityCheckLocation.addAll(idNpcMapping.values());
        }

        while (!pendingSanityCheckLocation.isEmpty()) {
            NPCBase npc = pendingSanityCheckLocation.poll();

            NPCBase.SanityCheckResult result = npc.doSanityCheck();
            if (result == NPCBase.SanityCheckResult.SKIPPED) {
                continue;
            } else if (result == NPCBase.SanityCheckResult.CHECKED) {
                break;
            } else if (result == NPCBase.SanityCheckResult.TAINTED) {
                forceRespawnNpc(npc.id);
                break;
            }
        }
    }

    /* ********************** */
    /* Traveling NPC specific */
    /* ********************** */
    private Map<String, BukkitRunnable> scheduledTravelTimers = new HashMap<>();

    private void startTravelTimer(String npcId, long nextTravelTime) {
        int delay = (int) Math.ceil((nextTravelTime - System.currentTimeMillis()) / 50.0);
        if (delay < 20) delay = 20;
        if (scheduledTravelTimers.containsKey(npcId)) {
            scheduledTravelTimers.remove(npcId).cancel();
        }
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                doTravelTimer(npcId);
            }
        };
        r.runTaskTimer(plugin, delay, 40L);
        scheduledTravelTimers.put(npcId, r);
    }

    private void stopTravelTimer(String npcId) {
        if (scheduledTravelTimers.containsKey(npcId)) {
            scheduledTravelTimers.remove(npcId).cancel();
        }
    }

    private void doTravelTimer(String npcId) {
        NPCBase npc = idNpcMapping.get(npcId);
        if (npc == null) {
            stopTravelTimer(npcId);
            return;
        }
        Location eyeLoc = npc.getEyeLocation();
        if (eyeLoc == null) {  // not spawned implies no eye location
            pendingEntityCreation.add(npcId);
            stopTravelTimer(npcId);
        } else {  // make sure no players around
            World w = eyeLoc.getWorld();
            if (w == null) {  // wtf?
                // respawn anyway
            } else {
                for (Player p : w.getPlayers()) {
                    double dst = p.getLocation().distanceSquared(eyeLoc);
                    if (dst < 2500) return; // too close, not respawn
                }
            }
            pendingEntityCreation.add(npcId);
            stopTravelTimer(npcId);
        }
    }

    /**
     * Eye-to-Eye vector to the closest player
     *
     * @return a vector, or null if no player to look at
     */
    private static Vector getNearestPlayerDirection(Location npcEyeLocation, double scanX, double scanY, double scanZ) {
        if (npcEyeLocation == null) throw new IllegalArgumentException();
        Collection<Player> nearbyPlayers = (Collection) (npcEyeLocation.getWorld().getNearbyEntities(npcEyeLocation, scanX, scanY, scanZ, (Entity e) -> e instanceof Player));
        if (nearbyPlayers.isEmpty()) return null;
        Player closest_player = null;
        double min_dst = Double.MAX_VALUE;
        for (Player p : nearbyPlayers) {
            double dst = p.getEyeLocation().distanceSquared(npcEyeLocation);
            if (dst < min_dst) {
                closest_player = p;
                min_dst = dst;
            }
        }
        if (closest_player == null) {
            return null;
        } else {
            return closest_player.getEyeLocation().subtract(npcEyeLocation).toVector().normalize();
        }
    }

    /* ************************** */
    /*       Event handlers       */
    /* ************************** */

    /**
     * When a chunk loads, scan for (possible) NPC entities and remove them.
     * Then add NPCs that are in the chunk into creationPendingList.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent ev) {
        if (ev.isNewChunk()) return;
        for (Entity e : ev.getChunk().getEntities()) {
            if (isNyaaNPC(e)) {
                e.remove();
            }
        }

        Map<String, NpcData> t = plugin.cfg.npcData.getNpcInChunk(ev.getWorld().getName(), ev.getChunk().getX(), ev.getChunk().getZ());
        pendingEntityCreation.addAll(t.keySet());
    }

    /**
     * When a chunk unloads, remove all NPC entities from both the chunk and tracking map.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnLoad(ChunkUnloadEvent ev) {
        NyaaPlayerCoser.debug(log->log.info(String.format("onChunkUnload %s", ev.getChunk())));
        for (Entity e : ev.getChunk().getEntities()) {
            if (!e.isValid() || e.isDead()) continue;
            String id = getNyaaNpcId(e);
            if (id != null) {
                if (idNpcMapping.containsKey(id)) {
                    idNpcMapping.get(id).despawn(e);
                } else {
                    NyaaPlayerCoser.trace(log -> log.info(String.format("onChunkUnLoad npcId=%s but not in idNpcMapping", id)));
                    e.remove();
                }
            }
        }
    }

    /**
     * Respawn NPC in case they are dead
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityRemoval(EntityDeathEvent ev) {
        Entity e = ev.getEntity();
        String id = getNyaaNpcId(e);
        if (id != null) {
            idNpcMapping.get(id).onEntityRemove(e);
            ev.setDroppedExp(0);
            ev.getDrops().clear();
            pendingEntityCreation.add(id);
        }
    }

    /**
     * NPCs immune to any damages
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onNPCAttacked(EntityDamageEvent ev) {
        if (isNyaaNPC(ev.getEntity())) {
            ev.setCancelled(true);
        }
    }


    private Map<String, Integer> playerViewDistanceSqPerWorld = new HashMap<>();

    private int getViewDistanceSquared(World w) {
        String worldname = w.getName();
        Integer ret = playerViewDistanceSqPerWorld.get(worldname);
        if (ret == null) {
            int def = Bukkit.spigot().getConfig().getInt("world-settings.default.entity-tracking-range.players", 48);
            int r = Bukkit.spigot().getConfig().getInt("world-settings." + worldname + ".entity-tracking-range.players", def);
            playerViewDistanceSqPerWorld.put(worldname, r * r);
            return r * r;
        } else {
            return ret;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent ev) {
        Location playerLoc = ev.getTo();
        for (NPCBase npc : idNpcMapping.values()) {
            if (npc.data.entityType == EntityType.PLAYER && npc.data.worldName.equals(playerLoc.getWorld().getName())) {
                double dst = Math.pow(playerLoc.getX() - npc.data.x, 2) +
                        Math.pow(playerLoc.getZ() - npc.data.z, 2);
                if (dst > getViewDistanceSquared(playerLoc.getWorld())) {
                    npc.onPlayerLeaveRange(ev.getPlayer());
                } else {
                    npc.onPlayerEnterRange(ev.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityCatchFire(EntityCombustEvent ev) {
        if (NPCBase.isNyaaNPC(ev.getEntity())) {
            ev.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreeperCharge(CreeperPowerEvent ev) {
        if (NPCBase.isNyaaNPC(ev.getEntity())) {
            ev.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPigMorph(PigZapEvent ev) {
        if (NPCBase.isNyaaNPC(ev.getEntity())) {
            ev.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent ev) {
        Location loc = ev.getLightning().getLocation();
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            if (NPCBase.isNyaaNPC(e)) {
                ev.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onNpcSpawnDecision(EntitySpawnEvent ev) {
        if (ev.getEntity().getScoreboardTags().contains(NPC_SPAWN_TEMPORARY_SCOREBOARD_TAG)) {
            ev.setCancelled(false); // force spawn npc
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onNpcSpawnDecisionMade(EntitySpawnEvent ev) {
        if (ev.getEntity().getScoreboardTags().contains(NPC_SPAWN_TEMPORARY_SCOREBOARD_TAG)) {
            if (ev.isCancelled()) { // the npc still not spawn
                ev.getEntity().removeScoreboardTag(NPC_SPAWN_TEMPORARY_SCOREBOARD_TAG);
            }
        }
    }
}
