package net.chriskatze.catocraftmod.enchantment;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.custom.ModEnchantmentEntry;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Registers and manages all custom enchantments for the Catocraft mod.
 * Includes safe client-side lazy initialization for menus and tooltips.
 */
public class ModEnchantments {

    // ---------------------------------------------------------------------
    // Enchantment definitions
    // ---------------------------------------------------------------------
    public static final ModEnchantmentEntry REINFORCEMENT = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("reinforcement")), 12);

    public static final ModEnchantmentEntry GATHERING = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("gathering")), 12);

    public static final ModEnchantmentEntry PROSPERITY = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("prosperity")), 12);

    public static final ModEnchantmentEntry ATTRACTION = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("attraction")), 12);

    public static final ModEnchantmentEntry REVELATION = new ModEnchantmentEntry(
            ResourceKey.create(Registries.ENCHANTMENT, CatocraftMod.id("revelation")), 12);

    // ---------------------------------------------------------------------
    // Bootstrap method (for Datapack registration)
    // ---------------------------------------------------------------------
    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        ModTags.initHolderSets(items); // ensure tag-based HolderSets are ready

        HolderSet<Item> revelationItems = ModTags.REVELATION_ITEMS_HOLDER;
        HolderSet<Item> reinforcementItems = ModTags.REINFORCEMENT_ITEMS_HOLDER;
        HolderSet<Item> gatheringItems = ModTags.GATHERING_ITEMS_HOLDER;
        HolderSet<Item> prosperityItems = ModTags.PROSPERITY_ITEMS_HOLDER;
        HolderSet<Item> attractionItems = ModTags.ATTRACTION_ITEMS_HOLDER;

        registerEnchant(context, REVELATION, revelationItems);
        registerEnchant(context, GATHERING, gatheringItems);
        registerEnchant(context, REINFORCEMENT, reinforcementItems);
        registerEnchant(context, PROSPERITY, prosperityItems);
        registerEnchant(context, ATTRACTION, attractionItems);
    }

    private static void registerEnchant(BootstrapContext<Enchantment> context,
                                        ModEnchantmentEntry entry,
                                        HolderSet<Item> allowedItems) {
        Enchantment.Builder builder = Enchantment.enchantment(
                Enchantment.definition(
                        allowedItems,
                        allowedItems,
                        entry.getMaxLevel(),
                        1,
                        Enchantment.dynamicCost(9999, 0),
                        Enchantment.dynamicCost(9999, 0),
                        1,
                        EquipmentSlotGroup.MAINHAND
                ));

        context.register(entry.getKey(), builder.build(entry.getKey().location()));

        entry.setHolder(context.lookup(Registries.ENCHANTMENT).getOrThrow(entry.getKey()));
    }

    // ---------------------------------------------------------------------
    // Runtime Holder Initialization (for server startup)
    // ---------------------------------------------------------------------
    public static void initHolders(HolderGetter<Enchantment> enchants) {
        initHolder(enchants, REINFORCEMENT, "reinforcement");
        initHolder(enchants, GATHERING, "gathering");
        initHolder(enchants, PROSPERITY, "prosperity");
        initHolder(enchants, ATTRACTION, "attraction");
        initHolder(enchants, REVELATION, "revelation");
    }

    private static void initHolder(HolderGetter<Enchantment> enchants,
                                   ModEnchantmentEntry entry,
                                   String logName) {
        entry.setHolder(
                enchants.get(entry.getKey()).orElseGet(() -> {
                    CatocraftMod.LOGGER.warn("[ModEnchantments] ⚠ Missing enchantment: {}", logName);
                    return null;
                })
        );
    }

    // ---------------------------------------------------------------------
    // Client-safe Lazy Initialization
    // ---------------------------------------------------------------------
    private static boolean tryLazyInit() {
        // Only attempt this on the physical client
        if (!FMLLoader.getDist().isClient()) return false;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                HolderLookup.RegistryLookup<Enchantment> lookup =
                        mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                initHolders(lookup);
                CatocraftMod.LOGGER.info("[Catocraft] Lazy-initialized ModEnchantments via client registry access.");
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // ---------------------------------------------------------------------
    // Safe Accessors
    // ---------------------------------------------------------------------
    private static ModEnchantmentEntry safeGet(ModEnchantmentEntry entry, String name) {
        if (!entry.hasHolder()) {
            CatocraftMod.LOGGER.warn("[Catocraft] {} accessed before initialization!", name);
            tryLazyInit();
        }
        return entry;
    }

    public static ModEnchantmentEntry getReinforcement() { return safeGet(REINFORCEMENT, "REINFORCEMENT"); }
    public static ModEnchantmentEntry getGathering()     { return safeGet(GATHERING, "GATHERING"); }
    public static ModEnchantmentEntry getProsperity()    { return safeGet(PROSPERITY, "PROSPERITY"); }
    public static ModEnchantmentEntry getAttraction()    { return safeGet(ATTRACTION, "ATTRACTION"); }
    public static ModEnchantmentEntry getRevelation()    { return safeGet(REVELATION, "REVELATION"); }
}