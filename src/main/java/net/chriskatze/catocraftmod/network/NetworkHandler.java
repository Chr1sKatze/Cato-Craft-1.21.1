package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Handles network registration for the Catocraft mod.
 *
 * Responsible for defining unique packet IDs and registering
 * the packet handlers that sync server-to-client data.
 */
public class NetworkHandler {

    // ---------------------------------------------------------------------
    // Packet Identifiers
    // ---------------------------------------------------------------------

    /**
     * Unique ResourceLocation for the Revelation Glow packet.
     * Used to identify the packet type when sending data from server to client.
     */
    public static final ResourceLocation REVELATION_GLOW_ID = ResourceLocation.fromNamespaceAndPath(
            CatocraftMod.MOD_ID, "revelation_glow"
    );

    // ---------------------------------------------------------------------
    // Packet Registration
    // ---------------------------------------------------------------------

    /**
     * Registers all custom network packet handlers.
     *
     * @param event Event provided by NeoForge to register payload handlers
     */
    public static void register(final RegisterPayloadHandlersEvent event) {
        /*
         * Create a registrar for this mod's packets:
         * - versioned("1") indicates the protocol version of this packet handler.
         *   This allows future updates to maintain backward compatibility.
         * - optional() means that the registration will not fail if the client
         *   does not have this mod installed (useful for optional mods or cross-play).
         */
        var registrar = event.registrar(CatocraftMod.MOD_ID).versioned("1").optional();

        // Register OreSenseGlowPacket for sending from server to client
        registrar.playToClient(
                RevelationGlowPacket.TYPE,    // Packet type
                RevelationGlowPacket.STREAM_CODEC, // Serialization codec
                RevelationGlowPacket::handle // Client-side handler method
        );
    }
}