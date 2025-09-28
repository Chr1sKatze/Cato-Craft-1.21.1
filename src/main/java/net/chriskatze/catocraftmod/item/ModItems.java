package net.chriskatze.catocraftmod.item;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CatocraftMod.MOD_ID);

    // Standard items
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_NUGGET = ITEMS.register("steel_nugget",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_STEEL = ITEMS.register("raw_steel",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLATINUM_INGOT = ITEMS.register("platinum_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLATINUM_NUGGET = ITEMS.register("platinum_nugget",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_PLATINUM = ITEMS.register("raw_platinum",
            () -> new Item(new Item.Properties()));
    // Custom enchanted book
    public static final DeferredItem<Item> GATHERING_SPEED_BOOK = ITEMS.register("gathering_speed_book",
            () -> new EnchantedBookItem(new Item.Properties().stacksTo(1)));

    /**
     * Returns the enchantment key for a given custom book item.
     * Safe to call at runtime (after registries are bound).
     */
    public static ResourceKey<net.minecraft.world.item.enchantment.Enchantment>
    getEnchantmentKeyFromBook(Item bookItem) {
        if (bookItem == GATHERING_SPEED_BOOK.get()) {
            return ModEnchantments.GATHERING_SPEED.getKey();
        }
        return null;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}