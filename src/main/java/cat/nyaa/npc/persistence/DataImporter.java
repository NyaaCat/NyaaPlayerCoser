package cat.nyaa.npc.persistence;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.npctype.NpcTypes;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;
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
                imported++;
            } catch (Exception ex) {
                skipped++;
                badCfg.set(npcIndex, npcSection);
                sender.sendMessage("Failed to import No." + npcIndex + ": " + ex.getMessage());
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
        while (plugin.cfg.tradeData.tradeList.containsKey(idxName)) {
            skRecipeIndex++;
            idxName = "sk-" + skRecipeIndex;
        }

        if (sec.contains("resultItem")) {
            sec.set("result", sec.get("resultItem")); // rename resultItem to result
        }
        TradeData td = new TradeData();
        td.deserialize(sec);

        plugin.cfg.tradeData.tradeList.put(idxName, td);
        skRecipeIndex++;
        return idxName;
    }

    private static int skNpcIndex = 0;
    public static final UUID DEFAULT_OWNER_ID_SHOPKEEPER = UUID.nameUUIDFromBytes("SHOPKEEPER_DEFAULT".getBytes(Charset.forName("UTF-8")));

    private static final String importShopkeeperNPC(NyaaPlayerCoser plugin, ConfigurationSection sec) {
        if (!"admin".equals(sec.getString("type", ""))) {
            throw new RuntimeException("type " + sec.getString("type", "[empt]") + " not supported");
        }

        String idxName = "sk-" + skNpcIndex;
        while (plugin.cfg.npcData.npcList.containsKey(idxName)) {
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
        npc.x = sec.getInt("x") + 0.5;
        npc.y = sec.getInt("y") + 0.0;
        npc.z = sec.getInt("z") + 0.5;
        npc.displayName = ChatColor.translateAlternateColorCodes('&', sec.getString("name", ""));
        npc.nbtTag = assembleShopkeeperNbt(sec);
        npc.entityType = EntityType.fromName(sec.getString("object"));
        if (npc.entityType == null) throw new RuntimeException("Unknown entity type: " + sec.getString("object"));
        npc.enabled = true;
        npc.npcType = NpcTypes.TRADER_UNLIMITED.getId();
        npc.trades = new ArrayList<>();

        if (sec.contains("recipes")) {
            for (String recIndex : sec.getConfigurationSection("recipes").getKeys(false)) {
                npc.trades.add(importShopkeeperRecipe(plugin, sec.getConfigurationSection("recipes." + recIndex)));
            }
        }

        plugin.cfg.npcData.npcList.put(idxName, npc);
        skNpcIndex++;
        return idxName;
    }

    private static final Random rnd = new Random();

    /**
     * https://github.com/Shopkeepers/Shopkeepers/tree/master/src/main/java/com/nisovin/shopkeepers/shopobjects/living/types
     *
     * @param sec shopkeeper section
     * @return NBT string for summon command
     */
    private static final String assembleShopkeeperNbt(ConfigurationSection sec) {
        EntityType entityType = EntityType.fromName(sec.getString("object"));
        switch (entityType) {
            case OCELOT: {
                String catTypeString = sec.getString("catType");
                if (catTypeString == null) sec.getString("skeletonType");
                if (catTypeString == null) catTypeString = "WILD_OCELOT";
                try {
                    Ocelot.Type t = Ocelot.Type.valueOf(catTypeString);
                    return String.format("{CatType:%d}", t.getId());
                } catch (IllegalArgumentException ex) {
                    return "{CatType:0}";
                }
            }
            case VILLAGER: {
                try {
                    Villager.Profession prof = Villager.Profession.valueOf(sec.getString("prof", "FARMER"));
                    int profId = prof.ordinal() - 1;
                    if (profId < 0 || profId > 5) profId = 0; // default to FARMER
                    return String.format("{Profession:%d,Career:0}", profId);
                } catch (IllegalArgumentException ex) {
                    return "{Profession:0,Career:0}";
                }
            }
            case PARROT: {
                return String.format("{Variant:%d}", rnd.nextInt(5));
            }
            case SHEEP: {
                int color = sec.getInt("color", 0);
                return String.format("{Color:%d}", color);
            }
            default: {
                return "";
            }
        }
    }

}
