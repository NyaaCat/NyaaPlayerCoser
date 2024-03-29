package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.ExternalPluginUtils;
import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.SkinData;
import cat.nyaa.nyaacore.utils.VersionUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import net.minecraft.world.entity.projectile.EntityEgg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NPCPlayer extends NPCBase {
    private static final SecureRandom rnd = new SecureRandom();
    public static final Map<Integer, NPCPlayer> spawnedDummyNPCs = new HashMap<>(); // map<entityId, NpcPlayer>

    public static UUID getVersion2UUID() {
        return getVersion2UUID(null);
    }

    public static UUID getVersion2UUID(String str) {
        byte[] md5Bytes = null;
        if (str != null) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
                throw new InternalError("MD5 not supported", nsae);
            }
            md5Bytes = md.digest(str.getBytes());
        } else {
            md5Bytes = new byte[16];
            rnd.nextBytes(md5Bytes);
        }
        md5Bytes[6] &= 0x0f;  /* clear version        */
        md5Bytes[6] |= 0x20;  /* set to version 2     */
        md5Bytes[8] &= 0x3f;  /* clear variant        */
        md5Bytes[8] |= 0x80;  /* set to IETF variant  */

        byte[] data = md5Bytes;
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (data[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (data[i] & 0xff);

        return new UUID(msb, lsb);
    }

    WrappedGameProfile profile;
    WrappedDataWatcher dataWatcher;

    private Location loc;
    private boolean spawned = false;
    private Integer entityId = null;
    private Set<Player> inRangePlayers = new HashSet<>();
    private byte pitch = 0;
    private byte yaw = 0;

    @Override
    public Integer getEntityId() {
        return this.entityId;
    }

    public NPCPlayer(String id, NpcData data) {
        super(id, data);
        if (data.entityType != EntityType.PLAYER) throw new IllegalArgumentException("not a player npc");

        if (data.displayName.length() > 16) data.displayName = data.displayName.substring(0, 16); // guard

        profile = new WrappedGameProfile(getVersion2UUID(), data.displayName);
        SkinData skin = NyaaPlayerCoser.instance.cfg.skinData.getSkinData(data.playerSkin);
        profile.getProperties().put("textures", new WrappedSignedProperty("textures", skin.texture_value, skin.texture_signature));

        dataWatcher = new WrappedDataWatcher();
        // https://wiki.vg/Entity_metadata#Entity
        // https://github.com/dmulloy2/ProtocolLib/issues/160#issuecomment-192983554
        //dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(10, WrappedDataWatcher.Registry.get(Integer.class)), 3);
        if (VersionUtils.isVersionGreaterOrEq(VersionUtils.getCurrentVersion(), "1.17")) {
            dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(17, WrappedDataWatcher.Registry.get(Byte.class)), (byte) skin.displayMask, true);
        } else if (VersionUtils.isVersionGreaterOrEq(VersionUtils.getCurrentVersion(), "1.16")) {
            dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(16, WrappedDataWatcher.Registry.get(Byte.class)), (byte) skin.displayMask, true);
        }

        World w = Bukkit.getWorld(data.worldName);
        if (w == null) throw new IllegalArgumentException();
        loc = new Location(w, data.x, data.y, data.z);
    }

    @Override
    public void onPlayerEnterRange(Player p) {
        if (spawned && !inRangePlayers.contains(p)) {
            inRangePlayers.add(p);
            try {
                PlayerInfoData playerInfoData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.CREATIVE, WrappedChatComponent.fromText(data.displayName));
                PacketContainer pktList = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
                pktList.getEnumModifier(EnumWrappers.PlayerInfoAction.class, 0).write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                List<PlayerInfoData> l = Stream.of(playerInfoData).collect(Collectors.toList());
                pktList.getPlayerInfoDataLists().write(0, l);
                ExternalPluginUtils.getPM().sendServerPacket(p, pktList);

                PacketContainer pktSpawn = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
                pktSpawn.getIntegers().write(0, entityId);
                pktSpawn.getUUIDs().write(0, playerInfoData.getProfile().getUUID());
                pktSpawn.getDoubles().write(0, loc.getX());
                pktSpawn.getDoubles().write(1, loc.getY());
                pktSpawn.getDoubles().write(2, loc.getZ());
                pktSpawn.getBytes().write(0, yaw); // yaw
                pktSpawn.getBytes().write(1, pitch); // pitch
//                 pktSpawn.getDataWatcherModifier().write(0, dataWatcher);
                ExternalPluginUtils.getPM().sendServerPacket(p, pktSpawn);

                PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
                List<WrappedWatchableObject> watchableObjects = dataWatcher.getWatchableObjects();
                packetContainer.getIntegers().write(0, entityId);
                packetContainer.getWatchableCollectionModifier().write(0, watchableObjects);
                ExternalPluginUtils.getPM().sendServerPacket(p, packetContainer);

                Bukkit.getScheduler().runTaskLater(NyaaPlayerCoser.instance, new Runnable() {
                    @Override
                    public void run() { // when client about to spawn the player, it still need the gameprofile from the list. so we cannot remove it early.
                        pktList.getEnumModifier(EnumWrappers.PlayerInfoAction.class, 0).write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
                        try {
                            ExternalPluginUtils.getPM().sendServerPacket(p, pktList);
                        } catch (InvocationTargetException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }, NyaaPlayerCoser.instance.cfg.tabListDelay);
            } catch (ReflectiveOperationException ex) {
                p.sendMessage("npc spawn fail. please report the bug");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onPlayerLeaveRange(Player p) {
        if (spawned && inRangePlayers.contains(p)) {
            inRangePlayers.remove(p);
            try {
                PacketContainer pktRemoveEntity = getRemoveEntityPacket(entityId);
                ExternalPluginUtils.getPM().sendServerPacket(p, pktRemoveEntity);
            } catch (InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onEntityRemove(Entity e) {

    }

    @Override
    public boolean despawn(Entity e) {
        if (e != null) Bukkit.getLogger().warning(String.format("NPCPlayer::despawn() received none null value %s. This should not happen.", e));
        try {
            if (!spawned) return true;
            if (!inRangePlayers.isEmpty()) {

                PacketContainer pktRemoveEntity = getRemoveEntityPacket(entityId);

                for (Player p : inRangePlayers) {
                    ExternalPluginUtils.getPM().sendServerPacket(p, pktRemoveEntity);
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        } finally {
            spawnedDummyNPCs.remove(entityId);
            entityId = null;
            inRangePlayers.clear();
            spawned = false;
        }
        return true;
    }

    private static int nextEntityId() {
        EntityEgg eg = new EntityEgg(null, 0, 0, 0);
        return eg.getId();
    }

    @Override
    public void spawn() {
        spawned = true;
        entityId = nextEntityId();
        spawnedDummyNPCs.put(entityId, this);
    }

    @Override
    public void setPitchYaw(Float pitch, Float yaw) {
        if (pitch != null) this.pitch = (byte) (pitch / 360.0 * 256);
        if (yaw != null) this.yaw = (byte) (yaw / 360.0 * 256);
        PacketContainer pktEntityLook = ExternalPluginUtils.getPM()
                .createPacketConstructor(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, int.class, short.class, short.class, short.class, byte.class, byte.class, boolean.class)
                .createPacket(entityId, (short)0, (short)0, (short)0, this.yaw, this.pitch, true);
        PacketContainer pktEntityHeadRotation = ExternalPluginUtils.getPM().createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        pktEntityHeadRotation.getIntegers().write(0, entityId);
        pktEntityHeadRotation.getBytes().write(0, this.yaw);

        for (Player p : inRangePlayers) {
            try {
                ExternalPluginUtils.getPM().sendServerPacket(p, pktEntityLook);
                ExternalPluginUtils.getPM().sendServerPacket(p, pktEntityHeadRotation);
            } catch (InvocationTargetException ex) {
                p.sendMessage("NPC cannot update direction. Please report this bug.");
                ex.printStackTrace();
            }
        }
    }
    private PacketContainer getRemoveEntityPacket(int EntityId){
        // version < 1.17 int[],1.17:int,1.17.1:List<int>
        PacketContainer pktRemoveEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        if(!VersionUtils.isVersionGreaterOrEq(Bukkit.getBukkitVersion(),"1.17")){
            pktRemoveEntity.getIntegerArrays().write(0, new int[]{EntityId}); // <1.17
        }else if(VersionUtils.isVersionGreaterOrEq(Bukkit.getBukkitVersion(),"1.17.1")){
            pktRemoveEntity.getIntLists().write(0, Collections.singletonList(EntityId)); //>=1.17.1
        }else{
            pktRemoveEntity.getIntegers().write(0, EntityId);//=1.17
        }
        return pktRemoveEntity;
    }
    @Override
    public Location getEyeLocation() {
        if (!spawned || inRangePlayers.isEmpty()) return null;
        return loc.clone().add(0, 1.62, 0);
    }

    @Override
    public Entity getUnderlyingSpawnedEntity() {
        return null;
    }

    @Override
    public SanityCheckResult doSanityCheck() {
        return SanityCheckResult.SKIPPED;
    }
}
