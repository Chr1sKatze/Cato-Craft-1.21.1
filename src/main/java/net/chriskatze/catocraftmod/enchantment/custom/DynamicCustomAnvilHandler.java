package net.chriskatze.catocraftmod.enchantment.custom;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.chriskatze.catocraftmod.CatocraftMod;

import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicCustomAnvilHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack target = event.getLeft();
        ItemStack book = event.getRight();

        if (target.isEmpty() || book.isEmpty()) return;

        Player player = event.getPlayer();
        Level world = player.getCommandSenderWorld();

        // Get registry lookup for enchantments
        HolderLookup.RegistryLookup<Enchantment> lookup =
                world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // Access enchantments through IItemStackExtension
        IItemStackExtension targetExt = (IItemStackExtension) target;
        IItemStackExtension bookExt = (IItemStackExtension) book;

        ItemEnchantments bookEnchants = bookExt.getAllEnchantments(lookup);

        // --- Fallback for enchanted books (StoredEnchantments) ---
        if (bookEnchants.isEmpty() && book.getItem() instanceof EnchantedBookItem) {
            ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!stored.isEmpty()) {
                bookEnchants = stored;
                CatocraftMod.LOGGER.info("Loaded stored enchantments from book: {}", stored);
            }
        }

        if (bookEnchants.isEmpty()) {
            CatocraftMod.LOGGER.info("Book has no enchantments!");
            return;
        }

        ItemEnchantments targetEnchants = targetExt.getAllEnchantments(lookup);

        // Copy target to create result
        ItemStack result = target.copy();
        AtomicBoolean changed = new AtomicBoolean(false);

        // Merge enchantments into mutable
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(targetEnchants);

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : bookEnchants.entrySet()) {
            Holder<Enchantment> enchHolder = entry.getKey();
            int bookLevel = entry.getIntValue();
            int currentLevel = targetEnchants.getLevel(enchHolder);
            int maxLevel = enchHolder.value().getMaxLevel();

            // Vanilla rule: only apply if the target item supports this enchantment
            boolean canApply = enchHolder.value().definition().supportedItems()
                    .stream()
                    .anyMatch(target::is);
            if (!canApply) continue;

            int newLevel = Math.min(currentLevel + bookLevel, maxLevel);
            if (newLevel > currentLevel) {
                merged.set(enchHolder, newLevel);
                changed.set(true);
            }
        }

        if (!changed.get()) return;

        // Apply merged enchantments back to result
        EnchantmentHelper.setEnchantments(result, merged.toImmutable());

        // Vanilla behaviour: keep the name, just change enchantments
        event.setOutput(result);
        event.setCost(1);

        player.sendSystemMessage(Component.literal("Upgraded enchantments"));
        CatocraftMod.LOGGER.info("Tool upgraded via anvil: {} -> {}", target, result);
    }
}