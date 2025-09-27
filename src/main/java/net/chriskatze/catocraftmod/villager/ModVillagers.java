package net.chriskatze.catocraftmod.villager;

import com.google.common.collect.ImmutableSet;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.sound.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(BuiltInRegistries.POINT_OF_INTEREST_TYPE, CatocraftMod.MOD_ID);

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(BuiltInRegistries.VILLAGER_PROFESSION, CatocraftMod.MOD_ID);

    public static final Holder<PoiType> FROST_MAGE_VILLAGER_POI = POI_TYPES.register("frost_mage_villager_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.STEEL_BLOCK.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final Holder<VillagerProfession> FROST_MAGE_VILLAGER = VILLAGER_PROFESSIONS.register("frost_mage_villager",
            () -> new VillagerProfession("frost_mage_villager", holder -> holder.value() == FROST_MAGE_VILLAGER_POI.value(),
                    poiTypeHolder -> poiTypeHolder.value() == FROST_MAGE_VILLAGER_POI.value(), ImmutableSet.of(), ImmutableSet.of(),
                    ModSounds.FROST_MAGE_VILLAGER_ACQUIRE_POI.get()));


    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}
