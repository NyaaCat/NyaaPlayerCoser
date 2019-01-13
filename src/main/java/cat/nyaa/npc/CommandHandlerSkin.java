package cat.nyaa.npc;

import cat.nyaa.npc.persistence.SkinData;
import cat.nyaa.nyaacore.CommandReceiver;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommandHandlerSkin extends CommandReceiver {
    private final NyaaPlayerCoser plugin;

    public CommandHandlerSkin(NyaaPlayerCoser plugin, I18n i18n) {
        super(plugin, i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return "skin";
    }

    @SubCommand(value = "add", permission = "npc.command.skin")
    public void addSkin(CommandSender sender, Arguments args) {
        String id = args.nextString();
        String val = args.nextString();
        String sig = args.nextString();
        String desc = args.next();
        if (plugin.cfg.skinData.hasSkinData(id)) {
            msg(sender, "user.skin.exists", id);
        } else {
            if (desc == null) desc = "";
            plugin.cfg.skinData.addSkinData(id, desc, val, sig);
        }
    }

    @SubCommand(value = "list", permission = "npc.command.skin")
    public void listSkins(CommandSender sender, Arguments args) {
        List<String> tmp = new ArrayList<>(plugin.cfg.skinData.skinDataMap.keySet());
        tmp.sort(Comparator.naturalOrder());
        for (String id : tmp) {
            SkinData skin = plugin.cfg.skinData.skinDataMap.get(id);
            String desc;
            if (skin.description == null || skin.description.length() <= 0) {
                desc = I18n.format("user.skin.list_no_desc");
            } else {
                desc = skin.description;
            }
            String line = I18n.substitute("user.skin.list",
                    "id", id,
                    "desc", desc,
                    "mask", skin.displayMask,
                    "uuid", skin.followPlayer == null ? "None" : skin.followPlayer);
            sender.sendMessage(line);
        }
    }

    @SubCommand(value = "setdefault", permission = "npc.command.skin")
    public void setDefaultSkin(CommandSender sender, Arguments args) {
        String id = args.nextString();
        if (!plugin.cfg.skinData.hasSkinData(id)) {
            msg(sender, "user.skin.notfound", id);
        } else {
            plugin.cfg.skinData.setAsDefault(plugin.cfg.skinData.getSkinData(id));
        }
    }
}
