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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class DynamicCustomAnvilHandler {

    private static final Map<String, Set<String>> sentMessages = new HashMap<>();
    private static String lastLeftHash = "";
    private static String lastRightHash = "";

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        Player player = event.getPlayer();

        if (left.isEmpty()) return;

        resetMessageCacheIfChanged(left, right);

        ItemStack result = left.copy();
        boolean operationSuccess = false;
        int materialCost = 0;
        String enchantmentErrorMessage = null;

        // ---------------- Repair logic ----------------
        AnvilConfig.RepairInfo repairInfo = AnvilConfig.getRepairInfo(left.getItem());
        if (repairInfo != null && !right.isEmpty() && right.getItem() == repairInfo.repairItem) {
            int damage = left.getDamageValue();
            int usableItems = (int) Math.min(Math.ceil(damage / (left.getMaxDamage() * repairInfo.repairPercentage)), right.getCount());
            if (usableItems > 0) {
                int totalRepair = Math.min((int) Math.round(left.getMaxDamage() * repairInfo.repairPercentage * usableItems), damage);
                result.setDamageValue(damage - totalRepair);
                materialCost += usableItems;
                operationSuccess = true;
            }
        }

        // ---------------- Enchantment logic ----------------
        if (!right.isEmpty()) {
            Level world = player.getCommandSenderWorld();
            var lookup = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            IItemStackExtension leftExt = (IItemStackExtension) left;
            IItemStackExtension rightExt = (IItemStackExtension) right;

            ItemEnchantments leftEnchants = leftExt.getAllEnchantments(lookup);
            ItemEnchantments rightEnchants = rightExt.getAllEnchantments(lookup);

            // If right is an empty enchanted book with stored enchantments, use stored
            if (rightEnchants.isEmpty()) {
                ItemEnchantments stored = right.getOrDefault(
                        net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY
                );
                if (!stored.isEmpty()) rightEnchants = stored;
            }

            ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(leftEnchants);
            int maxEnchantments = AnvilConfig.getMaxEnchantments(left.getItem());

            boolean rightWasCrystal = false;

            // ----------- Handle any CrystalItem -----------
            if (right.getItem() instanceof CrystalItem crystal) {
                rightWasCrystal = true;

                // Look up the target enchantment via the crystal's ID
                ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, crystal.getEnchantmentId());
                Holder<Enchantment> targetEnch = lookup.get(key)
                        .orElseThrow(() -> new IllegalStateException("Missing enchantment: " + crystal.getEnchantmentId()));

                int currentLevel = leftEnchants.getLevel(targetEnch);
                int maxLevel = targetEnch.value().getMaxLevel();

                if (currentLevel >= maxLevel) {
                    enchantmentErrorMessage = right.getHoverName().getString() + " enchantment is already at maximum";
                    operationSuccess = false;
                    materialCost = 0;
                    event.setOutput(ItemStack.EMPTY);
                    event.setCanceled(true);
                } else {
                    // Calculate how many levels we can actually apply
                    int toApply = Math.min(right.getCount() * crystal.getLevel(), maxLevel - currentLevel);

                    // Full crystals consumed and leftover levels
                    int fullCrystalsUsed = toApply / crystal.getLevel();
                    int remainderLevel = toApply % crystal.getLevel();

                    int finalLevel = currentLevel + toApply;
                    merged.set(targetEnch, finalLevel);
                    operationSuccess = true;

                    // NeoForge will automatically consume the number of items
                    materialCost = fullCrystalsUsed + (remainderLevel > 0 ? 1 : 0);
                }
            }

            // ----------- Handle vanilla enchanted books as before -----------
            if (!rightWasCrystal) {
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : rightEnchants.entrySet()) {
                    Holder<Enchantment> ench = entry.getKey();
                    int bookLevel = entry.getIntValue();
                    int currentLevel = leftEnchants.getLevel(ench);

                    boolean canApply = ench.value().definition().supportedItems().stream().anyMatch(left::is);
                    if (!canApply) continue;

                    boolean isNew = currentLevel == 0;
                    long uniqueCount = leftEnchants.keySet().stream()
                            .filter(h -> leftEnchants.getLevel(h) > 0).count();

                    if (isNew && uniqueCount >= maxEnchantments) {
                        enchantmentErrorMessage = "Cannot add more unique enchantments to " + left.getHoverName().getString();
                        continue;
                    }

                    int newLevel = Math.min(currentLevel + bookLevel, ench.value().getMaxLevel());
                    if (newLevel > currentLevel) {
                        merged.set(ench, newLevel);
                        operationSuccess = true;
                    } else if (currentLevel >= ench.value().getMaxLevel()) {
                        enchantmentErrorMessage = getEnchantmentName(ench) + " enchantment limit reached";
                    }
                }
            }

            // Apply merged enchantments if any operation succeeded
            if (operationSuccess) {
                EnchantmentHelper.setEnchantments(result, merged.toImmutable());
            } else if (!rightWasCrystal) { // Only cancel if right is a normal book and no enchantments applied
                result = ItemStack.EMPTY;
                event.setCanceled(true);
            }
        }

        // ---------------- Renaming logic ----------------
        if (event.getName() != null && !event.getName().isEmpty() && right.isEmpty() && !result.isEmpty()) {
            result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(event.getName()));
            operationSuccess = true;
        }

        // ---------------- Send error messages ----------------
        if (enchantmentErrorMessage != null) {
            sendPlayerMessageOnce(player, makePid(player, left, right), enchantmentErrorMessage);
        }

        // ---------------- Finalize ----------------
        if (!operationSuccess) {
            result = ItemStack.EMPTY;
            event.setCanceled(true);
            event.setCost(0);
            event.setMaterialCost(0);
        } else {
            event.setCost(1);
            event.setMaterialCost(materialCost);
        }

        event.setOutput(result);
    }


    // ---------------- HELPERS ----------------

    private static void resetMessageCacheIfChanged(ItemStack left, ItemStack right) {
        String leftHash = String.valueOf(left.hashCode());
        String rightHash = right.isEmpty() ? "" : String.valueOf(right.hashCode());
        if (!leftHash.equals(lastLeftHash) || !rightHash.equals(lastRightHash)) {
            sentMessages.clear();
            lastLeftHash = leftHash;
            lastRightHash = rightHash;
        }
    }

    private static void sendPlayerMessageOnce(Player player, String pid, String message) {
        if (player.level().isClientSide) return;

        sentMessages.computeIfAbsent(pid, k -> new HashSet<>());
        Set<String> messagesForPid = sentMessages.get(pid);

        if (!messagesForPid.contains(message)) {
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
            messagesForPid.add(message); // add AFTER sending
        }
    }

    private static String makePid(Player player, ItemStack left, ItemStack right) {
        String leftHash = String.valueOf(left.hashCode());
        String rightHash = right.isEmpty() ? "" : String.valueOf(right.hashCode());
        return player.getUUID() + "-" + leftHash + "-" + rightHash;
    }

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