package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.client.screen.EquipmentScreen;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 📦 EquipmentPartialSyncPacket
 *
 * Server → Client: syncs a *single equipment group* (faster than full sync).
 * Uses string-based group key instead of enum for data-driven groups.
 */
public record EquipmentPartialSyncPacket(String groupKey, CompoundTag tag)
        implements CustomPacketPayload {

    public static final Type<EquipmentPartialSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "equipment_partial_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentPartialSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, EquipmentPartialSyncPacket::groupKey,
                    ByteBufCodecs.COMPOUND_TAG, EquipmentPartialSyncPacket::tag,
                    EquipmentPartialSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EquipmentPartialSyncPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            PlayerEquipmentCapability cap = mc.player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
            if (cap == null) {
                CatocraftMod.LOGGER.warn("[PartialSync] Missing PlayerEquipmentCapability on client!");
                return;
            }

            cap.initializeGroupsIfMissing();

            EquipmentGroup group = EquipmentGroup.fromKey(msg.groupKey());
            if (group == null) {
                CatocraftMod.LOGGER.warn("[PartialSync] Unknown group key '{}' on client; skipping update.", msg.groupKey());
                return;
            }

            var groupHandler = cap.getAllGroups().get(group);
            if (groupHandler == null) {
                CatocraftMod.LOGGER.warn("[PartialSync] Group '{}' exists in registry but not in capability.", msg.groupKey());
                return;
            }

            try {
                groupHandler.deserializeNBT(mc.player.registryAccess(), msg.tag());
            } catch (Exception e) {
                CatocraftMod.LOGGER.error("[PartialSync] Failed to update group '{}': {}", msg.groupKey(), e.toString());
                return;
            }

            // 🪞 UI Refresh if open
            if (mc.screen instanceof EquipmentScreen equipmentScreen) {
                equipmentScreen.refreshFromCapability(cap);
            }

            // ⚙️ Reapply visual + attribute effects instantly
            try {
                cap.applyAllAttributes();
            } catch (Exception e) {
                CatocraftMod.LOGGER.warn("[PartialSync] Could not reapply attributes for '{}': {}", msg.groupKey(), e.toString());
            }
        });
    }
}