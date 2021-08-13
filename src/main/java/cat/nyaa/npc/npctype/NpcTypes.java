package cat.nyaa.npc.npctype;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcTypes {
    private static final Map<String, AbstractNpcType> NPC_TYPES = new HashMap<>();

    /*
    UNSPECIFIED,
    TRADER_BOX,
    TRADER_UNLIMITED,
    HEH_SELL_SHOP, // shop sell to player
    COMMAND
     */
    public static final AbstractNpcType UNSPECIFIED = register(new NpcTypeUnspecified("UNSPECIFIED"));
    public static final AbstractNpcType TRADER_UNLIMITED = register(new NpcTypeTraderUnlimited("TRADER_UNLIMITED"));
    public static final AbstractNpcType HEH_SELL_SHOP = register(new NpcTypeHehSellShop("HEH_SELL_SHOP"));
    public static final AbstractNpcType COMMAND = register(new NpcTypeCommand("COMMAND"));

    public static AbstractNpcType register(AbstractNpcType npcType) throws IllegalArgumentException {
        return register(npcType.getId(),npcType);
    }
    public static AbstractNpcType register(String id, AbstractNpcType npcType) throws IllegalArgumentException {
        if (NPC_TYPES.containsKey(id)) {
            AbstractNpcType oldType = NPC_TYPES.get(id);
            if (oldType != null && oldType != npcType)
                throw new IllegalArgumentException("NpcType:" + id + " already exists.");
            return oldType;
        } else {
            NPC_TYPES.put(id, npcType);
            return npcType;
        }
    }

    @Nullable
    public static AbstractNpcType getNpcType(String id) {
        if (NPC_TYPES.containsKey(id))
            return NPC_TYPES.get(id);
        return null;
    }
    public static boolean hasNpcType (String id) {
        return NPC_TYPES.containsKey(id);
    }

    public static List<AbstractNpcType> getNpcTypeList() {
        List<AbstractNpcType> result = new ArrayList<>();
        for (Map.Entry<String, AbstractNpcType> stringAbstractNpcTypeEntry : NPC_TYPES.entrySet()) {
            result.add(stringAbstractNpcTypeEntry.getValue());
        }
        return result;
    }
}