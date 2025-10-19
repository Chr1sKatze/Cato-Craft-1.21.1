package net.chriskatze.catocraftmod.enchantment.custom;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * A helper class representing a custom enchantment for the mod.
 *
 * Stores the enchantment's ResourceKey, maximum level, and a cached Holder
 * for registry access. This is used with ModEnchantments to bootstrap and
 * manage custom enchantments safely.
 */
public class ModEnchantmentEntry {

    /** The registry key for this enchantment */
    private final ResourceKey<Enchantment> key;

    /** The maximum allowed level for this enchantment */
    private final int maxLevel;

    /** Cached Holder for this enchantment from the registry (may be null until initialized) */
    private Holder<Enchantment> holder;

    public ModEnchantmentEntry(ResourceKey<Enchantment> key, int maxLevel) {
        this.key = key;
        this.maxLevel = maxLevel;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public ResourceKey<Enchantment> getKey() { return key; }
    public int getMaxLevel() { return maxLevel; }
    public Holder<Enchantment> getHolder() { return holder; }

    /** @return true once the registry holder has been initialized */
    public boolean hasHolder() { return holder != null; }

    /**
     * @return the holder or null if not initialized
     * (useful for optional UI contexts without throwing)
     */
    public Holder<Enchantment> getHolderOrNull() { return holder; }

    /**
     * @return the holder or throws if not initialized
     * (useful for server-only logic that expects full init)
     */
    public Holder<Enchantment> getHolderOrThrow() {
        if (holder == null) {
            throw new IllegalStateException("Enchantment holder not initialized for " + key.location());
        }
        return holder;
    }

    // ── Setters ────────────────────────────────────────────────────────────
    public void setHolder(Holder<Enchantment> holder) { this.holder = holder; }
}