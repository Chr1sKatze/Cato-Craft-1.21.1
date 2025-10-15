package net.chriskatze.catocraftmod.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class EarringItemHandler extends ItemStackHandler {
    public EarringItemHandler() {
        super(1); // only one slot
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // TODO: replace with your earring item check
        return !stack.isEmpty();
    }

    // Custom NBT save/load helpers
    public CompoundTag saveNBT(HolderLookup.Provider provider) {
        return serializeNBT(provider);
    }

    public void loadNBT(HolderLookup.Provider provider, CompoundTag tag) {
        deserializeNBT(provider, tag);
    }
}