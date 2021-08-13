package cat.nyaa.npc.npctype;

import cat.nyaa.npc.I18n;
import cat.nyaa.npc.persistence.NpcData;
import cat.nyaa.npc.utils.RunCommandUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public class NpcTypeCommand extends AbstractNpcType{
    NpcTypeCommand(@Nonnull String id) {
        super(id);
    }

    @Override
    public boolean canBeSet(NpcData data) {
        return true;
    }

    @Override
    public boolean canSpawn(EntityType entityType, CommandSender sender) {
        return true;
    }

    @Override
    public void activateNpcForPlayer(NpcData npcData, String npcId, Player player) {
        if (npcData.npcCommand == null || npcData.npcCommand.equals("")) {
            player.sendMessage(I18n.format("user.interact.not_ready"));
            return;
        }
        RunCommandUtils.executeCommand(player, npcData.npcCommand, npcData.commandPermission);
    }
}
