package cat.nyaa.npc.persistence;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.utils.MathUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class NpcTravelPlan implements ISerializable, Cloneable {
    public static final NpcTravelPlan DO_NOT_TRAVEL = new NpcTravelPlan();

    @Serializable
    public boolean isTraveller = false;
    @Serializable
    public int presentTimeMinSecond = 3600;
    @Serializable
    public int presentTimeMaxSecond = 36000;
    @Serializable
    public int absentTimeMinSecond = 3600;
    @Serializable
    public int absentTimeMaxSecond = 36000;
    @Serializable
    public int availableTradeMin = 1;
    @Serializable
    public int availableTradeMax = 4;
    @Serializable
    public List<String> completeTradeIdList = new ArrayList<>();
    @Serializable
    public int centralX = 0;
    @Serializable
    public int centralY = 64;
    @Serializable
    public int centralZ = 0;
    @Serializable
    public int randomXZmax = 10;
    @Serializable
    public int randomYPositiveMax = 10;
    @Serializable
    public int randomYNegativeMax = 5;
    @Serializable
    public int randomTryMax = 16;
    @Serializable
    public long nextMovementTime = -1;
    @Serializable
    public boolean isPresent = false;

    @Override
    public void deserialize(ConfigurationSection config) {
        if (config.getBoolean("isTraveller", false)) {
            ISerializable.deserialize(config, this);
        } else {
            isTraveller = false;
        }
    }

    @Override
    public void serialize(ConfigurationSection config) {
        if (isTraveller) {
            ISerializable.serialize(config, this);
        } else {
            config.set("isTraveller", false);
        }
    }

    @Override
    public NpcTravelPlan clone() {
        try {
            return (NpcTravelPlan) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }

    }

    public boolean isTimeToMove() {
        return isTraveller && System.currentTimeMillis() >= nextMovementTime;
    }

    public void doMove(NpcData data) {
        if (!isTraveller) return;
        long now = System.currentTimeMillis();
        while (now > nextMovementTime) {
            if (nextMovementTime < 0) {
                isPresent = true;
                nextMovementTime = now + MathUtils.uniformRangeInclusive(
                        presentTimeMinSecond * MathUtils.MILLI_IN_SEC,
                        presentTimeMaxSecond * MathUtils.MILLI_IN_SEC);
            } else {
                isPresent = !isPresent;
                if (isPresent) {
                    nextMovementTime += MathUtils.uniformRangeInclusive(
                            presentTimeMinSecond * MathUtils.MILLI_IN_SEC,
                            presentTimeMaxSecond * MathUtils.MILLI_IN_SEC);
                } else {
                    nextMovementTime += MathUtils.uniformRangeInclusive(
                            absentTimeMinSecond * MathUtils.MILLI_IN_SEC,
                            absentTimeMaxSecond * MathUtils.MILLI_IN_SEC);
                }
            }
        }

        if (isPresent) {
            data.trades = new ArrayList<>(
                    MathUtils.randomSelect(
                            completeTradeIdList,
                            MathUtils.uniformRangeInclusive(availableTradeMin, availableTradeMax)));
            World w = Bukkit.getWorld(data.worldName);
            if (w != null) {
                Location l = getRandomizedLegLocation(w);
                data.x = l.getBlockX() + 0.5D;
                data.y = (double) l.getBlockY();
                data.z = l.getBlockZ() + 0.5D;
            } else {
                data.x = centralX + 0.5D;
                data.y = (double) centralY;
                data.z = centralZ + 0.5D;
            }
            broadcastArrival(w, data);
            Vector locationVector = data.getLocationVector();
            showArriveEffect(w, locationVector);

        }else {
            World w = Bukkit.getWorld(data.worldName);
            broadcastDeparture(w, data);
            Vector locationVector = data.getLocationVector();
            showDepartureEffect(w, locationVector);
        }
    }

    private void showDepartureEffect(World w, Vector locationVector) {
        Location location = new Location(w, locationVector.getX(), locationVector.getY(), locationVector.getZ());
        if (w.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            w.spawnParticle(Particle.PORTAL, location, 300);
            w.playSound(location, Sound.ENTITY_VILLAGER_CELEBRATE, 20, 1);
        }
    }

    private void showArriveEffect(World w, Vector locationVector) {
        Location location = new Location(w, locationVector.getX(), locationVector.getY(), locationVector.getZ());
        if (w.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            w.spawnParticle(Particle.CLOUD, location, 300, 0, 0, 0, 0.1);
            w.playSound(location, Sound.ENTITY_VILLAGER_YES, 20, 1);
        }
    }

    private void broadcastDeparture(World w, NpcData data) {
        double broadcastRange = NyaaPlayerCoser.instance.cfg.broadcastRange;
        String departureMessage = NyaaPlayerCoser.instance.cfg.departMessage;
        departureMessage = departureMessage.replaceAll("\\{merchant\\.name}", data.displayName);
        broadcast(w, departureMessage, data.getLocationVector(), broadcastRange);
    }

    private void broadcastArrival(World w, NpcData data) {
        double broadcastRange = NyaaPlayerCoser.instance.cfg.broadcastRange;
        String arrivalMessage = NyaaPlayerCoser.instance.cfg.arrivalMessage;
        arrivalMessage = arrivalMessage.replaceAll("\\{merchant\\.name}", data.displayName);
        broadcast(w, arrivalMessage, data.getLocationVector(), broadcastRange);
    }

    private void broadcast(World w, String message, Vector location, double broadcastRange) {
        Message msg = new Message(ChatColor.translateAlternateColorCodes('&', message));
        w.getPlayers().stream()
                .filter(player -> player.getLocation().toVector().distance(location) < broadcastRange)
                .forEach(msg::send);
    }

    private Location getRandomizedLegLocation(World w) {
        for (int _try = 0; _try < randomTryMax; _try++) {
            int x = MathUtils.uniformRangeInclusive(centralX - randomXZmax, centralX + randomXZmax);
            int z = MathUtils.uniformRangeInclusive(centralZ - randomXZmax, centralZ + randomXZmax);
            int stat = 0;
            for (int y = Math.min(255, centralY + randomYPositiveMax);
                 y > Math.max(0, centralY - randomYNegativeMax); y--) {
                boolean isPassable = w.getBlockAt(x, y, z).isPassable();
                switch (stat) {
                    case 0:
                        if (isPassable) stat = 1;
                        break;
                    case 1:
                        if (isPassable) stat = 2;
                        else stat = 0;
                        break;
                    case 2:
                        if (!isPassable) return new Location(w, x, y + 1, z);
                }
            }
        }
        return new Location(w, centralX, centralY, centralZ);
    }

}
