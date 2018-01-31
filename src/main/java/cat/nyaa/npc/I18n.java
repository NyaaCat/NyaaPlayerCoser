package cat.nyaa.npc;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.plugin.java.JavaPlugin;

public class I18n extends LanguageRepository {
    private static I18n instance;
    private final NyaaPlayerCoser plugin;
    private final String lang;

    @Override
    protected JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    protected String getLanguage() {
        return lang;
    }

    public I18n(NyaaPlayerCoser plugin, String lang) {
        instance = this;
        this.plugin = plugin;
        this.lang = lang;
        load();
    }

    public static String format(String key, Object... args) {
        return instance.getFormatted(key, args);
    }
}
