package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.EquipmentMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → Server packet for opening a dynamic Equipment/Inventory menu.
 */
public record OpenEquipmentMenuPacket(ResourceLocation menuId) implements CustomPacketPayload {

    public static final Type<OpenEquipmentMenuPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "open_equipment_menu"));

    public static final StreamCodec<FriendlyByteBuf, OpenEquipmentMenuPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, OpenEquipmentMenuPacket::menuId, // ← change here
                    OpenEquipmentMenuPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEquipmentMenuPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null) return;

            if (msg.menuId().equals(CatocraftMod.id("equipment"))) {
                player.openMenu(new EquipmentMenu.Provider());
                CatocraftMod.LOGGER.debug("[OpenEquipmentMenuPacket] Opened unified equipment menu for {}", player.getName().getString());
            } else {
                CatocraftMod.LOGGER.warn("[OpenEquipmentMenuPacket] Unknown menu ID: {}", msg.menuId());
            }
        });
    }
}