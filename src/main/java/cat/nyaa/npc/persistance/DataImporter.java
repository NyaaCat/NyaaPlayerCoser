package cat.nyaa.npc.persistance;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.Message;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class DataImporter {
    public static final void importShopkeeper(NyaaPlayerCoser plugin, CommandSender sender) {
        YamlConfiguration badCfg = new YamlConfiguration();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "save.yml"));
        int imported = 0;
        int skipped = 0;

        for (String npcIndex : cfg.getKeys(false)) {
            ConfigurationSection npcSection = cfg.getConfigurationSection(npcIndex);
            try {
                importShopkeeperNPC(plugin, npcSection);
                imported ++;
            } catch (Exception ex) {
                skipped ++;
                badCfg.set(npcIndex, npcSection);
                sender.sendMessage("Failed to import No."+npcIndex+": " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        plugin.cfg.tradeData.save();
        plugin.cfg.npcData.save();

        try {
            badCfg.save(new File(plugin.getDataFolder(), "bad-save.yml"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        sender.sendMessage("Incompatible sections saved to bad-save.yml");
        plugin.entitiesManager.forceRespawnAllNpc();
        sender.sendMessage("Imported: " + imported);
        sender.sendMessage("Skipped: " + skipped);
    }

    private static int skRecipeIndex = 0;
    private static final String importShopkeeperRecipe(NyaaPlayerCoser plugin, ConfigurationSection sec) {
        String idxName = "sk-" + skRecipeIndex;
        while(plugin.cfg.tradeData.tradeList.containsKey(idxName)) {
            skRecipeIndex++;
            idxName = "sk-" + skRecipeIndex;
        }

        ItemStack item1 = sec.getItemStack("item1", new ItemStack(Material.AIR));
        ItemStack item2 = sec.getItemStack("item2", new ItemStack(Material.AIR));
        ItemStack result = sec.getItemStack("resultItem", new ItemStack(Material.AIR));
        TradeData td = new TradeData(item1, item2, result);
        plugin.cfg.tradeData.tradeList.put(idxName, td);
        skRecipeIndex ++;
        return idxName;
    }

    private static int skNpcIndex = 0;
    public static final UUID DEFAULT_OWNER_ID_SHOPKEEPER = UUID.nameUUIDFromBytes("SHOPKEEPER_DEFAULT".getBytes(Charset.forName("UTF-8")));
    private static final String importShopkeeperNPC(NyaaPlayerCoser plugin, ConfigurationSection sec) {
        if (!"admin".equals(sec.getString("type",""))) {
            throw new RuntimeException("type " + sec.getString("type", "[empt]") + " not supported");
        }

        String idxName = "sk-" + skNpcIndex;
        while(plugin.cfg.npcData.npcList.containsKey(idxName)) {
            skNpcIndex++;
            idxName = "sk-" + skNpcIndex;
        }

        NpcData npc = new NpcData();
        if (sec.contains("owner uuid")) {
            npc.ownerId = UUID.fromString(sec.getString("owner uuid"));
        } else {
            npc.ownerId = DEFAULT_OWNER_ID_SHOPKEEPER;
        }
        npc.worldName = sec.getString("world");
        if (!"world".equals(npc.worldName)) throw new RuntimeException("npc not in main world: " + npc.worldName);
        npc.x = sec.getInt("x")+0.5;
        npc.y = sec.getInt("y")+0.0;
        npc.z = sec.getInt("z")+0.5;
        npc.displayName = sec.getString("name", "");
        npc.nbtTag = ""; // TODO
        npc.type = EntityType.fromName(sec.getString("object"));
        if (npc.type == null) throw new RuntimeException("Unknown entity type: " + sec.getString("object"));
        npc.enabled = true;
        npc.npcType = NpcType.TRADER_UNLIMITED;
        npc.trades = new ArrayList<>();

        if (sec.contains("recipes")) {
            for (String recIndex : sec.getConfigurationSection("recipes").getKeys(false)) {
                npc.trades.add(importShopkeeperRecipe(plugin, sec.getConfigurationSection("recipes."+recIndex)));
            }
        }

        plugin.cfg.npcData.npcList.put(idxName, npc);
        skNpcIndex ++;
        return idxName;
    }

}
