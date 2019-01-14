package cat.nyaa.npc;

import cat.nyaa.npc.persistence.SkinData;
import cat.nyaa.nyaacore.CommandReceiver;
import com.google.common.collect.Iterators;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_13_R2.CraftOfflinePlayer;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

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

    public static <T> T nullableCall(Supplier<T> statement) {
        try {
            return statement.get();
        } catch (NullPointerException exc) {
            return null;
        }
    }

    @SubCommand(value = "pin", permission = "npc.command.skin")
    public void pinSkin(CommandSender sender, Arguments args) {
        // /npc skin pin <playerName> [follow] [skinId]
        String playerName = args.nextString();
        boolean follow = false;
        String skinId = playerName;
        if (args.next() != null) {
            follow = args.nextBoolean();
            if (args.next() != null) {
                skinId = args.nextString();
            }
        }

        OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
        if (p instanceof CraftPlayer) {
            CraftPlayer cp = (CraftPlayer) p;
            Property textures = nullableCall(() -> {
                return Iterators.get(
                        cp.getProfile().getProperties().get("textures").iterator(), 0, null);
            });
            if (textures == null || !textures.hasSignature()) {
                msg(sender, "user.skin.pin_fail");
            } else {
                SkinData sd = new SkinData(skinId, playerName, textures, follow ? cp.getUniqueId() : null);
                plugin.cfg.skinData.updateSkinData(skinId, sd);
                msg(sender, "user.skin.pin_success");
            }
        } else if (p instanceof CraftOfflinePlayer) {
            CraftOfflinePlayer cop = (CraftOfflinePlayer) p;
            Property textures = nullableCall(() -> {
                return Iterators.get(
                        cop.getProfile().getProperties().get("textures").iterator(), 0, null);
            });
            if (textures == null || !textures.hasSignature()) {
                msg(sender, "user.skin.pin_fail");
            } else {
                SkinData sd = new SkinData(skinId, playerName, textures, follow ? cop.getUniqueId() : null);
                plugin.cfg.skinData.updateSkinData(skinId, sd);
                msg(sender, "user.skin.pin_success");
            }
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
