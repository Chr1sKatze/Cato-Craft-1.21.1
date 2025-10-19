package net.chriskatze.catocraftmod.menu.layout;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 🔒 SlotRuleManager
 * Handles dependency, conflict, and (future) link logic between slot groups.
 *
 * - "requires": ensures certain groups are equipped first.
 * - "conflicts_with" / "conflicts": blocks equipping incompatible groups.
 * - "linked_with": (optional) metadata for synchronization, handled elsewhere.
 */
public class SlotRuleManager {

    /**
     * Checks whether equipping a group is allowed, given currently equipped groups.
     * Returns a message if disallowed, or {@code null} if allowed.
     */
    public static Component canEquip(String targetGroup,
                                     Set<String> equippedGroups,
                                     Map<String, SlotLayoutDefinition> defs,
                                     ServerPlayer player) {

        SlotLayoutDefinition def = defs.get(targetGroup);
        if (def == null) return null; // No definition → no rules → allow

        // ───────────────────────────────────────────────
        // 1️⃣ Requirements
        // ───────────────────────────────────────────────
        List<String> requires = def.requires() == null ? List.of() : def.requires();
        for (String required : requires) {
            if (!matchesAny(equippedGroups, required)) {
                return Component.literal("You must equip " + required + " first!");
            }
        }

        // ───────────────────────────────────────────────
        // 2️⃣ Conflicts
        // ───────────────────────────────────────────────
        List<String> conflicts = def.conflicts() == null ? List.of() : def.conflicts();
        for (String conflict : conflicts) {
            if (matchesAny(equippedGroups, conflict)) {
                return Component.literal("You cannot equip " + targetGroup + " while " + conflict + " is equipped!");
            }
        }

        // ✅ All rules passed
        return null;
    }

    /** Utility: check if the equipped set contains a group ID or its simple alias. */
    private static boolean matchesAny(Set<String> equippedGroups, String ruleId) {
        for (String equipped : equippedGroups) {
            // Allow flexible matching: "equipment/earrings" or just "earrings"
            if (equipped.equals(ruleId)
                    || equipped.endsWith("/" + ruleId)
                    || equipped.equalsIgnoreCase(ruleId)) {
                return true;
            }
        }
        return false;
    }

    /** Boolean-only convenience overload (no message). */
    public static boolean isEquipAllowed(String group,
                                         Set<String> equippedGroups,
                                         Map<String, SlotLayoutDefinition> defs) {
        return canEquip(group, equippedGroups, defs, null) == null;
    }
}