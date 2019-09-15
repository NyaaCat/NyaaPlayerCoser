package cat.nyaa.npc.persistence;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
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
