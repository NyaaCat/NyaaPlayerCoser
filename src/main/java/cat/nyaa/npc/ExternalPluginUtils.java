package cat.nyaa.npc;

import cat.nyaa.HamsterEcoHelper.HamsterEcoHelper;
import cat.nyaa.HamsterEcoHelper.database.Sign;
import cat.nyaa.HamsterEcoHelper.signshop.ShopMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExternalPluginUtils {
    public static class OperationNotSupportedException extends Exception {

    }

    private static HamsterEcoHelper heh = null;

    /**
     *
     * @param shopOwner
     * @param buyer the player who wants to buy from this shop
     * @param fakeLocation
     * @param shopId can be any string, stable identifiers are recommended.
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

        Sign fakeSign = new Sign();
        fakeSign.owner = shopOwner;
        fakeSign.shopMode = ShopMode.SELL; // shop SELL to player
        if (fakeLocation == null) fakeLocation = buyer.getLocation();
        fakeSign.x = (long)fakeLocation.getBlockX();
        fakeSign.y = (long)fakeLocation.getBlockY();
        fakeSign.z = (long)fakeLocation.getBlockZ();
        fakeSign.world = fakeLocation.getWorld().getName();

        heh.signShopManager.openShopGUI(buyer, fakeSign, 1);
    }
}
