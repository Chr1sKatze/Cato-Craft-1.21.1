package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 📡 NetworkHandler — unified packet registry
 *
 * Registers all custom Catocraft payloads for menu sync, slot updates,
 * and other gameplay communications.
 */
public final class NetworkHandler {

    private NetworkHandler() {}

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event
                .registrar(CatocraftMod.MOD_ID)
                .versioned("1")
                .optional();

        // ────────────────────────────────────────────────
        // 🖥️ Server → Client
        // ────────────────────────────────────────────────
        registrar.playToClient(
                MenuSyncHelper.FULL_SYNC_TYPE,
                MenuSyncHelper.FULL_SYNC_CODEC,
                MenuSyncHelper::handleFullSync
        );

        // (Optional) future: add ClientMenuOpener or partial sync packets here
        // registrar.playToClient(ClientMenuOpener.TYPE, ClientMenuOpener.STREAM_CODEC, ClientMenuOpener::handle);

        // ────────────────────────────────────────────────
        // 🧭 Client → Server
        // ────────────────────────────────────────────────
        registrar.playToServer(
                MenuSlotUpdatePacket.TYPE,
                MenuSlotUpdatePacket.STREAM_CODEC,
                MenuSlotUpdatePacket::handle
        );

        CatocraftMod.LOGGER.info("[NetworkHandler] Registered dynamic menu payloads");
    }

    /** Utility for sending payloads to a specific player. */
    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}