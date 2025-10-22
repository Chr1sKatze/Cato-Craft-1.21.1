package net.chriskatze.catocraftmod.menu.runtime;


import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Wraps an ItemStackHandler to behave like a vanilla Container.
 * This lets us use it in Slot objects.
 */
public class HandlerWrapper implements Container {

    private final ItemStackHandler handler;
    private final int index;

    public HandlerWrapper(ItemStackHandler handler, int index) {
        this.handler = handler;
        this.index = index;
    }

    @Override
    public int getContainerSize() {
        return handler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < handler.getSlots(); i++)
            if (!handler.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = handler.extractItem(slot, amount, false);
        setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = handler.getStackInSlot(slot);
        handler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < handler.getSlots(); i++)
            handler.setStackInSlot(i, ItemStack.EMPTY);
    }
}