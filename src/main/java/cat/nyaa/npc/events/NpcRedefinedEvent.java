package cat.nyaa.npc.events;

import cat.nyaa.npc.persistance.NpcData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Invoked when a NPC definition is changed.
 * Not cancellable.
 * WARN: you are not supposed to modify the npcData
 * It may be true that oldData == newData, if the data is modified in-place
 */
public class NpcRedefinedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    private final String npcId;
    private final NpcData oldNpcData;
    private final NpcData newNpcData;

    public NpcRedefinedEvent(String npcId, NpcData oldNpcData, NpcData newNpcData) {
        this.npcId = npcId;
        this.oldNpcData = oldNpcData;
        this.newNpcData = newNpcData;
    }

    public String getNpcId() {
        return npcId;
    }

    public NpcData getOldNpcData() {
        return oldNpcData;
    }

    public NpcData getNewNpcData() {
        return newNpcData;
    }
}
