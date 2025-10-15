package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.EarringMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Modern NeoForge 21.1+ packet: client → server request to open the EarringMenu.
 */
public record OpenEarringMenuPacket() implements CustomPacketPayload {

    // ✅ Correct ResourceLocation creation
    public static final Type<OpenEarringMenuPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "open_earring_menu"));

    // ✅ Use StreamCodec.of for encode/decode (no data needed here)
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEarringMenuPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {}, buf -> new OpenEarringMenuPacket());

    @Override
    public Type<OpenEarringMenuPacket> type() {
        return TYPE;
    }

    // ✅ Server-side handler
    public static void handle(OpenEarringMenuPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new EarringMenu(id, inv, p),
                        Component.translatable("container.catocraftmod.earring_inventory")
                ));
            }
        });
    }
}