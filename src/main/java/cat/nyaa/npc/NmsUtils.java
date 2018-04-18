package cat.nyaa.npc;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Entity;

import java.util.UUID;

public final class NmsUtils {
    /* see CommandEntityData.java */
    public static void setEntityTag(Entity e, String tag) {
        net.minecraft.server.v1_12_R1.Entity nmsEntity = ((CraftEntity) e).getHandle();

        if (nmsEntity instanceof EntityHuman) {
            throw new IllegalArgumentException("Player NBT cannot be edited");
        } else {
            NBTTagCompound nbtIn;
            try {
                nbtIn = MojangsonParser.parse(tag);
            } catch (MojangsonParseException ex) {
                throw new IllegalArgumentException("Invalid NBTTag string");
            }

            NBTTagCompound nmsOrigNBT = CommandAbstract.a(nmsEntity); // entity to nbt
            NBTTagCompound nmsClonedNBT = nmsOrigNBT.g(); // clone

            UUID uuid = nmsEntity.getUniqueID(); // er... store UUID?
            nmsOrigNBT.a(nbtIn); // merge
            nmsEntity.a(uuid);   // so what's the point here?

            if (nmsOrigNBT.equals(nmsClonedNBT)) {
                return;
            } else {
                nmsEntity.f(nmsOrigNBT); // set nbt
            }
        }
    }

    public static double getBlockHeight(org.bukkit.block.Block block) {
        Block nmsBlock = CraftMagicNumbers.getBlock(block);
        return nmsBlock.getBlockData().e(((CraftWorld) block.getWorld()).getHandle(), new BlockPosition(block.getX(), block.getY(), block.getZ())).e;
    }
}
