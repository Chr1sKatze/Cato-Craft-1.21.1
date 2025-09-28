package net.chriskatze.catocraftmod.item;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CatocraftMod.MOD_ID);

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
    public static final Supplier<Item> GATHERING_SPEED_BOOK = ITEMS.register("gathering_speed_book",
            () -> new EnchantedBookItem(new Item.Properties().stacksTo(1)));

    // Map books → enchantments for dynamic anvil handler
    private static final Map<Item, ResourceKey<?>> BOOK_TO_ENCHANTMENT = new HashMap<>();

    static {
        BOOK_TO_ENCHANTMENT.put(GATHERING_SPEED_BOOK.get(), ModEnchantments.GATHERING_SPEED.getKey());
        // Add more custom books here:
        // BOOK_TO_ENCHANTMENT.put(OTHER_BOOK.get(), ModEnchantments.OTHER_CUSTOM.getKey());
    }

    /**
     * Returns the ResourceKey<Enchantment> corresponding to a given custom book.
     * Returns null if the book is not a registered custom enchantment book.
     */
    @SuppressWarnings("unchecked")
    public static ResourceKey<net.minecraft.world.item.enchantment.Enchantment> getEnchantmentKeyFromBook(Item bookItem) {
        ResourceKey<?> key = BOOK_TO_ENCHANTMENT.get(bookItem);
        return (ResourceKey<net.minecraft.world.item.enchantment.Enchantment>) key; // safe cast
    }


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
