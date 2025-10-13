package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.client.render.OreGlowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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

public record OreSenseGlowPacket(List<BlockPos> positions, int durationTicks) implements CustomPacketPayload {

    public static final Type<OreSenseGlowPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "ore_sense_glow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreSenseGlowPacket> STREAM_CODEC =
            StreamCodec.of(OreSenseGlowPacket::encode, OreSenseGlowPacket::decode);

    public static void encode(RegistryFriendlyByteBuf buf, OreSenseGlowPacket packet) {
        buf.writeInt(packet.positions.size());
        for (BlockPos pos : packet.positions) {
            buf.writeLong(pos.asLong());
        }
        buf.writeInt(packet.durationTicks);
    }

    public static OreSenseGlowPacket decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readInt();
        List<BlockPos> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(BlockPos.of(buf.readLong()));
        }
        int duration = buf.readInt();
        return new OreSenseGlowPacket(list, duration);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // --- FIXED handle method ---
    public static void handle(OreSenseGlowPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                if (packet.durationTicks() <= 0) {
                    // Remove glow immediately
                    OreGlowRenderer.removeGlowingOres(packet.positions());
                } else {
                    // Add glow normally
                    OreGlowRenderer.addGlowingOres(packet.positions(), packet.durationTicks());
                }
            }
        });
    }

    public static void sendToClient(ServerLevel level, ServerPlayer player, List<BlockPos> positions, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new OreSenseGlowPacket(positions, durationTicks));
    }
}