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
                            ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":gathering_speed")),
                    12
            );

    public static final ModEnchantmentEntry REINFORCEMENT =
            new ModEnchantmentEntry(
                    ResourceKey.create(Registries.ENCHANTMENT,
                            ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":reinforcement")),
                    12
            );

    public static final ModEnchantmentEntry PROSPERITY =
            new ModEnchantmentEntry(
                    ResourceKey.create(Registries.ENCHANTMENT,
                            ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":prosperity")),
                    12
            );

    public static final ModEnchantmentEntry ATTRACTION =
            new ModEnchantmentEntry(
                    ResourceKey.create(Registries.ENCHANTMENT,
                            ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":attraction")),
                    12
            );

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        var gatheringToolRef = items.getOrThrow(
                ResourceKey.create(Registries.ITEM,
                        ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":gathering_tools"))
        );

        HolderSet<Item> gatheringTools = HolderSet.direct(gatheringToolRef);

        // --- GATHERING SPEED ---
        Enchantment.Builder gatheringBuilder = Enchantment.enchantment(Enchantment.definition(
                gatheringTools,
                gatheringTools,
                GATHERING_SPEED.getMaxLevel(),
                1,
                Enchantment.dynamicCost(9999, 0),
                Enchantment.dynamicCost(9999, 0),
                1,
                EquipmentSlotGroup.MAINHAND
        ));

        // Register and build the actual enchantment
        register(context, GATHERING_SPEED.getKey(), gatheringBuilder);

        // Retrieve the Holder<Enchantment> for later use in the mixin
        GATHERING_SPEED.setHolder(context.lookup(Registries.ENCHANTMENT).getOrThrow(GATHERING_SPEED.getKey()));

        // --- REINFORCEMENT ---
        Enchantment.Builder reinforcementBuilder = Enchantment.enchantment(Enchantment.definition(
                gatheringTools,
                gatheringTools,
                REINFORCEMENT.getMaxLevel(),
                1,
                Enchantment.dynamicCost(9999, 0),
                Enchantment.dynamicCost(9999, 0),
                1,
                EquipmentSlotGroup.MAINHAND
        ));

        // Register and build the actual enchantment
        register(context, REINFORCEMENT.getKey(), reinforcementBuilder);

        // Retrieve the Holder<Enchantment> for later use in the mixin
        REINFORCEMENT.setHolder(context.lookup(Registries.ENCHANTMENT).getOrThrow(REINFORCEMENT.getKey()));

        // --- PROSPERITY ---
        Enchantment.Builder prosperityBuilder = Enchantment.enchantment(Enchantment.definition(
                gatheringTools,
                gatheringTools,
                PROSPERITY.getMaxLevel(),
                1,
                Enchantment.dynamicCost(9999, 0),
                Enchantment.dynamicCost(9999, 0),
                1,
                EquipmentSlotGroup.MAINHAND
        ));

        // Register and build the actual enchantment
        register(context, PROSPERITY.getKey(), prosperityBuilder);

        // Retrieve the Holder<Enchantment> for later use in the mixin
        PROSPERITY.setHolder(context.lookup(Registries.ENCHANTMENT).getOrThrow(PROSPERITY.getKey()));

        // --- ATTRACTION ---
        Enchantment.Builder attractionBuilder = Enchantment.enchantment(Enchantment.definition(
                gatheringTools, // you can reuse your existing tool tag
                gatheringTools,
                ATTRACTION.getMaxLevel(),
                1,
                Enchantment.dynamicCost(9999, 0),
                Enchantment.dynamicCost(9999, 0),
                1,
                EquipmentSlotGroup.MAINHAND
        ));

        // Register and build the actual enchantment
        register(context, ATTRACTION.getKey(), attractionBuilder);

        // Retrieve the Holder<Enchantment> for later use in the mixin
        ATTRACTION.setHolder(context.lookup(Registries.ENCHANTMENT).getOrThrow(ATTRACTION.getKey()));
    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key,
                                 Enchantment.Builder builder) {
        registry.register(key, builder.build(key.location()));
    }
}