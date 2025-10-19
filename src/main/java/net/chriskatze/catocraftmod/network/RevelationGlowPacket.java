package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.render.OreGlowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom network packet for sending Revelation glow data from the server to the client.
 *
 * This packet contains a list of block positions that should glow and the duration
 * of the glow effect. It handles encoding, decoding, and applying the glow on the client.
 */
public record RevelationGlowPacket(List<BlockPos> positions, int durationTicks) implements CustomPacketPayload {

    // ---------------------------------------------------------------------
    // Packet Type & Codec
    // ---------------------------------------------------------------------

    /** Unique identifier for this custom packet type */
    public static final Type<RevelationGlowPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "revelation_glow"));

    /** Stream codec to serialize/deserialize the packet */
    public static final StreamCodec<RegistryFriendlyByteBuf, RevelationGlowPacket> STREAM_CODEC =
            StreamCodec.of(RevelationGlowPacket::encode, RevelationGlowPacket::decode);

    // ---------------------------------------------------------------------
    // Encoding & Decoding
    // ---------------------------------------------------------------------

    /**
     * Serializes the packet into the network buffer.
     * - Writes the number of positions
     * - Writes each block position as a long
     * - Writes the duration of the glow
     */
    public static void encode(RegistryFriendlyByteBuf buf, RevelationGlowPacket packet) {
        buf.writeInt(packet.positions.size());
        for (BlockPos pos : packet.positions) {
            buf.writeLong(pos.asLong());
        }
        buf.writeInt(packet.durationTicks);
    }

    /**
     * Deserializes the packet from the network buffer.
     * - Reads the number of positions
     * - Reads each block position
     * - Reads the duration of the glow
     */
    public static RevelationGlowPacket decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readInt();
        List<BlockPos> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(BlockPos.of(buf.readLong()));
        }
        int duration = buf.readInt();
        return new RevelationGlowPacket(list, duration);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ---------------------------------------------------------------------
    // Packet Handling (Client Side)
    // ---------------------------------------------------------------------

    /**
     * Handles the packet on the client.
     * Schedules the glow addition or removal on the main thread.
     */
    public static void handle(RevelationGlowPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                if (packet.durationTicks() <= 0) {
                    // Remove glow immediately if duration is zero or negative
                    OreGlowRenderer.removeGlowingOres(packet.positions());
                } else {
                    // Add glow normally
                    OreGlowRenderer.addGlowingOres(packet.positions(), packet.durationTicks());
                }
            }
        });
    }

    // ---------------------------------------------------------------------
    // Convenience Method for Server to Send Packet
    // ---------------------------------------------------------------------

    /**
     * Sends this packet to a specific client.
     *
     * @param level         The server level (dimension)
     * @param player        The player to send the packet to
     * @param positions     The list of blocks that should glow
     * @param durationTicks The duration of the glow in ticks
     */
    public static void sendToClient(ServerLevel level, ServerPlayer player, List<BlockPos> positions, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new RevelationGlowPacket(positions, durationTicks));
    }
}