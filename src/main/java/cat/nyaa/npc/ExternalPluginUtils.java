package cat.nyaa.npc;

import cat.nyaa.heh.HamsterEcoHelper;
import cat.nyaa.heh.business.signshop.SignShopSell;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExternalPluginUtils {
    public static class OperationNotSupportedException extends Exception {

    }

    private static HamsterEcoHelper heh = null;

    /**
     * @param shopOwner
     * @param buyer        the player who wants to buy from this shop
     * @param fakeLocation
     * @param shopId       can be any string, stable identifiers are recommended.
     * @throws OperationNotSupportedException
     */
    public static void hehOpenPlayerShop(UUID shopOwner, Player buyer, Location fakeLocation, String shopId) throws OperationNotSupportedException {
        if (shopOwner == null || buyer == null || shopId == null) {
            throw new IllegalArgumentException();
        }

        if (heh == null) {
            heh = (HamsterEcoHelper) Bukkit.getPluginManager().getPlugin("HamsterEcoHelper");
            if (heh == null)
                throw new OperationNotSupportedException();
        }

        SignShopSell fakeSign = new SignShopSell(shopOwner);
        fakeSign.newGUI().open(buyer);
    }

    private static ProtocolManager pm = null;

    public static ProtocolManager getPM() {
        if (pm == null) {
            pm = ProtocolLibrary.getProtocolManager();
        }
        return pm;
    }
}
