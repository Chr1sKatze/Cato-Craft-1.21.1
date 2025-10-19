package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles syncing the unified PlayerEquipmentCapability to the client.
 * Uses diff-based per-group sync to minimize data transfer.
 */
public final class EquipmentSyncHelper {

    private EquipmentSyncHelper() {}

    // Cache last synced NBT hash for each player → groupKey
    private static final Map<UUID, Map<String, Integer>> LAST_SYNC_HASHES = new HashMap<>();

    // Cache total hash (for full sync diff)
    private static final Map<UUID, Integer> LAST_FULL_HASHES = new HashMap<>();

    // ────────────────────────────────────────────────
    // FULL SYNC
    // ────────────────────────────────────────────────

    /** Sends a full capability sync if it has changed since last full hash. */
    public static void syncToClient(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;

        PlayerEquipmentCapability cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        CompoundTag fullNBT = cap.serializeNBT(player.registryAccess());
        int currentFullHash = fullNBT.hashCode();
        Integer lastHash = LAST_FULL_HASHES.get(player.getUUID());

        if (lastHash != null && lastHash == currentFullHash) return; // no change detected

        LAST_FULL_HASHES.put(player.getUUID(), currentFullHash);
        PacketDistributor.sendToPlayer(player, new EquipmentSyncPacket(fullNBT));

        CatocraftMod.LOGGER.debug("[EquipmentSync] Full sync sent to {} ({} groups)",
                player.getName().getString(), cap.getAllGroups().size());
    }

    /** Forces a full equipment sync regardless of diff. */
    public static void forceSyncToClient(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;

        PlayerEquipmentCapability cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        CompoundTag fullNBT = cap.serializeNBT(player.registryAccess());
        LAST_FULL_HASHES.put(player.getUUID(), fullNBT.hashCode());
        PacketDistributor.sendToPlayer(player, new EquipmentSyncPacket(fullNBT));

        CatocraftMod.LOGGER.debug("[EquipmentSync] Force-synced data to {}", player.getName().getString());
    }

    // ────────────────────────────────────────────────
    // PARTIAL SYNC
    // ────────────────────────────────────────────────

    /**
     * Syncs only the given group to the client if changed since last hash.
     * @return true if a packet was sent, false if skipped (no change or invalid state)
     */
    public static boolean syncGroupToClient(ServerPlayer player, EquipmentGroup group) {
        if (player == null || player.level().isClientSide) return false;

        PlayerEquipmentCapability cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return false;

        var handler = cap.getAllGroups().get(group);
        if (handler == null) return false;

        // Serialize the group
        CompoundTag groupNBT = handler.serializeNBT(player.registryAccess());
        int groupHash = groupNBT.hashCode();
        String groupKey = group.getKey();

        // Get or create player hash map
        var perPlayerHashes = LAST_SYNC_HASHES.computeIfAbsent(player.getUUID(),
                uuid -> new HashMap<>());

        Integer lastHash = perPlayerHashes.get(groupKey);
        if (lastHash != null && lastHash == groupHash) {
            // No change detected
            return false;
        }

        // Store new hash and send packet
        perPlayerHashes.put(groupKey, groupHash);
        PacketDistributor.sendToPlayer(player, new EquipmentPartialSyncPacket(groupKey, groupNBT));

        CatocraftMod.LOGGER.debug("[EquipmentSync] Sent partial sync for group '{}' to {}",
                groupKey, player.getGameProfile().getName());
        return true;
    }

    // ────────────────────────────────────────────────
    // UTILITY
    // ────────────────────────────────────────────────

    /** Clears cache when player logs out or server stops. */
    public static void clearPlayer(UUID uuid) {
        LAST_SYNC_HASHES.remove(uuid);
        LAST_FULL_HASHES.remove(uuid);
    }
}