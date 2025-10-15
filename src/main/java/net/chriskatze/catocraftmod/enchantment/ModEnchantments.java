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

/**
 * Registers all custom enchantments for the Catocraft mod.
 *
 * Each enchantment is associated with a maximum level and
 * a set of items it can be applied to.
 */
public class ModEnchantments {

    // ---------------------------------------------------------------------
    // Enchantment definitions
    // ---------------------------------------------------------------------

    /** Reinforcement enchantment increases tool durability */
    public static final ModEnchantmentEntry REINFORCEMENT = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("reinforcement")), 12);

    /** Gathering Speed enchantment increases mining or gathering speed */
    public static final ModEnchantmentEntry GATHERING = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("gathering")), 12);

    /** Prosperity enchantment affects loot quality or quantity */
    public static final ModEnchantmentEntry PROSPERITY = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("prosperity")), 12);

    /** Attraction enchantment attracts nearby items or entities */
    public static final ModEnchantmentEntry ATTRACTION = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("attraction")), 12);

    /** Revelation enchantment highlights ores around the player */
    public static final ModEnchantmentEntry REVELATION = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("revelation")), 12);

    // ---------------------------------------------------------------------
    // Bootstrap method
    // ---------------------------------------------------------------------

    /**
     * Initializes and registers all enchantments.
     *
     * @param context The bootstrap context used to register enchantments.
     */
    public static void bootstrap(BootstrapContext<Enchantment> context) {

        // Lookup all items from the registry
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        // Initialize all HolderSets from tags (tools, swords, etc.)
        ModTags.initHolderSets(items);

        // Retrieve HolderSets for each enchantment
        HolderSet<Item> revelationItems = ModTags.REVELATION_ITEMS_HOLDER;
        HolderSet<Item> reinforcementItems = ModTags.REINFORCEMENT_ITEMS_HOLDER;
        HolderSet<Item> gatheringItems = ModTags.GATHERING_ITEMS_HOLDER;
        HolderSet<Item> prosperityItems = ModTags.PROSPERITY_ITEMS_HOLDER;
        HolderSet<Item> attractionItems = ModTags.ATTRACTION_ITEMS_HOLDER;

        // Register each enchantment with its allowed items
        registerEnchant(context, REVELATION, revelationItems);
        registerEnchant(context, GATHERING, gatheringItems);
        registerEnchant(context, REINFORCEMENT, reinforcementItems);
        registerEnchant(context, PROSPERITY, prosperityItems);
        registerEnchant(context, ATTRACTION, attractionItems);
    }

    // ---------------------------------------------------------------------
    // Helper method to register a single enchantment
    // ---------------------------------------------------------------------

    /**
     * Registers a single enchantment in the context.
     *
     * @param context      The bootstrap context.
     * @param entry        The custom enchantment entry.
     * @param allowedItems The set of items this enchantment can be applied to.
     */
    private static void registerEnchant(BootstrapContext<Enchantment> context,
                                        ModEnchantmentEntry entry,
                                        HolderSet<Item> allowedItems) {

        // Build the enchantment definition
        Enchantment.Builder builder = Enchantment.enchantment(
                Enchantment.definition(
                        allowedItems,       // items that can receive the enchantment
                        allowedItems,       // items affected by the enchantment
                        entry.getMaxLevel(),// max level
                        1,                  // rarity weight
                        Enchantment.dynamicCost(9999, 0), // min cost function
                        Enchantment.dynamicCost(9999, 0), // max cost function
                        1,                  // minimum level cost multiplier
                        EquipmentSlotGroup.MAINHAND      // applicable equipment slots
                ));

        // Register enchantment in the registry
        context.register(entry.getKey(), builder.build(entry.getKey().location()));

        // Cache the Holder for later use in drop handlers or mixins
        entry.setHolder(context.lookup(Registries.ENCHANTMENT).getOrThrow(entry.getKey()));
    }
}