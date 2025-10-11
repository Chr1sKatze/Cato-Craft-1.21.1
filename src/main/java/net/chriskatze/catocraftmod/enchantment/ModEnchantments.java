package net.chriskatze.catocraftmod.enchantment;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.custom.ModEnchantmentEntry;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ModEnchantmentEntry REINFORCEMENT = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("reinforcement")), 12);

    public static final ModEnchantmentEntry GATHERING_SPEED = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("gathering_speed")), 12);

    public static final ModEnchantmentEntry PROSPERITY = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("prosperity")), 12);

    public static final ModEnchantmentEntry ATTRACTION = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("attraction")), 12);

    public static final ModEnchantmentEntry ORE_SENSE = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("ore_sense")), 12);

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        // Lookup all items from the registry
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        // Initialize all HolderSets from tags (tools, swords, combined sets)
        ModTags.initHolderSets(items);

        // Now the HolderSets exist and can be used safely
        HolderSet<Item> ore_SenseItems = ModTags.ORE_SENSE_ITEMS_HOLDER;
        HolderSet<Item> reinforcementItems = ModTags.REINFORCEMENT_ITEMS_HOLDER;
        HolderSet<Item> gatheringSpeedItems = ModTags.GATHERING_SPEED_ITEMS_HOLDER;
        HolderSet<Item> prosperityItems = ModTags.PROSPERITY_ITEMS_HOLDER;
        HolderSet<Item> attractionItems = ModTags.ATTRACTION_ITEMS_HOLDER;

        // Register enchantments
        registerEnchant(context, ORE_SENSE, ore_SenseItems);
        registerEnchant(context, GATHERING_SPEED, gatheringSpeedItems);
        registerEnchant(context, REINFORCEMENT, reinforcementItems);
        registerEnchant(context, PROSPERITY, prosperityItems);
        registerEnchant(context, ATTRACTION, attractionItems);
    }

    private static void registerEnchant(BootstrapContext<Enchantment> context,
                                        ModEnchantmentEntry entry,
                                        HolderSet<Item> allowedItems) {

        Enchantment.Builder builder = Enchantment.enchantment(
                Enchantment.definition(
                        allowedItems,       // items that can receive the enchantment
                        allowedItems,       // items affected by the enchantment
                        entry.getMaxLevel(),
                        1,                  // rarity weight
                        Enchantment.dynamicCost(9999, 0),
                        Enchantment.dynamicCost(9999, 0),
                        1,                  // minimum level cost multiplier
                        EquipmentSlotGroup.MAINHAND
                ));

        context.register(entry.getKey(), builder.build(entry.getKey().location()));

        // Cache the Holder for later use in DropHandler / mixins
        entry.setHolder(context.lookup(Registries.ENCHANTMENT).getOrThrow(entry.getKey()));
    }
}