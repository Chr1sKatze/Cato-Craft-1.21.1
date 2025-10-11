package net.chriskatze.catocraftmod.enchantment.custom;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Collection;
import java.util.UUID;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class BlockDropHandler {

    // -------------------
    // Block drops
    // -------------------
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        CatocraftMod.LOGGER.info("[Catocraft] BlockDropsEvent at {}", event.getPos());

        var breaker = event.getBreaker();
        if (!(breaker instanceof Player player)) {
            CatocraftMod.LOGGER.info("[Catocraft] Broken by non-player, skipping.");
            return;
        }

        ItemStack tool = event.getTool();
        CatocraftMod.LOGGER.info("[Catocraft] Player {} used tool {}", player.getName().getString(), tool.getItem());
        if (tool.isEmpty()) return;

        applyProsperity(event.getDrops(), tool, player, level);
        applyAttraction(event.getDrops(), tool, player, level);
    }

    // -------------------
    // Mob drops
    // -------------------
    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.getCommandSenderWorld();
        if (level.isClientSide()) return;

        CatocraftMod.LOGGER.info("[Catocraft] LivingDropsEvent for {}", entity.getName().getString());

        var sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof Player player)) {
            CatocraftMod.LOGGER.info("[Catocraft] Mob not killed by player, skipping.");
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        CatocraftMod.LOGGER.info("[Catocraft] Player {} used weapon {}", player.getName().getString(), weapon.getItem());
        if (weapon.isEmpty()) return;

        applyProsperity(event.getDrops(), weapon, player, level);
        applyAttraction(event.getDrops(), weapon, player, level);
    }

    // -------------------
    // Prosperity logic
    // -------------------
    private static void applyProsperity(Collection<ItemEntity> drops, ItemStack stack, Player player, Level level) {
        CatocraftMod.LOGGER.info("[Catocraft] Checking Prosperity for {}", stack.getItem());

        HolderSet<Item> prosperityItems = ModTags.getProsperityItemsHolder();
        if (!isItemInHolderSet(stack, prosperityItems, "PROSPERITY_ITEMS")) return;

        int levelEnch = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.PROSPERITY.getHolder(), stack);
        CatocraftMod.LOGGER.info("[Catocraft] Prosperity enchantment level = {}", levelEnch);
        if (levelEnch <= 0) return;

        RandomSource random = player.getRandom();
        float chancePerLevel = 0.20f; // 20% per level

        HolderSet<Item> affectedDrops = ModTags.getProsperityAffectedHolder();

        for (ItemEntity drop : drops) {
            ItemStack dropStack = drop.getItem();
            if (!isItemInHolderSet(dropStack, affectedDrops, "PROSPERITY_AFFECTED")) continue;

            // Calculate total extra drops based on level
            float totalChance = levelEnch * chancePerLevel;
            int guaranteedExtra = (int) totalChance;          // full 100% chunks = guaranteed extra drops
            float leftoverChance = totalChance - guaranteedExtra; // fractional leftover chance

            // Apply guaranteed extra drops
            if (guaranteedExtra > 0) {
                dropStack.grow(guaranteedExtra);
                CatocraftMod.LOGGER.info("[Catocraft] Prosperity guaranteed: grew {} by {} to count {}", dropStack.getItem(), guaranteedExtra, dropStack.getCount());
            }

            // Apply leftover chance
            float roll = random.nextFloat();
            CatocraftMod.LOGGER.info("[Catocraft] Prosperity roll={} vs leftoverChance={} for {}", roll, leftoverChance, dropStack.getItem());
            if (roll < leftoverChance) {
                dropStack.grow(1);
                CatocraftMod.LOGGER.info("[Catocraft] Prosperity extra by chance: grew {} to count {}", dropStack.getItem(), dropStack.getCount());
            }
        }
    }

    // -------------------
    // Attraction logic
    // -------------------
    private static void applyAttraction(Collection<ItemEntity> drops, ItemStack stack, Player player, Level level) {
        CatocraftMod.LOGGER.info("[Catocraft] Checking Attraction for {}", stack.getItem());

        HolderSet<Item> attractionItems = ModTags.getAttractionItemsHolder();
        if (!isItemInHolderSet(stack, attractionItems, "ATTRACTION_ITEMS")) return;

        UUID playerUUID = player.getUUID();
        HolderSet<Item> affectedDrops = ModTags.getAttractionAffectedHolder();

        for (ItemEntity drop : drops) {
            ItemStack dropStack = drop.getItem();
            if (!isItemInHolderSet(dropStack, affectedDrops, "ATTRACTION_AFFECTED")) continue;

            drop.getPersistentData().putUUID("CatocraftMiner", playerUUID);
            CatocraftMod.LOGGER.info("[Catocraft] Attraction applied: tagged {} with player {}", dropStack.getItem(), playerUUID);
        }
    }

    // -------------------
    // Helper methods for HolderSets
    // -------------------
    private static boolean isItemInHolderSet(ItemStack stack, HolderSet<Item> holderSet, String debugName) {
        for (Holder<Item> holder : holderSet) {
            if (holder.value() == stack.getItem()) {
                CatocraftMod.LOGGER.info("[Catocraft] {} is in {}", stack.getItem(), debugName);
                return true;
            }
        }
        CatocraftMod.LOGGER.info("[Catocraft] {} not in {}", stack.getItem(), debugName);
        return false;
    }
}