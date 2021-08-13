package cat.nyaa.npc.npctype;

import cat.nyaa.npc.ExternalPluginUtils;
import cat.nyaa.npc.I18n;
import cat.nyaa.npc.persistence.NpcData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;

public class NpcTypeHehSellShop extends AbstractNpcType {
    NpcTypeHehSellShop(@Nonnull String id) {
        super(id);
    }

    @Override
    public boolean canBeSet(NpcData data) {
        if (data.hehShopOwnerUUID == null)
            data.hehShopOwnerUUID = data.ownerId;
        return true;
    }

    @Override
    public void activateNpcForPlayer(NpcData npcData, String npcId, Player player) {
        try {
            ExternalPluginUtils.hehOpenPlayerShop(npcData.ownerId, player, player.getLocation(), "npc-" + npcId);
        } catch (ExternalPluginUtils.OperationNotSupportedException ex) {
            player.sendMessage(I18n.format("user.interact.heh_not_support"));
        }
    }

    @Override
    public boolean canSpawn(EntityType entityType, CommandSender sender) {
        return true;
    }
}
