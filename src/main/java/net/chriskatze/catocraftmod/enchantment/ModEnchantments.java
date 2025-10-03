package net.chriskatze.catocraftmod.enchantment;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.custom.ModEnchantmentEntry;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {

    // Custom enchantment entry
    public static final ModEnchantmentEntry GATHERING_SPEED =
            new ModEnchantmentEntry(
                    ResourceKey.create(Registries.ENCHANTMENT,
                            ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "gathering_speed")),
                    5
            );

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        var gatheringToolRef = items.getOrThrow(
                ResourceKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "gathering_tools"))
        );

        HolderSet<Item> gatheringTools = HolderSet.direct(gatheringToolRef);

        // Register the enchantment
        register(context, GATHERING_SPEED.getKey(), Enchantment.enchantment(Enchantment.definition(
                gatheringTools,
                gatheringTools,
                GATHERING_SPEED.getMaxLevel(),
                1,
                Enchantment.dynamicCost(9999, 0),
                Enchantment.dynamicCost(9999, 0),
                1,
                EquipmentSlotGroup.MAINHAND
        )));
    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }
}