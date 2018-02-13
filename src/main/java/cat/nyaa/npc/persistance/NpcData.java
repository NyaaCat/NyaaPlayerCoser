package cat.nyaa.npc.persistance;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class NpcData implements ISerializable {
    public NpcData() { }
    public NpcData(Location loc, String displayName, EntityType type) {
        this.location = loc;
        this.displayName = displayName;
        this.type = type;
    }
    @Serializable
    public Location location;
    @Serializable
    public String displayName;
    @Serializable
    public EntityType type;
    @Serializable
    public boolean enabled = true;
}
