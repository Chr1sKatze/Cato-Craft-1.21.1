package net.chriskatze.catocraftmod.enchantment.custom;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.UUID;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class BlockDropHandler {

    private static final TagKey<Item> GATHERING_TOOLS_TAG =
            TagKey.create(Registries.ITEM, CatocraftMod.id("gathering_tools"));

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getLevel().isClientSide()) return; // Only server side

        var breaker = event.getBreaker();
        if (!(breaker instanceof Player player)) return;

        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !tool.is(GATHERING_TOOLS_TAG)) {
            CatocraftMod.LOGGER.debug("[BlockDropHandler] Player {} does not hold a gathering tool", player.getName().getString());
            return;
        }

        UUID playerUUID = player.getUUID();
        var drops = event.getDrops();

        for (ItemEntity drop : drops) {
            drop.getPersistentData().putUUID("CatocraftMiner", playerUUID);
        }

        CatocraftMod.LOGGER.info("[BlockDropHandler] Tagged {} items for player {}", drops.size(), player.getName().getString());
    }
}