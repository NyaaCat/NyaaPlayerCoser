package cat.nyaa.npc.persistence;

import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class SkinDataConfig extends FileConfigure {
    private static final String DEFAULT_TEXTURE_VALUE = "eyJ0aW1lc3RhbXAiOjE1MDc2NTQzNTY0ODgsInByb2ZpbGVJZCI6IjQzYTgz" +
            "NzNkNjQyOTQ1MTBhOWFhYjMwZjViM2NlYmIzIiwicHJvZmlsZU5hbWUiOiJTa3VsbENsaWVudFNraW42Iiwic2lnbmF0dXJlUmVxdWly" +
            "ZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82NmZl" +
            "NTE3NjY1MTdmM2QwMWNmZGI3MjQyZWI1ZjM0YWVhOTYyOGExNjZlM2U0MGZhZjRjMTMyMTY5NiJ9fX0=";
    private static final String DEFAULT_TEXTURE_SIGNATURE = "av2V34uYK2t2vpVjgffrXKZIFfNKcZFZmfOohJ6FiuOXL66gJHg2EwSR" +
            "KanZG9E21Fc4WkKAcBXJ3VsXpfMeZFSB2BeMm6jzILjJInJv2pZ+5J0CvffN7E/lmwU5JHEPcA0PV3Er5USUGN/wcp8Bgg8IZLOOQNjO" +
            "MgWC8SXmMlORbxZMyp6g5Jhwj6eXba1iRVK4xX9WRB2gQw9TQ375m1RzfJl+ZlhhP9x6IQz/on0hjSOJV/xvA5iZzTOIrOJpwy1kzktr" +
            "5cvOc4V1SxWCLCcCqMHix3ho6y2ilaIfsDIRhnwnFOx5ci0lP+XoVWX4aIHfrdF82aF+uMro72/2zyUMmYtYssfaJ/3/sZi0UpzPzMB1" +
            "7eknht0zF9HaJMIBkW0oief4fpkXZdHa12oyvndMXqtC2kGH3kH1/HoyuxPaCFduirNRcIBWh1xNC2JsOyAos7p5vhHRlq4SSC1U4Y8Z" +
            "1Gm7sttALim9TZwC7hJN56C3IbrkI753QrGObE+X5zyScefO46Mh6stdPRp58inJPaG/xT2wRCLsDligxk37uszb/b/9JxT/zWFOH7Du" +
            "PyKOfFGeexZA7dOzJha8qd1eVD5R5kAsI0Jw0giwU8RYeKCkXVWtXRxoYwKcWSbCNtc/hd7cvnCKfJFTRlk53AcIAGK0kKIbG6ZuRglN" +
            "BbU=";

    @Override
    protected String getFileName() {
        return "skins.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    private final NyaaPlayerCoser plugin;

    public SkinDataConfig(NyaaPlayerCoser plugin) {
        this.plugin = plugin;
    }

    @Serializable(name = "skin_data")
    public Map<String, SkinData> skinDataMap = new HashMap<>();
    @Serializable
    public SkinData defaultSkinData = new SkinData("default", "Default steve skin", DEFAULT_TEXTURE_VALUE, DEFAULT_TEXTURE_SIGNATURE);

    public void addSkinData(String name, String desc, String val, String sig) {
        if (name == null || val == null || sig == null) {
            throw new IllegalArgumentException();
        }
        if (skinDataMap.containsKey(name)) {
            throw new RuntimeException("duplicated key");
        }
        skinDataMap.put(name, new SkinData(name, desc, val, sig));
        save();
    }

    public boolean hasSkinData(String name) {
        if (name == null) return false;
        if ("default".equals(name)) return true;
        return skinDataMap.containsKey(name);
    }

    public SkinData getSkinData(String name) {
        if (name == null) return defaultSkinData.clone();
        if ("default".equals(name)) return defaultSkinData.clone();
        if (skinDataMap.containsKey(name)) {
            return skinDataMap.get(name).clone();
        } else {
            return defaultSkinData.clone();
        }
    }

    public void setAsDefault(SkinData skin) {
        if (skin == null) throw new IllegalArgumentException();
        defaultSkinData = skin;
        save();
    }

    public SkinData removeSkinData(String name) {
        if (name == null) throw new IllegalArgumentException();
        SkinData ret = skinDataMap.remove(name);
        if (ret != null) {
            save();
        }
        return ret;
    }

    public void updateSkinData(String name, String desc, String val, String sig) {
        if (name == null || val == null || sig == null) {
            throw new IllegalArgumentException();
        }
        skinDataMap.put(name, new SkinData(name, desc, val, sig));
        save();
    }

    public void updateSkinData(String key, SkinData data) {
        skinDataMap.put(key, data);
        save();
    }
}
