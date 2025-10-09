package net.chriskatze.catocraftmod.enchantment.custom;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Helper class to store a custom enchantment and its max level.
 */
public class ModEnchantmentEntry {
    private final ResourceKey<Enchantment> key;
    private final int maxLevel;
    private Holder<Enchantment> holder; // Add this field

    public ModEnchantmentEntry(ResourceKey<Enchantment> key, int maxLevel) {
        this.key = key;
        this.maxLevel = maxLevel;
    }

    public ResourceKey<Enchantment> getKey() {
        return key;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    // Add these methods:
    public void setHolder(Holder<Enchantment> holder) {
        this.holder = holder;
    }

    public Holder<Enchantment> getHolder() {
        return holder;
    }
}