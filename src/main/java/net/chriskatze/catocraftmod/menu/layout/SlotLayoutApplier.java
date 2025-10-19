package net.chriskatze.catocraftmod.menu.layout;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Applies a SlotLayout to a menu, dynamically building Slot instances.
 * Supports both grid and procedural modes.
 */
public class SlotLayoutApplier {

    /**
     * Adds slots from a SlotLayout definition to a menu.
     *
     * @param layout The layout definition
     * @param container The backing inventory (ItemStackHandler via wrapper)
     * @param playerInventory The player's inventory (for shift-click behavior)
     * @param addSlotFunc The callback used to register new Slot instances
     * @param validator Optional item validator (e.g. from layout.validItems)
     */
    public static void buildSlots(
            SlotLayout layout,
            Container container,
            Inventory playerInventory,
            Function<Slot, Slot> addSlotFunc,
            Predicate<ItemStack> validator
    ) {
        int originX = layout.originX();
        int originY = layout.originY();
        int spacing = layout.spacing();
        int wrapAfter = layout.wrapAfter();
        boolean gridMode = layout.isGridMode();

        int index = 0;

        if (gridMode) {
            for (SlotLayout.Point cell : layout.cells()) {
                int x = originX + cell.x() * spacing;
                int y = originY + cell.y() * spacing;
                addSlotFunc.apply(new ValidatedSlot(container, index++, x, y, validator));
            }
        } else {
            int x = originX;
            int y = originY;
            for (int i = 0; i < container.getContainerSize(); i++) {
                addSlotFunc.apply(new ValidatedSlot(container, i, x, y, validator));

                if (layout.direction() == LayoutDirection.HORIZONTAL) {
                    x += spacing;
                    if (wrapAfter > 0 && (i + 1) % wrapAfter == 0) {
                        x = originX;
                        y += spacing;
                    }
                } else {
                    y += spacing;
                    if (wrapAfter > 0 && (i + 1) % wrapAfter == 0) {
                        y = originY;
                        x += spacing;
                    }
                }
            }
        }
    }

    /**
     * A slot wrapper that only accepts items matching a predicate.
     */
    private static class ValidatedSlot extends Slot {
        private final Predicate<ItemStack> validator;

        public ValidatedSlot(Container container, int index, int x, int y, Predicate<ItemStack> validator) {
            super(container, index, x, y);
            this.validator = validator != null ? validator : stack -> true;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return validator.test(stack);
        }
    }
}