package cat.nyaa.npc;

import cat.nyaa.npc.persistence.NpcDataConfig;
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
    @StandaloneConfig
    public NpcDataConfig npcData;
    @StandaloneConfig
    public TradeDataConfig tradeData;

    // helper functions

    public boolean isAllowedType(EntityType type) {
        return allowedEntityType.contains(type.name());
    }
}
