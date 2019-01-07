package cat.nyaa.npc.persistence;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class TradeDataConfig extends FileConfigure {
    @Override
    protected String getFileName() {
        return "trades.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    private final NyaaPlayerCoser plugin;

    public TradeDataConfig(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
    }

    @Serializable
    public Map<String, TradeData> tradeList = new HashMap<>();
    @Serializable
    public int maxId = 0;

    /**
     * Add an new trade then save immediately.
     *
     * @param data trade data
     * @return newly assigned trade id.
     */
    public String addTrade(TradeData data) {
        while (tradeList.containsKey(Integer.toString(maxId))) {
            maxId++;
        }
        tradeList.put(Integer.toString(maxId), data);
        save();
        return Integer.toString(maxId++);
    }
}
