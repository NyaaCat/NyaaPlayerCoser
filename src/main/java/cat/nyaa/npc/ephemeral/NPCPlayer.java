package cat.nyaa.npc.ephemeral;

import cat.nyaa.npc.ExternalPluginUtils;
import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.persistence.SkinData;
import cat.nyaa.nyaacore.utils.VersionUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NPCPlayer extends NPCBase {
    private static final SecureRandom rnd = new SecureRandom();
    // Use a high starting value to avoid conflicts with real entity IDs
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE / 2);
    public static final Map<Integer, NPCPlayer> spawnedDummyNPCs = new HashMap<>(); // map<entityId, NpcPlayer>
    private static final Integer PLAYER_SKIN_PARTS_INDEX = resolvePlayerSkinPartsIndex();

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
        profile.getProperties().put("textures", createTextureProperty(skin));

        dataWatcher = new WrappedDataWatcher();
        // https://wiki.vg/Entity_metadata#Entity
        // https://github.com/dmulloy2/ProtocolLib/issues/160#issuecomment-192983554
        //dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(10, WrappedDataWatcher.Registry.get(Integer.class)), 3);
        if (VersionUtils.isVersionGreaterOrEq(VersionUtils.getCurrentVersion(), "1.17")) {
            Integer skinPartsIndex = PLAYER_SKIN_PARTS_INDEX;
            if (skinPartsIndex != null) {
                dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(skinPartsIndex, WrappedDataWatcher.Registry.get(Byte.class)), (byte) skin.displayMask, true);
            } else {
                dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(17, WrappedDataWatcher.Registry.get(Byte.class)), (byte) skin.displayMask, true);
            }
        } else if (VersionUtils.isVersionGreaterOrEq(VersionUtils.getCurrentVersion(), "1.16")) {
            dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(16, WrappedDataWatcher.Registry.get(Byte.class)), (byte) skin.displayMask, true);
        }

        loc = resolveLocation();
    }

    @Override
    public void onPlayerEnterRange(Player p) {
        if (spawned && !inRangePlayers.contains(p)) {
            inRangePlayers.add(p);
            try {
                Location currentLoc = resolveLocation();
                if (currentLoc == null) {
                    return;
                }
                PlayerInfoData playerInfoData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.CREATIVE, WrappedChatComponent.fromText(data.displayName));
                PacketContainer pktList = buildPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, playerInfoData);
                ExternalPluginUtils.getPM().sendServerPacket(p, pktList);

                PacketContainer pktSpawn = buildPlayerSpawnPacket(currentLoc, entityId, playerInfoData.getProfile().getUUID(), yaw, pitch);
                ExternalPluginUtils.getPM().sendServerPacket(p, pktSpawn);

                PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
                packetContainer.getIntegers().write(0, entityId);
                writeEntityMetadata(packetContainer, dataWatcher);
                ExternalPluginUtils.getPM().sendServerPacket(p, packetContainer);

                Bukkit.getScheduler().runTaskLater(NyaaPlayerCoser.instance, new Runnable() {
                    @Override
                    public void run() { // when client about to spawn the player, it still need the gameprofile from the list. so we cannot remove it early.
                        PacketContainer pktRemove = buildPlayerInfoRemovePacket(playerInfoData.getProfile().getUUID(), playerInfoData);
                        ExternalPluginUtils.getPM().sendServerPacket(p, pktRemove);
                    }
                }, NyaaPlayerCoser.instance.cfg.tabListDelay);
            } catch (Exception ex) {
                p.sendMessage("npc spawn fail. please report the bug");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onPlayerLeaveRange(Player p) {
        if (spawned && inRangePlayers.contains(p)) {
            inRangePlayers.remove(p);
            PacketContainer pktRemoveEntity = getRemoveEntityPacket(entityId);
            ExternalPluginUtils.getPM().sendServerPacket(p, pktRemoveEntity);
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
        } finally {
            spawnedDummyNPCs.remove(entityId);
            entityId = null;
            inRangePlayers.clear();
            spawned = false;
        }
        return true;
    }

    private static int nextEntityId() {
        return ENTITY_ID_COUNTER.getAndIncrement();
    }

    private static PacketContainer buildPlayerInfoPacket(EnumWrappers.PlayerInfoAction action, PlayerInfoData playerInfoData) {
        PacketContainer packet = new PacketContainer(getPlayerInfoPacketType());
        if (!writePlayerInfoActions(packet, action)) {
            packet.getPlayerInfoAction().write(0, action);
        }
        writePlayerInfoDataList(packet, Collections.singletonList(playerInfoData));
        return packet;
    }

    private static PacketContainer buildPlayerInfoRemovePacket(UUID uuid, PlayerInfoData playerInfoData) {
        if (PacketType.Play.Server.PLAYER_INFO_REMOVE.isSupported()) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            packet.getUUIDLists().write(0, Collections.singletonList(uuid));
            return packet;
        }
        return buildPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, playerInfoData);
    }

    private static PacketType getPlayerInfoPacketType() {
        try {
            var field = PacketType.Play.Server.class.getField("PLAYER_INFO_UPDATE");
            PacketType packetType = (PacketType) field.get(null);
            if (packetType != null && packetType.isSupported()) {
                return packetType;
            }
        } catch (ReflectiveOperationException ignored) {
            // fall through to legacy packet type
        }
        return PacketType.Play.Server.PLAYER_INFO;
    }

    private static PacketContainer buildPlayerSpawnPacket(Location location, int entityId, UUID uuid, byte yaw, byte pitch) {
        PacketType packetType = resolvePlayerSpawnPacketType();
        PacketContainer packet;
        try {
            packet = new PacketContainer(packetType);
        } catch (IllegalArgumentException ex) {
            packetType = PacketType.Play.Server.SPAWN_ENTITY;
            packet = new PacketContainer(packetType);
        }
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        if (packetType != PacketType.Play.Server.SPAWN_ENTITY) {
            packet.getBytes().write(0, yaw);
            packet.getBytes().write(1, pitch);
        } else {
            packet.getEntityTypeModifier().write(0, EntityType.PLAYER);
            writeSpawnRotation(packet, yaw, pitch);
        }
        return packet;
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
            ExternalPluginUtils.getPM().sendServerPacket(p, pktEntityLook);
            ExternalPluginUtils.getPM().sendServerPacket(p, pktEntityHeadRotation);
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
        Location currentLoc = resolveLocation();
        if (currentLoc == null) return null;
        return currentLoc.clone().add(0, 1.62, 0);
    }

    @Override
    public Entity getUnderlyingSpawnedEntity() {
        return null;
    }

    @Override
    public SanityCheckResult doSanityCheck() {
        return SanityCheckResult.SKIPPED;
    }

    private static boolean writePlayerInfoActions(PacketContainer packet, EnumWrappers.PlayerInfoAction action) {
        try {
            var modifier = packet.getModifier().withType(EnumSet.class);
            if (modifier.size() > 0) {
                modifier.write(0, buildGenericActionSet(action));
                return true;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return false;
    }

    private static void writePlayerInfoDataList(PacketContainer packet, List<PlayerInfoData> playerInfoDataList) {
        try {
            var listModifier = packet.getModifier().withType(List.class,
                    BukkitConverters.getListConverter(PlayerInfoData.getConverter()));
            if (listModifier.size() > 0) {
                listModifier.write(0, playerInfoDataList);
                return;
            }
        } catch (Exception ignored) {
            // fall back to legacy accessor
        }
        packet.getPlayerInfoDataLists().write(0, playerInfoDataList);
    }

    private static PacketType resolvePlayerSpawnPacketType() {
        List<String> candidates = Arrays.asList(
                "PLAYER_SPAWN",
                "SPAWN_PLAYER",
                "NAMED_ENTITY_SPAWN"
        );
        for (String fieldName : candidates) {
            try {
                var field = PacketType.Play.Server.class.getField(fieldName);
                PacketType packetType = (PacketType) field.get(null);
                if (packetType != null && packetType.isSupported()) {
                    return packetType;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next candidate
            }
        }
        return PacketType.Play.Server.SPAWN_ENTITY;
    }

    private static void writeSpawnRotation(PacketContainer packet, byte yaw, byte pitch) {
        try {
            if (packet.getBytes().size() >= 2) {
                packet.getBytes().write(0, yaw);
                packet.getBytes().write(1, pitch);
                if (packet.getBytes().size() > 2) {
                    packet.getBytes().write(2, yaw);
                }
                return;
            }
        } catch (Exception ignored) {
            // fall through to integer rotation
        }
        try {
            if (packet.getIntegers().size() >= 6) {
                packet.getIntegers().write(4, (int) (pitch * 256.0F / 360.0F));
                packet.getIntegers().write(5, (int) (yaw * 256.0F / 360.0F));
            }
        } catch (Exception ignored) {
            // ignore if structure differs
        }
    }

    private static void writeEntityMetadata(PacketContainer packet, WrappedDataWatcher watcher) {
        try {
            if (packet.getDataValueCollectionModifier().size() > 0) {
                packet.getDataValueCollectionModifier().write(0, watcher.toDataValueCollection());
                return;
            }
        } catch (Exception ignored) {
            // fall back to legacy watchable objects
        }
        try {
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        } catch (Exception ignored) {
            // ignore if structure differs
        }
    }

    private static Integer resolvePlayerSkinPartsIndex() {
        try {
            Class<?> avatarClass = Class.forName("net.minecraft.world.entity.Avatar");
            Integer result = readDataAccessorId(avatarClass, "DATA_PLAYER_MODE_CUSTOMISATION", "DATA_PLAYER_MODE_CUSTOMIZATION");
            if (result != null) {
                return result;
            }
        } catch (ClassNotFoundException ignored) {
            // fall back to version-based indices
        }
        return null;
    }

    private static Integer readDataAccessorId(Class<?> holderClass, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = holderClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object accessor = field.get(null);
                Method idMethod = accessor.getClass().getMethod("id");
                Object value = idMethod.invoke(accessor);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next candidate
            }
        }
        return null;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EnumSet buildGenericActionSet(EnumWrappers.PlayerInfoAction action) {
        Class<?> actionClass = EnumWrappers.getPlayerInfoActionClass();
        if (actionClass == null) {
            return EnumSet.of(action);
        }
        EnumSet set = EnumSet.noneOf((Class) actionClass);
        try {
            Object generic = EnumWrappers.getPlayerInfoActionConverter().getGeneric(action);
            if (generic instanceof Enum) {
                set.add((Enum) generic);
            }
        } catch (Exception ignored) {
            // leave empty; caller will handle failures
        }
        return set;
    }

    private Location resolveLocation() {
        World w = Bukkit.getWorld(data.worldName);
        if (w == null) return null;
        if (loc == null || loc.getWorld() != w) {
            loc = new Location(w, data.x, data.y, data.z);
        } else {
            loc.setX(data.x);
            loc.setY(data.y);
            loc.setZ(data.z);
        }
        return loc;
    }

    private WrappedSignedProperty createTextureProperty(SkinData skin) {
        SkinData resolved = skin;
        if (resolved == null || resolved.texture_value == null || resolved.texture_signature == null
                || resolved.texture_value.isEmpty() || resolved.texture_signature.isEmpty()) {
            resolved = NyaaPlayerCoser.instance.cfg.skinData.getSkinData("default");
        }
        try {
            return new WrappedSignedProperty("textures", resolved.texture_value, resolved.texture_signature);
        } catch (Exception ex) {
            SkinData fallback = NyaaPlayerCoser.instance.cfg.skinData.getSkinData("default");
            return new WrappedSignedProperty("textures", fallback.texture_value, fallback.texture_signature);
        }
    }
}
