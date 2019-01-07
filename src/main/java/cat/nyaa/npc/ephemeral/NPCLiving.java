package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.nyaacore.utils.NmsUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NPCLiving extends NPCBase {
    private LivingEntity spawnedEntity = null;

    public NPCLiving(String id, NpcData data) {
        super(id, data);
    }

    @Override
    public void onPlayerEnterRange(Player p) {
    }

    @Override
    public void onPlayerLeaveRange(Player p) {
    }

    @Override
    public void onEntityRemove(Entity e) {
        if (spawnedEntity == e) {
            spawnedEntity = null;
        }
    }

    @Override
    public void despawn() {
        if (spawnedEntity != null) {
            spawnedEntity.remove();
            spawnedEntity = null;
        }
    }

    @Override
    public void spawn() {
        // check spawning conditions
        if (data == null) throw new RuntimeException("missing npc data");
        World w = Bukkit.getWorld(data.worldName);
        if (w == null) throw new RuntimeException("invalid world");
        Location loc = new Location(w, data.x, data.y, data.z);

        // spawn
        LivingEntity e = (LivingEntity) w.spawnEntity(loc, data.type);
        if (data.nbtTag != null && data.nbtTag.length() > 0) {
            NmsUtils.setEntityTag(e, data.nbtTag);
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
    }

    @Override
    public void setPitchYaw(Float pitch, Float yaw) {
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
}
