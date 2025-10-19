package net.chriskatze.catocraftmod.capability.util;

import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Map;

/**
 * Shared static utilities for managing equipment slot groups and ItemStack logic.
 * <p>
 * These helpers are intentionally simple, pure functions that can be used by:
 *  - {@link net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability}
 *  - Right-click equip/unequip item handlers
 *  - GUI and dynamic menu builders
 *  - Future in-game UI constructors or editors
 */
public class EquipmentUtils {

    /**
     * Finds the first valid (empty) slot index for a given equipment group.
     * Returns -1 if no suitable slot was found.
     */
    public static int findFirstValidSlot(Map<EquipmentGroup, ItemStackHandler> groups, EquipmentGroup group) {
        var handler = groups.get(group);
        if (handler == null) return -1;

        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the first empty slot index within a given group.
     * Returns -1 if the group does not exist or all slots are full.
     */
    public static int findEmptySlot(Map<EquipmentGroup, ItemStackHandler> groups, EquipmentGroup group) {
        var handler = groups.get(group);
        if (handler == null) return -1;

        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds a slot index in a group that already contains the same item type.
     * Returns -1 if none are found.
     */
    public static int findMatchingSlot(Map<EquipmentGroup, ItemStackHandler> groups, EquipmentGroup group, ItemStack stack) {
        var handler = groups.get(group);
        if (handler == null) return -1;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack current = handler.getStackInSlot(i);
            if (ItemStack.isSameItem(current, stack)) {
                return i;
            }
        }
        return -1;
    }
}