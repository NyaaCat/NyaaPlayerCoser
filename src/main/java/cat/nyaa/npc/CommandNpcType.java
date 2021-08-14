package cat.nyaa.npc;

import cat.nyaa.npc.npctype.NpcTypes;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;

public class CommandNpcType extends CommandReceiver {
    private final NyaaPlayerCoser plugin;
    public CommandNpcType(NyaaPlayerCoser plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "npctype";
    }

    @SubCommand(value = "list", permission = "npc.command.npctype.list")
    public void listNpcType(CommandSender sender, Arguments args) {
        NpcTypes.getNpcTypeList().forEach(npcType -> sender.sendMessage(npcType.getId()));
    }
}
