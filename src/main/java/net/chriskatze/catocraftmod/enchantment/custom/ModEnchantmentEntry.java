package net.chriskatze.catocraftmod.enchantment.custom;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * A helper class representing a custom enchantment for the mod.
 *
 * Stores the enchantment's ResourceKey, maximum level, and a cached Holder
 * for registry access. This is used in combination with ModEnchantments
 * to bootstrap and manage custom enchantments safely.
 */
public class ModEnchantmentEntry {

    // ---------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------

    /** The registry key for this enchantment */
    private final ResourceKey<Enchantment> key;

    /** The maximum allowed level for this enchantment */
    private final int maxLevel;

    /** Cached Holder for this enchantment from the registry */
    private Holder<Enchantment> holder;

    // ---------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------

    /**
     * Creates a new ModEnchantmentEntry.
     *
     * @param key      The ResourceKey of the enchantment.
     * @param maxLevel The maximum level of the enchantment.
     */
    public ModEnchantmentEntry(ResourceKey<Enchantment> key, int maxLevel) {
        this.key = key;
        this.maxLevel = maxLevel;
    }

    // ---------------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------------

    /** Returns the ResourceKey of this enchantment */
    public ResourceKey<Enchantment> getKey() {
        return key;
    }

    /** Returns the maximum level allowed for this enchantment */
    public int getMaxLevel() {
        return maxLevel;
    }

    /** Returns the cached Holder for this enchantment */
    public Holder<Enchantment> getHolder() {
        return holder;
    }

    // ---------------------------------------------------------------------
    // Setters
    // ---------------------------------------------------------------------

    /** Caches the Holder for this enchantment after registration */
    public void setHolder(Holder<Enchantment> holder) {
        this.holder = holder;
    }
}