package net.chriskatze.catocraftmod.event;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.item.ModItems;
import net.chriskatze.catocraftmod.villager.ModVillagers;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModEvents {

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if(event.getType() == ModVillagers.FROST_MAGE_VILLAGER.value()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            trades.get(1).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.RAW_STEEL, 1),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 1), 60, 40, 0.05f));

            trades.get(1).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.STEEL_INGOT, 1),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(1).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModItems.STEEL_NUGGET, 1),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(1).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(ModBlocks.STEEL_BLOCK, 1),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 1), 60, 30, 0.05f));

            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.DIAMOND, 2),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.LAPIS_LAZULI, 2),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(2).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.BOOK, 2),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 1), 60, 30, 0.05f));

            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.LEATHER, 2),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.IRON_AXE, 3),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.IRON_PICKAXE, 3),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(3).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.IRON_SHOVEL, 3),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));

            trades.get(4).add((entity, randomSource) -> new MerchantOffer(
                    new ItemCost(Items.IRON_SWORD, 3),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 18), 6, 3, 0.05f));
        }
    }
}