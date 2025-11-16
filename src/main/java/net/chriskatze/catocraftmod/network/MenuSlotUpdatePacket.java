package net.chriskatze.catocraftmod.network;


import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 📤 MenuSlotUpdatePacket
 *
 * Client → Server:
 * Sent when the player modifies a slot in any dynamic menu.
 * The server updates the PlayerEquipmentCapability and re-syncs data.
 */
public record MenuSlotUpdatePacket(String groupKey, int slotIndex, ItemStack stack)
        implements CustomPacketPayload {

    public static final Type<MenuSlotUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "menu_slot_update"));

    // 👇 Use RegistryFriendlyByteBuf because ItemStack.STREAM_CODEC depends on it
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuSlotUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, MenuSlotUpdatePacket::groupKey,
                    ByteBufCodecs.VAR_INT, MenuSlotUpdatePacket::slotIndex,
                    ItemStack.STREAM_CODEC, MenuSlotUpdatePacket::stack,
                    MenuSlotUpdatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ────────────────────────────────────────────────
    // Server handler
    // ────────────────────────────────────────────────
    public static void handle(MenuSlotUpdatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null) return;

            PlayerEquipmentCapability cap = EquipmentCapabilityHandler.get(player);
            if (cap == null) return;

            EquipmentGroup group = EquipmentGroup.fromKey(msg.groupKey());
            if (group == null) {
                CatocraftMod.LOGGER.warn("[MenuSlotUpdate] Unknown group key '{}' — skipping update.", msg.groupKey());
                return;
            }

            try {
                // Apply item to the correct slot
                cap.setItem(group, msg.stack());
                cap.applyAllAttributes();

                CatocraftMod.LOGGER.debug(
                        "[MenuSlotUpdate] Server applied slot update for '{}' index {} (stack: {})",
                        msg.groupKey(), msg.slotIndex(), msg.stack().getItem()
                );

                // Partial sync back to client (diff-aware)
                boolean success = MenuSyncHelper.syncGroupToClient((net.minecraft.server.level.ServerPlayer) player, group);
                if (success) {
                    CatocraftMod.LOGGER.debug("[MenuSlotUpdate] Partial sync sent for '{}' to {}", msg.groupKey(), player.getGameProfile().getName());
                } else {
                    CatocraftMod.LOGGER.trace("[MenuSlotUpdate] No change detected for '{}' — sync skipped.", msg.groupKey());
                }
            } catch (Exception e) {
                CatocraftMod.LOGGER.error("[MenuSlotUpdate] Failed to apply slot update for '{}': {}", msg.groupKey(), e.toString());
            }
        });
    }

    // ────────────────────────────────────────────────
    // Client helper
    // ────────────────────────────────────────────────
    public static void sendToServer(EquipmentGroup group, int slotIndex, ItemStack stack) {
        if (group == null) {
            CatocraftMod.LOGGER.warn("[MenuSlotUpdate] Tried to send packet with null group.");
            return;
        }
        PacketDistributor.sendToServer(new MenuSlotUpdatePacket(group.getKey(), slotIndex, stack));
    }
}