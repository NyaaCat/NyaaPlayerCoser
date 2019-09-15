package cat.nyaa.npc.events;

import cat.nyaa.npc.persistence.TradeData;
import com.google.common.collect.ImmutableList;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Invoked when a trade data definition is changed.
 * Not cancellable.
 * WARN: you are not supposed to modify the trade data
 * It may be true that oldData == newData, if the data is modified in-place
 */
public class TradeRedefinedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    private final String tradeId;
    private final TradeData oldTradeData;
    private final TradeData newTradeData;
    private final ImmutableList<String> affectedNpc; // list of npcid

    public TradeRedefinedEvent(String tradeId, TradeData oldTradeData, TradeData newTradeData, ImmutableList<String> affectedNpc) {
        this.tradeId = tradeId;
        this.oldTradeData = oldTradeData;
        this.newTradeData = newTradeData;
        this.affectedNpc = affectedNpc;
    }

    public String getTradeId() {
        return tradeId;
    }

    public TradeData getOldTradeData() {
        return oldTradeData;
    }

    public TradeData getNewTradeData() {
        return newTradeData;
    }

    public ImmutableList<String> getAffectedNpc() {
        return affectedNpc;
    }
}