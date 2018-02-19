package cat.nyaa.npc;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class NyaaPlayerCoser extends JavaPlugin {
    public static final String PLUGIN_COMMAND_NAME = "nyaaplayercoser";
    public static NyaaPlayerCoser instance;

    public I18n i18n;
    public Configuration cfg;
    public CommandHandler cmd;
    public EntitiesManager entitiesManager;
    public AIController ai;

    @Override
    public void onEnable() {
        instance = this;
        cfg = new Configuration(this);
        cfg.load();
        i18n = new I18n(this, cfg.language);
        cmd = new CommandHandler(this, i18n);
        getCommand(PLUGIN_COMMAND_NAME).setExecutor(cmd);
        getCommand(PLUGIN_COMMAND_NAME).setTabCompleter(cmd);
        ai = new AIController(this);
        entitiesManager = new EntitiesManager(this);
    }

    @Override
    public void onDisable() {
        entitiesManager.destructor();
        ai.destructor();
        getServer().getScheduler().cancelTasks(this);
        getCommand(PLUGIN_COMMAND_NAME).setExecutor(null);
        getCommand(PLUGIN_COMMAND_NAME).setTabCompleter(null);
        HandlerList.unregisterAll(this);
        cfg.save();
    }

    public void onReload() {
        entitiesManager.destructor();
        ai.destructor();
        getServer().getScheduler().cancelTasks(this);
        getCommand(PLUGIN_COMMAND_NAME).setExecutor(null);
        getCommand(PLUGIN_COMMAND_NAME).setTabCompleter(null);
        HandlerList.unregisterAll(this);
        onEnable();
    }
}
