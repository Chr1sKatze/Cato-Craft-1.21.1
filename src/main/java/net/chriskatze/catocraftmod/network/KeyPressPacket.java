package net.chriskatze.catocraftmod.network;

import com.mojang.blaze3d.platform.InputConstants;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.mobs.NightShade;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KeyPressPacket(String key) implements CustomPacketPayload {

    public static final Type<KeyPressPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "key_press"));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyPressPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, KeyPressPacket::key,
                    KeyPressPacket::new
            );

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Run on server thread
            var player = ctx.player();
            if (player != null) {
                NightShade.spawnExample(player);
            }
        });
    }

    public static void sendToServer(KeyMapping key) {
        PacketDistributor.sendToServer(new KeyPressPacket(key.getName()));
    }
}
