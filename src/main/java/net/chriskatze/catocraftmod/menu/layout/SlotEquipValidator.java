package net.chriskatze.catocraftmod.menu.layout;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ✅ SlotEquipValidator — unified rule validator for dynamic menus
 *
 * Validates whether a group or item can be equipped based on JSON definitions
 * loaded through {@link SlotLayoutLoader}. Supports both client- and server-side
 * checks, requirements, conflicts, and valid item rules.
 */
public class SlotEquipValidator {

    // ────────────────────────────────────────────────
    // GROUP VALIDATION — dependency & conflict logic
    // ────────────────────────────────────────────────
    public static boolean canEquip(ServerPlayer player, EquipmentGroup targetGroup, Set<EquipmentGroup> equippedGroups) {
        SlotLayoutDefinition def = SlotLayoutLoader.getDefinition(targetGroup);
        if (def == null) return true;

        String targetKey = targetGroup.getKey();

        // 1️⃣ Requirements
        for (String req : def.requires()) {
            boolean has = equippedGroups.stream()
                    .anyMatch(g -> g.getKey().equalsIgnoreCase(req));
            if (!has) {
                player.displayClientMessage(Component.literal("⚠ You must equip " + req + " first!"), true);
                return false;
            }
        }

        // 2️⃣ Conflicts
        for (String conflict : def.conflicts()) {
            boolean hasConflict = equippedGroups.stream()
                    .anyMatch(g -> g.getKey().equalsIgnoreCase(conflict));
            if (hasConflict) {
                player.displayClientMessage(
                        Component.literal("You cannot equip " + targetKey + " while " + conflict + " is equipped!"),
                        true
                );
                return false;
            }
        }

// 3️⃣ Tag-based mutual exclusion (same category slots)
        if (def.tags != null && !def.tags.isEmpty()) {
            Set<String> thisTags = new HashSet<>(def.tags);
            for (EquipmentGroup equipped : equippedGroups) {
                SlotLayoutDefinition otherDef = SlotLayoutLoader.getDefinition(equipped);
                if (otherDef == null || otherDef.tags == null) continue;

                for (String tag : otherDef.tags) {
                    if (thisTags.contains(tag)) {
                        player.displayClientMessage(
                                Component.literal("You already have an item equipped in another '" + tag + "' slot!"),
                                true
                        );
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // ────────────────────────────────────────────────
    // ITEM VALIDATION — based on "valid_items" list
    // ────────────────────────────────────────────────
    public static boolean canEquipItem(ServerPlayer player, EquipmentGroup group, ItemStack stack) {
        if (stack.isEmpty()) return true;

        SlotLayoutDefinition def = SlotLayoutLoader.getDefinition(group);
        if (def == null) return true;

        List<String> rules = def.valid_items();
        if (rules == null || rules.isEmpty()) return true;

        boolean allowed = matchesAnyRule(stack, rules);
        if (!allowed) {
            player.displayClientMessage(
                    Component.literal("That item cannot be equipped in " + group.getKey() + "."),
                    true
            );
        }
        return allowed;
    }

    // ────────────────────────────────────────────────
    // CLIENT-SAFE OVERLOAD — used from GUI slots
    // ────────────────────────────────────────────────
    public static boolean canEquipItem(Player player, ResourceLocation groupId, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            EquipmentGroup group = EquipmentGroup.fromId(groupId);
            return group == null || canEquipItem(serverPlayer, group, stack);
        }

        // Client-only fallback
        if (stack.isEmpty()) return true;
        SlotLayoutDefinition def = SlotLayoutLoader.getDefinition(groupId);
        if (def == null || def.valid_items().isEmpty()) return true;
        return matchesAnyRule(stack, def.valid_items());
    }

    // ────────────────────────────────────────────────
    // Helper: rule matcher
    // ────────────────────────────────────────────────
    private static boolean matchesAnyRule(ItemStack stack, List<String> rules) {
        for (String raw : rules) {
            if (raw == null || raw.isBlank()) continue;
            String rule = raw.trim();

            // Tag rule (#mod:tag)
            if (rule.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(rule.substring(1));
                if (tagId != null) {
                    TagKey<Item> key = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
                    if (stack.is(key)) return true;
                }
            } else {
                // Direct item id (mod:item)
                ResourceLocation itemId = ResourceLocation.tryParse(rule);
                if (itemId != null) {
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item != null && item != net.minecraft.world.item.Items.AIR && stack.is(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ────────────────────────────────────────────────
    // Legacy compatibility overloads (kept temporarily)
    // ────────────────────────────────────────────────
    @Deprecated(forRemoval = true)
    public static boolean canEquip(ServerPlayer player, ResourceLocation groupId, Set<ResourceLocation> equippedGroups) {
        EquipmentGroup group = EquipmentGroup.fromId(groupId);
        if (group == null) return true;
        Set<EquipmentGroup> converted = new HashSet<>();
        for (ResourceLocation id : equippedGroups) {
            EquipmentGroup g = EquipmentGroup.fromId(id);
            if (g != null) converted.add(g);
        }
        return canEquip(player, group, converted);
    }

    @Deprecated(forRemoval = true)
    public static boolean canEquipItem(ServerPlayer player, ResourceLocation groupId, ItemStack stack) {
        EquipmentGroup group = EquipmentGroup.fromId(groupId);
        return group == null || canEquipItem(player, group, stack);
    }
}