package cat.nyaa.npc;

import cat.nyaa.nyaacore.CommandReceiver;

public class CommandHandler extends CommandReceiver {
    private final NyaaPlayerCoser plugin;

    public CommandHandler(NyaaPlayerCoser plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }
}
