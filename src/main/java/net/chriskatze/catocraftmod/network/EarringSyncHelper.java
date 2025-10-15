package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EarringCapabilityHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.ItemStackHandler;

public class EarringSyncHelper {

    /** Sends the player’s earring slot data to their client. */
    public static void syncToClient(ServerPlayer player) {
        var cap = player.getCapability(EarringCapabilityHandler.EARRING_CAP);
        if (cap instanceof ItemStackHandler handler) {
            // ✅ registry-aware serialization
            CompoundTag tag = handler.serializeNBT(player.registryAccess());
            NetworkHandler.sendToPlayer(player, new EarringSyncPacket(tag));
            CatocraftMod.LOGGER.debug("Synced earring data to client for {}", player.getName().getString());
        } else {
            CatocraftMod.LOGGER.warn("No earring handler found for {}", player.getName().getString());
        }
    }
}