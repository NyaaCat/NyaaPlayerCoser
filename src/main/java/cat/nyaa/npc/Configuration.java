package cat.nyaa.npc;

import cat.nyaa.npc.persistence.NpcDataConfig;
import cat.nyaa.npc.persistence.SkinDataConfig;
import cat.nyaa.npc.persistence.TradeDataConfig;
import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Configuration extends PluginConfigure {
    private final NyaaPlayerCoser plugin;

    public Configuration(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
        this.npcData = new NpcDataConfig(plugin);
        this.tradeData = new TradeDataConfig(plugin);
        this.skinData = new SkinDataConfig(plugin);

        for (EntityType t : EntityType.values()) {
            if (t.isSpawnable() && t.isAlive()) {
                allowedEntityType.add(t.name());
            }
        }
        allowedEntityType.add(EntityType.PLAYER.name());
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    // data fields

    @Serializable
    public String language = "en_US";
    @Serializable
    public List<String> allowedEntityType = new ArrayList<>();
    @Serializable
    public int tabListDelay = 15;
    @Serializable
    public int playerNpcLimit = 6;
    @Serializable(name = "travel_merchant.distance_check")
    public int travelMerchantDistanceCheck = 50;
    @Serializable(name = "travel_merchant.broadcast_range")
    public double broadcastRange = 50;
    @Serializable(name = "travel_merchant.message_arrival")
    public String arrivalMessage = "&aTraveling merchant &r{merchant.name}&a has arrived.";
    @Serializable(name = "travel_merchant.message_depart")
    public String departMessage = "&aTraveling merchant &r{merchant.name}&a has departed.";

    @StandaloneConfig
    public NpcDataConfig npcData;
    @StandaloneConfig
    public TradeDataConfig tradeData;
    @StandaloneConfig
    public SkinDataConfig skinData;

    // helper functions

    public boolean isAllowedType(EntityType type) {
        return allowedEntityType.contains(type.name());
    }
}
