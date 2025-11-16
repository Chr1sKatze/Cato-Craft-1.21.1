package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 📡 MenuSyncHelper — handles server↔client sync for dynamic menus.
 *
 * Replaces old EquipmentSyncHelper.
 * Provides both full and per-group diff-based sync.
 */
public final class MenuSyncHelper {

    private MenuSyncHelper() {}

    // Cache last sync hashes for optimization
    private static final Map<UUID, Map<String, Integer>> LAST_SYNC_HASHES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_FULL_HASHES = new HashMap<>();

    // ────────────────────────────────────────────────
    // FULL SYNC PAYLOAD (Server → Client)
    // ────────────────────────────────────────────────
    public record FullSyncPayload(CompoundTag tag) implements CustomPacketPayload {
        public static final Type<FullSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "menu_full_sync"));

        public static final StreamCodec<FriendlyByteBuf, FullSyncPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.COMPOUND_TAG, FullSyncPayload::tag,
                        FullSyncPayload::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // constants for registration
    public static final CustomPacketPayload.Type<FullSyncPayload> FULL_SYNC_TYPE = FullSyncPayload.TYPE;
    public static final StreamCodec<FriendlyByteBuf, FullSyncPayload> FULL_SYNC_CODEC = FullSyncPayload.STREAM_CODEC;

    // ────────────────────────────────────────────────
    // SERVER → CLIENT  full sync
    // ────────────────────────────────────────────────
    public static void syncFullToClient(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;

        PlayerEquipmentCapability cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        CompoundTag fullNBT = cap.serializeNBT(player.registryAccess());
        int currentHash = fullNBT.hashCode();
        Integer lastHash = LAST_FULL_HASHES.get(player.getUUID());

        if (lastHash != null && lastHash == currentHash) return; // no change

        LAST_FULL_HASHES.put(player.getUUID(), currentHash);
        PacketDistributor.sendToPlayer(player, new FullSyncPayload(fullNBT));

        CatocraftMod.LOGGER.debug("[MenuSyncHelper] Sent full sync to {}", player.getGameProfile().getName());
    }

    // ────────────────────────────────────────────────
    // CLIENT → HANDLER (receives FullSyncPayload)
    // ────────────────────────────────────────────────
    public static void handleFullSync(FullSyncPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            var cap = mc.player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
            if (cap == null) return;

            try {
                cap.deserializeNBT(mc.player.registryAccess(), msg.tag());
                cap.initializeGroupsIfMissing();
                cap.applyAllAttributes();

                CatocraftMod.LOGGER.debug("[MenuSyncHelper] Applied full sync ({} groups)", cap.getAllGroups().size());
            } catch (Exception e) {
                CatocraftMod.LOGGER.error("[MenuSyncHelper] Failed to apply sync: {}", e.toString());
            }
        });
    }

    // ────────────────────────────────────────────────
    // PARTIAL GROUP SYNC (optional)
    // ────────────────────────────────────────────────
    public static boolean syncGroupToClient(ServerPlayer player, EquipmentGroup group) {
        if (player == null || player.level().isClientSide) return false;

        PlayerEquipmentCapability cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return false;

        var handler = cap.getAllGroups().get(group);
        if (handler == null) return false;

        CompoundTag groupNBT = handler.serializeNBT(player.registryAccess());
        int hash = groupNBT.hashCode();
        var perPlayer = LAST_SYNC_HASHES.computeIfAbsent(player.getUUID(), id -> new HashMap<>());

        if (hash == perPlayer.getOrDefault(group.getKey(), -1)) return false;
        perPlayer.put(group.getKey(), hash);

        PacketDistributor.sendToPlayer(player, new MenuSlotUpdatePacket(group.getKey(), 0, null));

        return true;
    }

    // ────────────────────────────────────────────────
// FULL SYNC SUPPORT (restored from EquipmentSyncHelper)
// ────────────────────────────────────────────────

    /** Sends a full capability sync if the data has changed since last update. */
    public static void syncToClient(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;

        PlayerEquipmentCapability cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        CompoundTag fullNBT = cap.serializeNBT(player.registryAccess());
        int currentFullHash = fullNBT.hashCode();
        Integer lastHash = LAST_FULL_HASHES.get(player.getUUID());

        // Skip if unchanged
        if (lastHash != null && lastHash == currentFullHash) return;

        LAST_FULL_HASHES.put(player.getUUID(), currentFullHash);
        PacketDistributor.sendToPlayer(player, new FullSyncPayload(fullNBT));

        CatocraftMod.LOGGER.debug("[MenuSync] Full sync sent to {} ({} groups)",
                player.getGameProfile().getName(), cap.getAllGroups().size());
    }

    /** Forces a full sync regardless of hash or diff state. */
    public static void forceSyncToClient(ServerPlayer player) {
        if (player == null || player.level().isClientSide) return;

        PlayerEquipmentCapability cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        CompoundTag fullNBT = cap.serializeNBT(player.registryAccess());
        LAST_FULL_HASHES.put(player.getUUID(), fullNBT.hashCode());
        PacketDistributor.sendToPlayer(player, new FullSyncPayload(fullNBT));

        CatocraftMod.LOGGER.debug("[MenuSync] Force-synced data to {}", player.getGameProfile().getName());
    }
}