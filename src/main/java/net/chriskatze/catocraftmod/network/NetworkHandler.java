package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 📡 Central registration for all Catocraft network packets.
 *
 * Handles both:
 *  - server → client syncs (e.g. EquipmentSyncPacket)
 *  - client → server updates (e.g. EquipmentSlotUpdatePacket)
 */
public class NetworkHandler {

    public static final ResourceLocation REVELATION_GLOW_ID =
            ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "revelation_glow");

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CatocraftMod.MOD_ID)
                .versioned("1")
                .optional();

        // ────────────────────────────────────────────────
        // Server → Client
        // ────────────────────────────────────────────────

        // 1️⃣ RevelationGlowPacket
        registrar.playToClient(
                RevelationGlowPacket.TYPE,
                RevelationGlowPacket.STREAM_CODEC,
                RevelationGlowPacket::handle
        );

        // 2️⃣ Unified EquipmentSyncPacket
        registrar.playToClient(
                EquipmentSyncPacket.TYPE,
                EquipmentSyncPacket.STREAM_CODEC,
                EquipmentSyncPacket::handle
        );

        // ────────────────────────────────────────────────
        // Client → Server
        // ────────────────────────────────────────────────

        // 3️⃣ OpenEquipmentMenuPacket
        registrar.playToServer(
                OpenEquipmentMenuPacket.TYPE,
                OpenEquipmentMenuPacket.STREAM_CODEC,
                OpenEquipmentMenuPacket::handle
        );

        // 4️⃣ EquipmentSlotUpdatePacket (slot drag/drop updates)
        registrar.playToServer(
                EquipmentSlotUpdatePacket.TYPE,
                EquipmentSlotUpdatePacket.STREAM_CODEC,
                EquipmentSlotUpdatePacket::handle
        );

        CatocraftMod.LOGGER.info(
                "[NetworkHandler] Registered packets: revelation_glow, equipment_sync, open_equipment_menu, equipment_slot_update"
        );
    }

    /** Utility for sending to one specific player (server → client). */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}