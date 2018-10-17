package cat.nyaa.npc.events;

import cat.nyaa.npc.persistance.NpcData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Invoked when a NPC definition is removed.
 * Not cancellable.
 * WARN: you are not supposed to modify the npcData
 */
public class NpcUndefinedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }

    private final String npcId;
    private final NpcData oldNpcData;

    public NpcUndefinedEvent(String npcId, NpcData oldNpcData) {
        this.npcId = npcId;
        this.oldNpcData = oldNpcData;
    }

    public String getNpcId() {
        return npcId;
    }

    public NpcData getOldNpcData() {
        return oldNpcData;
    }
}
