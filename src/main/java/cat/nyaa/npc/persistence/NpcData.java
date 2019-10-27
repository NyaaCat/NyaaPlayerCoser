package cat.nyaa.npc.persistence;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NpcData implements ISerializable, Cloneable {
    public NpcData() {
    }

    public NpcData(UUID ownerId, Location loc, String displayName, EntityType entityType, NpcType npcType, String nbtTag) {
        if ((!entityType.isAlive() || !entityType.isSpawnable()) && entityType != EntityType.PLAYER)
            throw new IllegalArgumentException();
        this.ownerId = ownerId;
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.displayName = displayName;
        this.entityType = entityType;
        this.npcType = npcType;
        this.nbtTag = nbtTag;
    }

    public void setChestLocation(Location l) {
        if (l == null) { // unset location
            chestWorldName = "";
            chestX = chestY = chestZ = 0;
        } else {
            chestWorldName = l.getWorld().getName();
            chestX = l.getBlockX();
            chestY = l.getBlockY();
            chestZ = l.getBlockZ();
        }
    }

    public Location getChestLocation() {
        if (npcType != NpcType.TRADER_BOX || chestWorldName == null || chestWorldName.length() <= 0) {
            return null;
        }
        World w = Bukkit.getWorld(chestWorldName);
        if (w == null) return null; // TODO warn
        Location l = new Location(w, chestX, chestY, chestZ);
        Material m = l.getBlock().getType();
        if (m != Material.CHEST && m != Material.TRAPPED_CHEST && m != Material.SHULKER_BOX) return null; // TODO warn
        return l;
    }

    public Vector getLocationVector() {
        return new Vector(x, y, z);
    }

    /**
     * @deprecated may cause chunk load
     */
    public Location getEntityLocation() {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;
        return new Location(w, x, y, z, 0, 0);
    }

    @Serializable
    public UUID ownerId;
    @Serializable
    public String worldName;
    @Serializable
    public Double x;
    @Serializable
    public Double y;
    @Serializable
    public Double z;
    @Serializable
    public String displayName;
    @Serializable
    public String nbtTag;
    @Serializable
    public EntityType entityType;
    @Serializable
    public boolean enabled = true; // TODO make it effective
    @Serializable
    public NpcType npcType = NpcType.TRADER_UNLIMITED;

    // meaningful for TRADER_BOX and TRADER_UNLIMITED
    @Serializable
    public List<String> trades = new ArrayList<>();

    // meaningful for TRADER_BOX
    @Serializable
    public String chestWorldName = "";
    @Serializable
    public int chestX = 0;
    @Serializable
    public int chestY = 0;
    @Serializable
    public int chestZ = 0;

    // meaningful for HEH_SELL_SHOP
    @Serializable
    public UUID hehShopOwnerUUID;

    // meaningful for entityType == PLAYER
    @Serializable
    public String playerSkin = "default";

    // Travelling Merchants, treat null as DO_NOT_TRAVEL
    @Serializable
    public NpcTravelPlan travelPlan = NpcTravelPlan.DO_NOT_TRAVEL;

    public int chunkX() {
        return ((int) Math.floor(x)) >> 4;
    }

    public int chunkZ() {
        return ((int) Math.floor(z)) >> 4;
    }

    @Override
    public NpcData clone() {
        try {
            NpcData cloned = (NpcData) (super.clone());
            if (this.trades != null) {
                cloned.trades = new ArrayList<>();
                cloned.trades.addAll(this.trades);
                cloned.travelPlan = this.travelPlan.clone();
            }
            return cloned;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    // TODO: traveling merchant specific functions, use specific names
    public static class SelfModificationResponse {
        // request a modification immediately, or request a check in the future
        // but not both

        public SelfModificationResponse(NpcData newDefinition) {
            this.newDefinition = newDefinition;
        }

        public SelfModificationResponse(long nextModTime) {
            this.nextModTime = Math.max(nextModTime, System.currentTimeMillis() + 1);
        }

        public NpcData newDefinition = null;
        public long nextModTime = -1;
    }

    /**
     * Allows the NpcData to change itself every time it's about to spawn
     * Mainly to support the "Traveling Merchant" feature
     * Every time a change is requested, it's handled as an NpcRedefine and
     * will be handled by EntitiesManager.
     *
     * @return null if neither mod this time nor future mod
     */
    public SelfModificationResponse requestSelfModificationBeforeSpawn() {
        if (travelPlan == null || !travelPlan.isTraveller) return null;
        if (travelPlan.isTimeToMove()) {
            NpcData clone = clone();
            clone.travelPlan.doMove(clone);
            return new SelfModificationResponse(clone);
        } else {
            return new SelfModificationResponse(travelPlan.nextMovementTime);
        }
    }

    /**
     * Can suppress the NPC from spawning.
     * Used for the "Traveling Merchant" feature
     */
    public boolean shouldSpawn() {
        if (travelPlan == null) return true;
        if (!travelPlan.isTraveller) return true;
        return travelPlan.isPresent;
    }
}
