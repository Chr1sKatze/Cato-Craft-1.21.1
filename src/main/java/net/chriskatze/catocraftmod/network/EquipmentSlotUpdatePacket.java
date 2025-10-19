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
 * 📤 EquipmentSlotUpdatePacket
 *
 * Client → Server:
 * Triggered when the player modifies a slot in the EquipmentMenu.
 * The server updates the PlayerEquipmentCapability and re-syncs attributes and data.
 *
 * ✅ Updated to use dynamic group keys instead of enum constants.
 */
public record EquipmentSlotUpdatePacket(String groupKey, int slotIndex, ItemStack stack)
        implements CustomPacketPayload {

    public static final Type<EquipmentSlotUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "equipment_slot_update"));

    // 👇 Use RegistryFriendlyByteBuf because ItemStack.STREAM_CODEC needs it
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentSlotUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, EquipmentSlotUpdatePacket::groupKey,
                    ByteBufCodecs.VAR_INT, EquipmentSlotUpdatePacket::slotIndex,
                    ItemStack.STREAM_CODEC, EquipmentSlotUpdatePacket::stack,
                    EquipmentSlotUpdatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EquipmentSlotUpdatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null) return;

            PlayerEquipmentCapability cap = EquipmentCapabilityHandler.get(player);
            if (cap == null) return;

            EquipmentGroup group = EquipmentGroup.fromKey(msg.groupKey());
            if (group == null) {
                CatocraftMod.LOGGER.warn("[EquipmentSlotUpdate] Unknown group key '{}' — skipping update.", msg.groupKey());
                return;
            }

            // Apply item change to the correct slot
            cap.setItem(group, msg.stack());
            cap.applyAllAttributes();

            CatocraftMod.LOGGER.debug(
                    "[EquipmentSlotUpdate] Server applied slot update for group '{}' index {} (stack: {})",
                    msg.groupKey(), msg.slotIndex(), msg.stack().getItem()
            );

            // ✅ Partial group sync (optimized)
            boolean success = EquipmentSyncHelper.syncGroupToClient(
                    (net.minecraft.server.level.ServerPlayer) player, group
            );

            if (success) {
                CatocraftMod.LOGGER.debug(
                        "[EquipmentSlotUpdate] Partial sync sent for '{}' to {}",
                        msg.groupKey(), player.getGameProfile().getName()
                );
            } else {
                CatocraftMod.LOGGER.trace(
                        "[EquipmentSlotUpdate] No change detected for '{}' — sync skipped.",
                        msg.groupKey()
                );
            }
        });
    }

    public static void sendToServer(EquipmentGroup group, int slotIndex, ItemStack stack) {
        if (group == null) {
            CatocraftMod.LOGGER.warn("[EquipmentSlotUpdate] Tried to send packet with null group.");
            return;
        }
        PacketDistributor.sendToServer(new EquipmentSlotUpdatePacket(group.getKey(), slotIndex, stack));
    }
}