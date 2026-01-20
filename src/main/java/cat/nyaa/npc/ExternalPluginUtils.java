package cat.nyaa.npc;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExternalPluginUtils {
    public static class OperationNotSupportedException extends Exception {

    }

    /**
     * HamsterEcoHelper integration is currently disabled pending API compatibility update.
     *
     * @param shopOwner
     * @param buyer        the player who wants to buy from this shop
     * @param fakeLocation
     * @param shopId       can be any string, stable identifiers are recommended.
     * @throws OperationNotSupportedException always thrown - HEH integration disabled
     */
    public static void hehOpenPlayerShop(UUID shopOwner, Player buyer, Location fakeLocation, String shopId) throws OperationNotSupportedException {
        // HamsterEcoHelper integration disabled - API has changed
        throw new OperationNotSupportedException();
    }

    private static ProtocolManager pm = null;

    public static ProtocolManager getPM() {
        if (pm == null) {
            pm = ProtocolLibrary.getProtocolManager();
        }
        return pm;
    }
}
