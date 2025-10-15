package net.chriskatze.catocraftmod.enchantment.custom;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.config.AnvilConfig;
import net.chriskatze.catocraftmod.item.CrystalItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles dynamic custom anvil behavior:
 * - Repairing items based on custom config
 * - Applying custom enchantments via CrystalItem or normal enchanted books
 * - Sending one-time error messages to the player
 *
 * This class intercepts the AnvilUpdateEvent from NeoForged and applies
 * custom rules for repair, enchantments, and crystals.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class DynamicCustomAnvilHandler {

    // ---------------- Tag for identifying items that can be enchanted with crystals ----------------
    private static final TagKey<Item> ALLOWED_CRYSTAL_TARGETS = ItemTags.create(
            ResourceLocation.tryParse("catocraftmod:enchantable_crystal_items")
    );

    // ---------------- Caches to prevent sending duplicate messages to the player ----------------
    private static final Map<String, Set<String>> sentMessages = new HashMap<>();
    private static String lastLeftHash = "";
    private static String lastRightHash = "";

    /**
     * Main handler for the anvil update event.
     * This method applies repair logic, enchantment logic (both crystals and books),
     * and ensures max enchantment limits are respected.
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();      // Left item in anvil (item being modified)
        ItemStack right = event.getRight();    // Right item in anvil (material or enchanted book)
        Player player = event.getPlayer();

        if (left.isEmpty()) return;  // Nothing to do if left item is empty

        // Reset message cache if left or right items changed
        resetMessageCacheIfChanged(left, right);

        // Create a copy of the left item to serve as the resulting item
        ItemStack result = left.copy();
        boolean operationSuccess = false;   // Tracks if any operation was successful
        int materialCost = 0;               // Tracks number of materials used

        Level world = player.getCommandSenderWorld();
        var lookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // Use NeoForged extensions to read all enchantments from items
        IItemStackExtension leftExt = (IItemStackExtension) left;
        IItemStackExtension rightExt = (IItemStackExtension) right;

        ItemEnchantments leftEnchants = leftExt.getAllEnchantments(lookup);
        ItemEnchantments rightEnchants = rightExt.getAllEnchantments(lookup);

        // If right item has no enchantments, check for stored enchantments (like empty books)
        if (rightEnchants.isEmpty()) {
            ItemEnchantments stored = right.getOrDefault(
                    net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY
            );
            if (!stored.isEmpty()) rightEnchants = stored;
        }

        // Mutable copy to apply changes without altering original items
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(leftEnchants);

        // Maximum number of unique enchantments allowed for this item (from config)
        int maxEnchantments = AnvilConfig.getMaxEnchantments(left.getItem());
        boolean rightWasCrystal = false;  // Tracks whether the right item is a CrystalItem

        // ---------------- Repair logic ----------------
        AnvilConfig.RepairInfo repairInfo = AnvilConfig.getRepairInfo(left.getItem());
        if (repairInfo != null && !right.isEmpty() && right.getItem() == repairInfo.repairItem) {
            int damage = left.getDamageValue();
            int usableItems = (int) Math.min(
                    Math.ceil(damage / (left.getMaxDamage() * repairInfo.repairPercentage)),
                    right.getCount()
            );

            if (usableItems > 0) {
                int totalRepair = Math.min(
                        (int) Math.round(left.getMaxDamage() * repairInfo.repairPercentage * usableItems),
                        damage
                );
                result.setDamageValue(damage - totalRepair);
                materialCost += usableItems;
                operationSuccess = true;
            }
        }

        // ---------------- Handle CrystalItem ----------------
        if (right.getItem() instanceof CrystalItem crystal) {
            rightWasCrystal = true;

            // Only certain items can be enchanted with crystals
            if (!left.is(ALLOWED_CRYSTAL_TARGETS)) {
                cancelAnvilOperation(event, player, left, right, "This item cannot be enchanted with crystals!");
                return;
            }

            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, crystal.getEnchantmentId());
            Holder<Enchantment> targetEnch = lookup.get(key)
                    .orElseThrow(() -> new IllegalStateException("Missing enchantment: " + crystal.getEnchantmentId()));

            int currentLevel = merged.getLevel(targetEnch);
            int maxLevel = targetEnch.value().getMaxLevel();
            boolean isNewEnchantment = currentLevel == 0;
            long uniqueCount = merged.keySet().stream().filter(h -> merged.getLevel(h) > 0).count();

            // Check if adding a new unique enchantment would exceed max
            if (isNewEnchantment && uniqueCount >= maxEnchantments) {
                cancelAnvilOperation(event, player, left, right,
                        "Cannot add more unique enchantments to " + left.getHoverName().getString());
                return;
            }
            // Check if this enchantment is already at its max level
            else if (currentLevel >= maxLevel) {
                cancelAnvilOperation(event, player, left, right,
                        right.getHoverName().getString() + " enchantment is already at maximum");
                return;
            }

            // Determine how many crystals to apply without exceeding max level
            int remaining = maxLevel - currentLevel;
            int crystalsToApply = Math.min(right.getCount(), (int) Math.ceil((double) remaining / crystal.getLevel()));
            int finalLevel = Math.min(currentLevel + crystalsToApply * crystal.getLevel(), maxLevel);
            merged.set(targetEnch, finalLevel);

            operationSuccess = true;
            materialCost = crystalsToApply;
        }

        // ---------------- Handle vanilla enchanted books ----------------
        if (!rightWasCrystal) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : rightEnchants.entrySet()) {
                Holder<Enchantment> ench = entry.getKey();
                int bookLevel = entry.getIntValue();
                int currentLevel = merged.getLevel(ench);

                // Skip if enchantment cannot apply to this item
                boolean canApply = ench.value().definition().supportedItems().stream().anyMatch(left::is);
                if (!canApply) continue;

                boolean isNew = currentLevel == 0;
                long uniqueCount = merged.keySet().stream().filter(h -> merged.getLevel(h) > 0).count();

                // Check if adding a new unique enchantment would exceed max
                if (isNew && uniqueCount >= maxEnchantments) {
                    cancelAnvilOperation(event, player, left, right,
                            "Cannot add more unique enchantments to " + left.getHoverName().getString());
                    return;
                }

                // Apply enchantment without exceeding max level
                int newLevel = Math.min(currentLevel + bookLevel, ench.value().getMaxLevel());
                if (newLevel > currentLevel) {
                    merged.set(ench, newLevel);
                    operationSuccess = true;
                } else if (currentLevel >= ench.value().getMaxLevel()) {
                    cancelAnvilOperation(event, player, left, right,
                            getEnchantmentName(ench) + " enchantment limit reached");
                    return;
                }
            }
        }

        // ---------------- Finalize result ----------------
        if (operationSuccess) {
            EnchantmentHelper.setEnchantments(result, merged.toImmutable());
        } else if (!rightWasCrystal) {
            result = ItemStack.EMPTY;
            event.setCanceled(true);
        }

        event.setOutput(result);
        event.setCost(operationSuccess ? 1 : 0);
        event.setMaterialCost(operationSuccess ? materialCost : 0);
    }

    // ---------------- Helper Methods ----------------

    /**
     * Resets message cache if left/right items have changed.
     * This prevents spamming players with repeated messages.
     */
    private static void resetMessageCacheIfChanged(ItemStack left, ItemStack right) {
        String leftHash = String.valueOf(left.hashCode());
        String rightHash = right.isEmpty() ? "" : String.valueOf(right.hashCode());
        if (!leftHash.equals(lastLeftHash) || !rightHash.equals(lastRightHash)) {
            sentMessages.clear();
            lastLeftHash = leftHash;
            lastRightHash = rightHash;
        }
    }

    /**
     * Sends a message to the player only once per operation.
     */
    private static void sendPlayerMessageOnce(Player player, String pid, String message) {
        if (player.level().isClientSide) return;

        sentMessages.computeIfAbsent(pid, k -> new HashSet<>());
        Set<String> messagesForPid = sentMessages.get(pid);

        if (!messagesForPid.contains(message)) {
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
            messagesForPid.add(message);
        }
    }

    /**
     * Generates a unique ID for the player + item combination to track sent messages.
     */
    private static String makePid(Player player, ItemStack left, ItemStack right) {
        String leftHash = String.valueOf(left.hashCode());
        String rightHash = right.isEmpty() ? "" : String.valueOf(right.hashCode());
        return player.getUUID() + "-" + leftHash + "-" + rightHash;
    }

    /**
     * Cancels the current anvil operation and sends a message to the player.
     */
    private static void cancelAnvilOperation(AnvilUpdateEvent event, Player player, ItemStack left, ItemStack right, String message) {
        event.setOutput(ItemStack.EMPTY);
        event.setCanceled(true);
        event.setCost(0);
        event.setMaterialCost(0);
        sendPlayerMessageOnce(player, makePid(player, left, right), message);
    }

    /**
     * Converts an Enchantment Holder into a human-readable name.
     */
    private static String getEnchantmentName(Holder<Enchantment> ench) {
        String path = ench.unwrapKey().map(k -> k.location().getPath()).orElse("unknown_enchantment");
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}