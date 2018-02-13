package cat.nyaa.npc;

import cat.nyaa.npc.persistance.NpcDataConfig;
import cat.nyaa.npc.persistance.TradeDataConfig;
import cat.nyaa.nyaacore.configuration.PluginConfigure;
import org.bukkit.plugin.java.JavaPlugin;

public class Configuration extends PluginConfigure {
    private final NyaaPlayerCoser plugin;

    public Configuration(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
        this.npcData = new NpcDataConfig(plugin);
        this.tradeData = new TradeDataConfig(plugin);
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    // data fields

    @Serializable
    public String language = "en_US";
    @StandaloneConfig
    public NpcDataConfig npcData;
    @StandaloneConfig
    public TradeDataConfig tradeData;
}
