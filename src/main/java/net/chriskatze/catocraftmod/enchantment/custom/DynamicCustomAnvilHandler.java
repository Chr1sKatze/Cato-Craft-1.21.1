package net.chriskatze.catocraftmod.enchantment.custom;

import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dynamic anvil handler for multiple custom enchantments.
 * Supports linear progression of enchantment levels via a single book type per enchantment.
 */
public class DynamicCustomAnvilHandler {

    private static final Map<ResourceKey<Enchantment>, Integer> CUSTOM_ENCHANTMENTS = new HashMap<>();

    static {
        CUSTOM_ENCHANTMENTS.put(ModEnchantments.GATHERING_SPEED.getKey(), ModEnchantments.GATHERING_SPEED.getMaxLevel());
    }

    private static final ResourceLocation GATHERING_TOOLS_TAG = Objects.requireNonNull(
            ResourceLocation.tryParse("catocraftmod:gathering_tools")
    );

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        Player player = event.getPlayer();
        Level world = player.getCommandSenderWorld();

        // Get the enchantment key from the book
        ResourceKey<Enchantment> enchantmentKey = ModItems.getEnchantmentKeyFromBook(right.getItem());
        if (enchantmentKey == null) return;

        if (!left.is(ItemTags.create(GATHERING_TOOLS_TAG))) return;

        // Get Holder<Enchantment>
        Holder<Enchantment> enchantmentHolder = world.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(enchantmentKey);

        // Get the raw Enchantment from the holder
        Enchantment enchantment = enchantmentHolder.value();

        // Current level using Holder<Enchantment>
        int currentLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantmentHolder, left);

        int maxLevel = CUSTOM_ENCHANTMENTS.getOrDefault(enchantmentKey, 1);
        int newLevel = Math.min(currentLevel + 1, maxLevel);

        // Apply enchantment to a copy of the tool
        ItemStack result = left.copy();

        // Get existing enchantments on the item
        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(result);

        // Update or add your custom enchantment
        currentEnchants.put(enchantment, newLevel);

        // Apply the updated enchantments back to the ItemStack
        EnchantmentHelper.setEnchantments(currentEnchants, result);

        // Set output
        event.setOutput(result);
        event.setCost(1);
    }
}