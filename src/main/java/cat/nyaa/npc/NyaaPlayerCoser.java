package cat.nyaa.npc;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class NyaaPlayerCoser extends JavaPlugin {
    public static final String PLUGIN_COMMAND_NAME = "nyaaplayercoser";
    public static NyaaPlayerCoser instance;
    public static boolean debugEnabled = false;

    public static void debug(Consumer<Logger> logWriter) {
        if (!debugEnabled) return;
        Logger l = instance == null ? Bukkit.getLogger() : instance.getLogger();
        l.info("[NPC DEBUG] THREAD = " + Thread.currentThread().getName());
        try {
            logWriter.accept(l);
        } catch (Throwable ex) {
            l.severe("[NPC DEBUG] Exception thrown from log writer");
            ex.printStackTrace();
        }
    }

    public static void trace(Consumer<Logger> logWriter) {
        if (!debugEnabled) return;
        Logger l = instance == null ? Bukkit.getLogger() : instance.getLogger();
        l.info("[NPC TRACE] THREAD = " + Thread.currentThread().getName());
        try {
            if (logWriter != null) logWriter.accept(l);
            l.info("[NPC TRACE]" + ExceptionUtils.getFullStackTrace(new Throwable()));
        } catch (Throwable ex) {
            l.severe("[NPC TRACE] Exception thrown from log writer");
            ex.printStackTrace();
        }
    }

    public I18n i18n;
    public Configuration cfg;
    public CommandHandler cmd;
    public EntitiesManager entitiesManager;
    public TradingController tradingController;

    @Override
    public void onEnable() {
        instance = this;
        cfg = new Configuration(this);
        cfg.load();
        i18n = new I18n(this, cfg.language);
        cmd = new CommandHandler(this, i18n);
        getCommand(PLUGIN_COMMAND_NAME).setExecutor(cmd);
        getCommand(PLUGIN_COMMAND_NAME).setTabCompleter(cmd);
        tradingController = new TradingController(this);
        entitiesManager = new EntitiesManager(this);
    }

    @Override
    public void onDisable() {
        entitiesManager.destructor();
        tradingController.destructor();
        getServer().getScheduler().cancelTasks(this);
        getCommand(PLUGIN_COMMAND_NAME).setExecutor(null);
        getCommand(PLUGIN_COMMAND_NAME).setTabCompleter(null);
        HandlerList.unregisterAll(this);
        cfg.save();
    }

    public void onReload() {
        entitiesManager.destructor();
        tradingController.destructor();
        getServer().getScheduler().cancelTasks(this);
        getCommand(PLUGIN_COMMAND_NAME).setExecutor(null);
        getCommand(PLUGIN_COMMAND_NAME).setTabCompleter(null);
        HandlerList.unregisterAll(this);
        onEnable();
    }
}
