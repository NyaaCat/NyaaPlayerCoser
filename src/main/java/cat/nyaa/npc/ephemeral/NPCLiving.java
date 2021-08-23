package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NPCLiving extends NPCBase {
    private LivingEntity spawnedEntity = null;

    public NPCLiving(String id, NpcData data) {
        super(id, data);
    }

    @Override
    public Integer getEntityId() {
        if (spawnedEntity == null) return null;
        return spawnedEntity.getEntityId();
    }

    @Override
    public void onPlayerEnterRange(Player p) {
    }

    @Override
    public void onPlayerLeaveRange(Player p) {
    }

    @Override
    public void onEntityRemove(Entity e) {
        NyaaPlayerCoser.trace(log -> log.info(String.format("NPCLiving::onEntityRemove(%s:%s)%s", e, System.identityHashCode(e), data.displayName)));
        if (spawnedEntity == e) {
            spawnedEntity = null;
        }
    }

    @Override
    public boolean despawn(Entity e) {
        if (spawnedEntity == null) return true;
        NyaaPlayerCoser.trace(log -> log.info(String.format("NPCLiving::despawn() %s:%s %s", spawnedEntity, System.identityHashCode(spawnedEntity), data.displayName)));
        if (e != null && e != spawnedEntity) return false;
        spawnedEntity.remove();
        spawnedEntity = null;
        return true;
    }

    @Override
    public void spawn() {
        // check spawning conditions
        if (data == null) throw new RuntimeException("missing npc data");
        World w = Bukkit.getWorld(data.worldName);
        if (w == null) throw new RuntimeException("invalid world");
        Location loc = new Location(w, data.x, data.y, data.z);

        // spawn
        LivingEntity e = (LivingEntity) w.spawn(loc, data.entityType.getEntityClass(),
                ee -> ee.addScoreboardTag(NPC_SPAWN_TEMPORARY_SCOREBOARD_TAG));
        if (data.nbtTag != null && data.nbtTag.length() > 0) {
            NmsUtils.setEntityTag(e, data.nbtTag);
        }
        if (!e.getScoreboardTags().contains(NPC_SPAWN_TEMPORARY_SCOREBOARD_TAG) || Bukkit.getEntity(e.getUniqueId()) == null) {
            // some how failed to spawn
            e.remove();
            return;
        } else {
            e.removeScoreboardTag(NPC_SPAWN_TEMPORARY_SCOREBOARD_TAG);
        }

        // post spawn customization
        e.addScoreboardTag(SCOREBOARD_TAG_PREFIX + id);
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
        spawnedEntity = e;
        NyaaPlayerCoser.debug(log -> log.info(String.format("NPCLiving::spawn() %s:%s %s", e, System.identityHashCode(e), data.displayName)));
    }

    @Override
    public void setPitchYaw(Float pitch, Float yaw) {
        if (data.entityType == EntityType.PHANTOM) {
            if (pitch != null) {
                pitch = -pitch;
            }
        }
        if (spawnedEntity != null) {
            NmsUtils.updateEntityYawPitch(spawnedEntity, yaw, pitch);
        }
    }

    @Override
    public Location getEyeLocation() {
        if (spawnedEntity != null) {
            return spawnedEntity.getEyeLocation();
        } else {
            return null;
        }
    }

    @Override
    public Entity getUnderlyingSpawnedEntity() {
        return spawnedEntity;
    }

    @Override
    public SanityCheckResult doSanityCheck() {
        if (spawnedEntity != null) {
            if (spawnedEntity.isDead()) return SanityCheckResult.TAINTED;
            if (spawnedEntity.getType() != data.entityType) return SanityCheckResult.TAINTED;
            spawnedEntity.setFireTicks(0);

            // reset location
            if (spawnedEntity.isInsideVehicle()) spawnedEntity.leaveVehicle();
            Location loc = spawnedEntity.getLocation();
            Location definedLoc = data.getEntityLocation();
            double deltaX = loc.getX() - definedLoc.getX();
            double deltaY = loc.getY() - definedLoc.getY();
            double deltaZ = loc.getZ() - definedLoc.getZ();

            if (!loc.getWorld().getName().equals(definedLoc.getWorld().getName()) || Math.abs(deltaY) > 2 || deltaX * deltaX + deltaZ * deltaZ > 0.1) {
                spawnedEntity.teleport(definedLoc);
            }

            return SanityCheckResult.CHECKED;
        } else {
            return SanityCheckResult.SKIPPED;
        }
    }
}
