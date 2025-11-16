package net.chriskatze.catocraftmod.mobs;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class ModEntitiesEvents {

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.ENTITY_TYPE,helper -> helper.register(NightShade.LOCATION, NightShade.entityType()));
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        NightShade.registerSpawnPlacement(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        NightShade.registerRenderer(event);
    }
}