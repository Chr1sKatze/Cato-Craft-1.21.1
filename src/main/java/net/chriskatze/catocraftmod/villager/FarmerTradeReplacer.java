package net.chriskatze.catocraftmod.villager;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.block.ModBlocks;
import net.chriskatze.catocraftmod.item.ModItems;
import net.minecraft.world.entity.npc.VillagerProfession;
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
    public class FarmerTradeReplacer {

        @SubscribeEvent
        public static void addFarmerTrades(VillagerTradesEvent event) {
            if (event.getType() != VillagerProfession.FARMER) return;

            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();

            // Clear existing trades (optional)
            trades.values().forEach(List::clear);

            // Level 1 trades
            trades.get(1).add((entity, random) -> new MerchantOffer(
                    new ItemCost(ModItems.RAW_STEEL, 5),
                    new ItemStack(Items.CARROT, 10),
                    12, 5, 0.05f
            ));

            trades.get(1).add((entity, random) -> new MerchantOffer(
                    new ItemCost(Items.WHEAT, 20),
                    new ItemStack(ModItems.STEEL_NUGGET.get(), 3),
                    10, 2, 0.05f
            ));

            // Level 2 trades
            trades.get(2).add((entity, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 3),
                    new ItemStack(ModItems.STEEL_INGOT.get(), 1),
                    10, 3, 0.05f
            ));

            trades.get(2).add((entity, random) -> new MerchantOffer(
                    new ItemCost(ModItems.RAW_PLATINUM, 2),
                    new ItemStack(Items.POTATO, 15),
                    8, 2, 0.05f
            ));

            // Level 3 trades
            trades.get(3).add((entity, random) -> new MerchantOffer(
                    new ItemCost(ModItems.STEEL_INGOT, 2),
                    new ItemStack(ModItems.RAW_PLATINUM.get(), 2),
                    6, 3, 0.05f
            ));

            trades.get(3).add((entity, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 5),
                    new ItemStack(ModBlocks.STEEL_BLOCK.get(), 1),
                    5, 5, 0.05f
            ));
        }
    }