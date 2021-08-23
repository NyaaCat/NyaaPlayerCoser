package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.persistence.NpcData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Abstraction layer for animal-based NPCs and player-based NPCs
 */
public abstract class NPCBase {
    public static final String SCOREBOARD_TAG_PREFIX = "nyaa_npc_id:";
    public static final String NPC_SPAWN_TEMPORARY_SCOREBOARD_TAG = "nyaa_npc_spawning";

    public abstract Integer getEntityId();

    /**
     * Check if the entity is a NyaaNPC
     */
    public static boolean isNyaaNPC(Entity e) {
        if (e == null) return false;
        return getNyaaNpcId(e) != null;
    }

    /**
     * @param e the entity
     * @return npcid if it is a NyaaNPC, null otherwise
     */
    public static String getNyaaNpcId(Entity e) {
        if (e == null) return null;
        for (String s : e.getScoreboardTags()) {
            if (s.startsWith(SCOREBOARD_TAG_PREFIX)) {
                return s.substring(SCOREBOARD_TAG_PREFIX.length());
            }
        }
        return null;
    }

    public final String id;
    public final NpcData data;

    public static NPCBase fromNpcData(String id, NpcData data) {
        if (data.entityType == EntityType.PLAYER) {
            return new NPCPlayer(id, data);
        } else {
            return new NPCLiving(id, data);
        }
    }

    protected NPCBase(String id, NpcData data) {
        this.id = id;
        this.data = data;
    }

    /**
     * Notified by EntitiesManager when a player enters the view range
     *
     * @param p the player who enters the range
     */
    public abstract void onPlayerEnterRange(Player p);

    /**
     * Notified by EntitiesManager when a player leaves the view range
     *
     * @param p the player who leaves the range
     */
    public abstract void onPlayerLeaveRange(Player p);

    /**
     * Notified by EntitiesManager when a potential corresponding entity is removed/died
     * No auto-spawn should be done
     *
     * @param e the removed entity
     */
    public abstract void onEntityRemove(Entity e);


    /**
     * remove spawned/traced entity, only if the given e matches the traced e
     * if e is null, remove any traced entity
     * return true if the entity is successfully removed
     */
    public abstract boolean despawn(Entity e);

    /**
     * Spawn the corresponding npc entity.
     * No chunk-loaded check will be done.
     * NPCLiving use this
     */
    public abstract void spawn();

    public abstract void setPitchYaw(Float pitch, Float yaw);

    /**
     * Get entity's eye location
     *
     * @return null if entity does not exists
     */
    public abstract Location getEyeLocation();

    public abstract Entity getUnderlyingSpawnedEntity();

    /**
     * periodically called by EntitiesManager.
     * <p>
     * return true if anything goes wrong and requires a respawn
     */
    public abstract SanityCheckResult doSanityCheck();

    public enum SanityCheckResult {
        SKIPPED, // nothing is done, may check one more
        CHECKED, // entity checked and considered good or minor errors corrected
        TAINTED  // inconsistencies found and requires a respawn
    }


}
