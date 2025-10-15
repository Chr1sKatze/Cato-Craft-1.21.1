package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EarringCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEarringCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EarringSyncPacket(CompoundTag tag) implements CustomPacketPayload {
    public static final Type<EarringSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "earring_sync"));

    public static final StreamCodec<FriendlyByteBuf, EarringSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, EarringSyncPacket::tag,
                    EarringSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EarringSyncPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            CompoundTag nbt = msg.tag() == null ? new CompoundTag() : msg.tag();

            var cap = mc.player.getCapability(EarringCapabilityHandler.EARRING_CAP);
            if (cap instanceof PlayerEarringCapability handler) {
                // ✅ Use safe method to apply server sync (prevents feedback loop)
                handler.applySyncFromServer(nbt, mc.player.registryAccess());
                CatocraftMod.LOGGER.debug("[EarringSyncPacket] Client applied synced NBT safely: {}", nbt);

                // Optional visual refresh
                var synced = handler.getStackInSlot(
                        net.chriskatze.catocraftmod.menu.EarringMenu.EARRING_SLOT_INDEX
                );

                if (mc.player.containerMenu instanceof net.chriskatze.catocraftmod.menu.EarringMenu menu) {
                    menu.getSlot(net.chriskatze.catocraftmod.menu.EarringMenu.EARRING_SLOT_INDEX)
                            .set(synced.copy());
                    CatocraftMod.LOGGER.debug("[EarringSyncPacket] Refreshed EarringMenu slot with {}", synced);
                } else {
                    mc.player.inventoryMenu.broadcastChanges();
                }
            }
        });
    }
}