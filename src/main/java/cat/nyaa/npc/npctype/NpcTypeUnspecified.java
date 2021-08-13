package cat.nyaa.npc.npctype;

import cat.nyaa.npc.persistence.NpcData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

public class NpcTypeUnspecified extends AbstractNpcType{
    NpcTypeUnspecified(String id) {
        super(id);
    }

    @Override
    public boolean canSpawn(EntityType entityType, CommandSender sender) {
        return false;
    }

    @Override
    public boolean canBeSet(NpcData data) {
        return false;
    }
}
