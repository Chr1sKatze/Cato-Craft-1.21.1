package net.chriskatze.catocraftmod.enchantment.custom;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.config.AnvilConfig;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Handles custom enchantment effects on block and mob drops.
 *
 * Specifically manages:
 * - Prosperity: increases item drops based on enchantment level
 * - Attraction: tags dropped items so they can be "pulled" to the player
 *
 * Subscribes to Forge/Neoforge events for block and mob drops.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class BlockDropHandler {

    // ---------------------------------------------------------------------
    // Block drop handling
    // ---------------------------------------------------------------------

    /**
     * Called when a block drops items.
     * Applies custom enchantment effects based on the tool used.
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return; // Only run on server

        var breaker = event.getBreaker();
        if (!(breaker instanceof Player player)) return;

        ItemStack tool = event.getTool();
        if (tool.isEmpty()) return;

        // Apply custom enchantments
        applyProsperity(event.getDrops(), tool, player, level);
        applyAttraction(event.getDrops(), tool, player, level);
    }

    // ---------------------------------------------------------------------
    // Mob drop handling
    // ---------------------------------------------------------------------

    /**
     * Called when a living entity drops items.
     * Applies custom enchantment effects based on the weapon used.
     */
    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.getCommandSenderWorld();
        if (level.isClientSide()) return; // Only run on server

        var sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof Player player)) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        // Apply custom enchantments
        applyProsperity(event.getDrops(), weapon, player, level);
        applyAttraction(event.getDrops(), weapon, player, level);
    }

    // ---------------------------------------------------------------------
    // Prosperity enchantment logic
    // ---------------------------------------------------------------------

    /**
     * Modifies drops based on the Prosperity enchantment level.
     *
     * @param drops  The items dropped by the block or mob
     * @param stack  The tool/weapon used
     * @param player The player who broke/killed the entity
     * @param level  The level of the world
     */
    private static void applyProsperity(Collection<ItemEntity> drops, ItemStack stack, Player player, Level level) {

        // Only proceed if the tool/weapon is tagged as valid for Prosperity
        HolderSet<Item> prosperityItems = ModTags.getProsperityItemsHolder();
        if (!isItemInHolderSet(stack, prosperityItems)) return;

        // Get enchantment level
        int levelEnch = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.PROSPERITY.getHolder(), stack);
        if (levelEnch <= 0) return;

        RandomSource random = player.getRandom();
        float chancePerLevel = 0.20f; // 20% chance per enchantment level

        // Only apply Prosperity to items affected by this enchantment
        HolderSet<Item> affectedDrops = ModTags.getProsperityAffectedHolder();

        for (ItemEntity drop : drops) {
            ItemStack dropStack = drop.getItem();
            if (!isItemInHolderSet(dropStack, affectedDrops)) continue;

            // Calculate extra drops
            float totalChance = levelEnch * chancePerLevel;
            int guaranteedExtra = (int) totalChance;          // whole number extra drops
            float leftoverChance = totalChance - guaranteedExtra; // fractional extra chance

            // Apply guaranteed extra drops
            if (guaranteedExtra > 0) dropStack.grow(guaranteedExtra);

            // Apply leftover chance
            if (random.nextFloat() < leftoverChance) dropStack.grow(1);
        }
    }

    // ---------------------------------------------------------------------
    // Attraction enchantment logic
    // ---------------------------------------------------------------------

    /**
     * Tags dropped items with the player's UUID for the Attraction enchantment.
     *
     * @param drops  The items dropped by the block or mob
     * @param stack  The tool/weapon used
     * @param player The player who broke/killed the entity
     * @param level  The level of the world
     */
    private static void applyAttraction(Collection<ItemEntity> drops, ItemStack stack, Player player, Level level) {

        // Only proceed if the tool/weapon is tagged as valid for Attraction
        HolderSet<Item> attractionItems = ModTags.getAttractionItemsHolder();
        if (!isItemInHolderSet(stack, attractionItems)) return;

        UUID playerUUID = player.getUUID();
        HolderSet<Item> affectedDrops = ModTags.getAttractionAffectedHolder();

        for (ItemEntity drop : drops) {
            ItemStack dropStack = drop.getItem();
            if (!isItemInHolderSet(dropStack, affectedDrops)) continue;

            // Tag the item with the player's UUID
            drop.getPersistentData().putUUID("CatocraftMiner", playerUUID);
        }
    }

    // ---------------------------------------------------------------------
    // Helper methods for checking HolderSets
    // ---------------------------------------------------------------------

    /**
     * Checks if a given ItemStack belongs to a HolderSet.
     *
     * @param stack      The item stack to check
     * @param holderSet  The HolderSet to check against
     * @return true if the stack's item is in the HolderSet
     */
    private static boolean isItemInHolderSet(ItemStack stack, HolderSet<Item> holderSet) {
        for (Holder<Item> holder : holderSet) {
            if (holder.value() == stack.getItem()) return true;
        }
        return false;
    }
}