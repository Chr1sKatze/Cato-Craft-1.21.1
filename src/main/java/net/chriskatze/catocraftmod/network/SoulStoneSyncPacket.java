package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.PlayerSoulStoneCapability;
import net.chriskatze.catocraftmod.capability.SoulStoneCapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SoulStoneSyncPacket(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SoulStoneSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "soulstone_sync"));

    public static final StreamCodec<FriendlyByteBuf, SoulStoneSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, SoulStoneSyncPacket::tag,
                    SoulStoneSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoulStoneSyncPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            CompoundTag nbt = msg.tag() == null ? new CompoundTag() : msg.tag();

            var cap = mc.player.getCapability(SoulStoneCapabilityHandler.SOULSTONE_CAP);
            if (cap instanceof PlayerSoulStoneCapability handler) {
                // ✅ Safe apply from server
                handler.applySyncFromServer(nbt, mc.player.registryAccess());
                CatocraftMod.LOGGER.debug("[SoulStoneSyncPacket] Client applied synced NBT safely: {}", nbt);

                // Optional visual update for the menu
                var synced = handler.getStackInSlot(
                        net.chriskatze.catocraftmod.menu.EarringMenu.SOULSTONE_SLOT_INDEX
                );

                if (mc.player.containerMenu instanceof net.chriskatze.catocraftmod.menu.EarringMenu menu) {
                    int soulSlot = net.chriskatze.catocraftmod.menu.EarringMenu.getSoulStoneSlotIndex();
                    if (soulSlot >= 0 && soulSlot < menu.slots.size()) {
                        menu.getSlot(soulSlot).set(synced.copy());
                        menu.getSlot(soulSlot).setChanged();
                        menu.broadcastChanges();
                        CatocraftMod.LOGGER.debug("[SoulStoneSyncPacket] Updated Soul Stone slot {}", synced);
                    }
                } else {
                    mc.player.inventoryMenu.broadcastChanges();
                }
            }
        });
    }
}