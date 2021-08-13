package cat.nyaa.npc.npctype;

import cat.nyaa.npc.I18n;
import cat.nyaa.npc.NyaaPlayerCoser;
import cat.nyaa.npc.ephemeral.NyaaMerchant;
import cat.nyaa.npc.persistence.NpcData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import javax.annotation.Nonnull;

public abstract class AbstractNpcType {
    protected final NyaaPlayerCoser plugin;
    private final String typeId;
    AbstractNpcType(@Nonnull String id){
        this.typeId=id;
        this.plugin=NyaaPlayerCoser.instance;
    }
    public void activateNpcForPlayer(NpcData npcData, String npcId, Player player) {
        player.sendMessage(I18n.format("user.interact.not_ready"));
    }
    public abstract boolean canSpawn(EntityType entityType, CommandSender sender);


    public String getId() {
        return typeId;
    }

    public void playerInteractWithWindow(InventoryClickEvent ev, NpcData data, NyaaMerchant m) {
    }

    public abstract boolean canBeSet(NpcData data);
}
