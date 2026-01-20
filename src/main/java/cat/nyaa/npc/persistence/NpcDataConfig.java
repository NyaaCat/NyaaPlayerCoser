package cat.nyaa.npc.persistence;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NpcDataConfig extends FileConfigure {
    @Override
    protected String getFileName() {
        return "npcs.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public void load() {
        super.load();
        // Filter out deprecated HEH_SELL_SHOP NPCs (player-created NPCs)
        // HamsterEcoHelper integration is no longer supported
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, NpcData> entry : npcList.entrySet()) {
            if (entry.getValue().npcType == NpcType.HEH_SELL_SHOP) {
                toRemove.add(entry.getKey());
                plugin.getLogger().info("Removing deprecated HEH_SELL_SHOP NPC: " + entry.getKey() + " (" + entry.getValue().displayName + ")");
            }
        }
        if (!toRemove.isEmpty()) {
            for (String id : toRemove) {
                npcList.remove(id);
            }
            plugin.getLogger().info("Removed " + toRemove.size() + " deprecated player-created NPC(s). Saving config...");
            save();
        }
    }

    private final NyaaPlayerCoser plugin;

    public NpcDataConfig(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
    }

    @Serializable
    public Map<String, NpcData> npcList = new HashMap<>();
    @Serializable
    public int maxId = 0;

    /**
     * Add an new npc then save immediately.
     * You may want to use {@link cat.nyaa.npc.EntitiesManager#createNpcDefinition(NpcData)}
     *
     * @param data npc data
     * @return newly assigned npc id.
     */
    public String addNpc(NpcData data) {
        while (npcList.containsKey(Integer.toString(maxId))) {
            maxId++;
        }
        npcList.put(Integer.toString(maxId), data);
        save();
        return Integer.toString(maxId++);
    }

    /**
     * Replace current NPC definition.
     * You may want to use {@link cat.nyaa.npc.EntitiesManager#replaceNpcDefinition(String, NpcData)}
     */
    public NpcData replaceNpc(String npcId, NpcData data) {
        if (!npcList.containsKey(npcId)) throw new IllegalArgumentException();
        NpcData oldData = npcList.get(npcId);
        npcList.put(npcId, data);
        save();
        return oldData;
    }

    public NpcData removeNpc(String npcId) {
        if (!npcList.containsKey(npcId)) throw new IllegalArgumentException();
        NpcData oldData = npcList.get(npcId);
        npcList.remove(npcId);
        save();
        return oldData;
    }

    /**
     * Return list of NPCs in the given chunk.
     * TODO optimize
     */
    public Map<String, NpcData> getNpcInChunk(String world, int chunkX, int chunkZ) {
        Map<String, NpcData> ret = new HashMap<>();
        for (Map.Entry<String, NpcData> e : npcList.entrySet()) {
            if (e.getValue().chunkX() == chunkX && e.getValue().chunkZ() == chunkZ && e.getValue().worldName.equalsIgnoreCase(world)) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }

    public Map<String, NpcData> getNpcByOwnerId(UUID ownerId) {
        if (ownerId == null) throw new IllegalArgumentException();
        Map<String, NpcData> ret = new HashMap<>();
        for (Map.Entry<String, NpcData> e : npcList.entrySet()) {
            if (ownerId.equals(e.getValue().ownerId)) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }

    /**
     * Return list of NPCs that has the specified trade.
     * TODO optimize
     */
    public Map<String, NpcData> getNpcByTradeId(String tradeId) {
        if (tradeId == null) throw new IllegalArgumentException();
        Map<String, NpcData> ret = new HashMap<>();
        for (Map.Entry<String, NpcData> e : npcList.entrySet()) {
            if (e.getValue().trades.contains(tradeId)) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }
}
