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

    // Custom enchantment with its own max level
    public static final ModEnchantmentEntry GATHERING_SPEED =
            new ModEnchantmentEntry(
                    ResourceKey.create(Registries.ENCHANTMENT,
                            ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "gathering_speed")),
                    5 // max level
            );

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        // Reference to your custom item tag
        var gatheringToolRef = items.getOrThrow(
                ResourceKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "gathering_tools"))
        );

        // Wrap the reference in a HolderSet<Item>
        HolderSet<Item> gatheringTools = HolderSet.direct(gatheringToolRef);

        // Register the enchantment
        register(context, GATHERING_SPEED.getKey(), Enchantment.enchantment(Enchantment.definition(
                gatheringTools,                // primary items
                gatheringTools,                // secondary items
                GATHERING_SPEED.getMaxLevel(), // max level
                1,                             // rarity/cost
                Enchantment.dynamicCost(9999, 0), // min cost (disabled)
                Enchantment.dynamicCost(9999, 0), // max cost (disabled)
                1,                             // weight
                EquipmentSlotGroup.MAINHAND
        )));
    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }
}