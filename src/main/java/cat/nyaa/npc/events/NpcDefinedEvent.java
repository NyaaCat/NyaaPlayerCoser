package cat.nyaa.npc.events;

import cat.nyaa.npc.persistance.NpcData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Invoked when a NPC is newly defined.
 * Not cancellable.
 * WARN: you are not supposed to modify the npcData
 */
public class NpcDefinedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }

    private final String npcId;
    private final NpcData npcData;

    public NpcDefinedEvent(String npcId, NpcData npcData) {
        super();
        this.npcId = npcId;
        this.npcData = npcData;
    }

    public String getNpcId() {
        return npcId;
    }

    public NpcData getNpcData() {
        return npcData;
    }
}
