package net.chriskatze.catocraftmod.item;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
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

    public static Holder<Enchantment> getEnchantmentHolderFromBook(ItemStack bookStack, Level world) {
        // Get all enchantments on the book
        var enchants = bookStack.getEnchantments();

        // Try to get a Holder<Enchantment> for our gathering_speed enchantment
        Holder<Enchantment> gatheringHolder = world.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ModEnchantments.GATHERING_SPEED.getKey())
                .orElse(null);

        if (gatheringHolder == null) return null;

        // If the book contains the gathering_speed enchantment, return the holder
        if (enchants.getLevel(gatheringHolder) > 0) {
            return gatheringHolder;
        }

        return null;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}