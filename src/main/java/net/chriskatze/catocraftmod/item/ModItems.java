package net.chriskatze.catocraftmod.item;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CatocraftMod.MOD_ID);

    // ---------------- Standard items ----------------
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

    // ---------------- Crystal Items ----------------
    public static final DeferredItem<Item> GATHERING_CRYSTAL = ITEMS.register("gathering_crystal",
            () -> new CrystalItem(1, ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":gathering_speed"),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> REINFORCEMENT_CRYSTAL = ITEMS.register("reinforcement_crystal",
            () -> new CrystalItem(1, ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":reinforcement"),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> PROSPERITY_CRYSTAL = ITEMS.register("prosperity_crystal",
            () -> new CrystalItem(1, ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":prosperity"),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> ATTRACTION_CRYSTAL = ITEMS.register("attraction_crystal",
            () -> new CrystalItem(1, ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":attraction"),
                    new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> ORE_SENSE_CRYSTAL = ITEMS.register("ore_sense_crystal",
            () -> new CrystalItem(1, ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":ore_sense"),
                    new Item.Properties().stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}