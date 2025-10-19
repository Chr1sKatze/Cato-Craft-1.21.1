package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Handles (de)serialization of EquipmentGroup enum for packets.
 */
public class EquipmentGroupStreamCodec implements StreamCodec<FriendlyByteBuf, EquipmentGroup> {
    public static final EquipmentGroupStreamCodec INSTANCE = new EquipmentGroupStreamCodec();

    @Override
    public EquipmentGroup decode(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        return EquipmentGroup.fromKey(key);
    }

    @Override
    public void encode(FriendlyByteBuf buf, EquipmentGroup group) {
        buf.writeUtf(group.getKey());
    }
}