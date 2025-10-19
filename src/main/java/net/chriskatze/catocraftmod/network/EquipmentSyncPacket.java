package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.client.screen.EquipmentScreen;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 📦 EquipmentSyncPacket
 *
 * Server → Client: syncs the player's entire equipment capability (all groups).
 * Automatically updates the EquipmentScreen UI and reapplies attributes.
 *
 * This now uses dynamic string-based EquipmentGroups, so it supports
 * hot-reloaded and JSON-defined layouts instead of static enums.
 */
public record EquipmentSyncPacket(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<EquipmentSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "equipment_sync"));

    public static final StreamCodec<FriendlyByteBuf, EquipmentSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, EquipmentSyncPacket::tag,
                    EquipmentSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EquipmentSyncPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            CompoundTag nbt = msg.tag() == null ? new CompoundTag() : msg.tag();

            // 🧩 Retrieve the client capability instance
            PlayerEquipmentCapability cap = mc.player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
            if (cap == null) {
                CatocraftMod.LOGGER.error("[EquipmentSync] Player capability missing on client! Skipping sync.");
                return;
            }

            try {
                // Deserialize full NBT from server
                cap.deserializeNBT(mc.player.registryAccess(), nbt);
            } catch (Exception e) {
                CatocraftMod.LOGGER.error("[EquipmentSync] Failed to deserialize capability NBT: {}", e.toString());
                return;
            }

            // 🔁 Ensure all groups are initialized (handles new/removed JSON layouts)
            cap.initializeGroupsIfMissing();

            // ✅ Verify valid group keys for debugging (helps catch JSON/layout issues)
            if (nbt.contains("Groups", 9)) { // list of compound tags
                nbt.getList("Groups", 10).forEach(tagElement -> {
                    if (tagElement instanceof CompoundTag groupTag) {
                        String key = groupTag.getString("GroupKey");
                        if (EquipmentGroup.fromKey(key) == null) {
                            CatocraftMod.LOGGER.warn("[EquipmentSync] Unknown EquipmentGroup key received: {}", key);
                        }
                    }
                });
            }

            // 🪞 If the equipment screen is open, refresh it immediately
            if (mc.screen instanceof EquipmentScreen equipmentScreen) {
                equipmentScreen.refreshFromCapability(cap);
                CatocraftMod.LOGGER.debug("[EquipmentSync] EquipmentScreen refreshed after full sync.");
            }

            // ⚙️ Reapply attributes and visuals
            try {
                cap.applyAllAttributes();
            } catch (Exception e) {
                CatocraftMod.LOGGER.warn("[EquipmentSync] Could not reapply attributes after sync: {}", e.toString());
            }

            CatocraftMod.LOGGER.debug(
                    "[EquipmentSync] Client applied updated equipment data ({} groups).",
                    cap.getAllGroups().size()
            );
        });
    }
}