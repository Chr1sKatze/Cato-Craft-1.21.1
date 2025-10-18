package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.SoulStoneCapabilityHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SoulStoneSyncHelper {

    /** Sends the player’s Soul Stone slot data to their client. */
    public static void syncToClient(ServerPlayer player) {
        var cap = player.getCapability(SoulStoneCapabilityHandler.SOULSTONE_CAP);
        if (cap instanceof ItemStackHandler handler) {
            // ✅ registry-aware serialization
            CompoundTag tag = handler.serializeNBT(player.registryAccess());
            NetworkHandler.sendToPlayer(player, new SoulStoneSyncPacket(tag));
            CatocraftMod.LOGGER.debug("Synced Soul Stone data to client for {}", player.getName().getString());
        } else {
            CatocraftMod.LOGGER.warn("No Soul Stone handler found for {}", player.getName().getString());
        }
    }
}