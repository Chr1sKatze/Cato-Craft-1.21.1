package net.chriskatze.catocraftmod.menu.runtime;


import net.chriskatze.catocraftmod.menu.layout.SlotType;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A normal Slot that remembers its SlotType for rendering or logic.
 */
public class TypedSlot extends Slot {
    private final SlotType slotType;

    public TypedSlot(SlotType slotType, Container container, int index, int x, int y) {
        super(container, index, x, y);
        this.slotType = slotType;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // type-specific restrictions can go here
        return switch (slotType) {
            case CRAFTING_RESULT, INGREDIENT_RESULT -> false;
            default -> super.mayPlace(stack);
        };
    }
}