package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class NetworkHandler {

    public static final ResourceLocation ORE_SENSE_GLOW_ID = ResourceLocation.fromNamespaceAndPath(
            CatocraftMod.MOD_ID, "ore_sense_glow"
    );

    public static void register(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(CatocraftMod.MOD_ID).versioned("1").optional();

        registrar.playToClient(
                OreSenseGlowPacket.TYPE,
                OreSenseGlowPacket.STREAM_CODEC,
                OreSenseGlowPacket::handle
        );
    }
}