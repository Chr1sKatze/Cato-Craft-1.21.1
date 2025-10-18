package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Central registration for all Catocraft network packets.
 */
public class NetworkHandler {

    public static final ResourceLocation REVELATION_GLOW_ID =
            ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "revelation_glow");

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CatocraftMod.MOD_ID)
                .versioned("1")
                .optional();

        // 1️⃣ RevelationGlowPacket (server → client)
        registrar.playToClient(
                RevelationGlowPacket.TYPE,
                RevelationGlowPacket.STREAM_CODEC,
                RevelationGlowPacket::handle
        );

        // 2️⃣ EarringSyncPacket (server → client)
        registrar.playToClient(
                EarringSyncPacket.TYPE,
                EarringSyncPacket.STREAM_CODEC,
                EarringSyncPacket::handle
        );

        // 3️⃣ OpenEarringMenuPacket (client → server)
        registrar.playToServer(
                OpenEarringMenuPacket.TYPE,
                OpenEarringMenuPacket.STREAM_CODEC,
                OpenEarringMenuPacket::handle
        );

        // 4️⃣ SoulStoneSyncPacket (server → client)
        registrar.playToClient(
                SoulStoneSyncPacket.TYPE,
                SoulStoneSyncPacket.STREAM_CODEC,
                SoulStoneSyncPacket::handle
        );

        CatocraftMod.LOGGER.info(
                "[NetworkHandler] Registered packets: revelation_glow, earring_sync, open_earring_menu, earring_slot_changed"
        );
    }

    /** Utility for sending to one specific player (server → client). */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}