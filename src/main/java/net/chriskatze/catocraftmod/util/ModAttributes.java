package net.chriskatze.catocraftmod.util;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Central attribute helper — keeps modifier UUIDs consistent and creation simple.
 * Fully compatible with Minecraft 1.21.1 / NeoForge 21.1.209.
 */
public class ModAttributes {

    /** Deterministic ID for this modifier (stable across runs & items). */
    public static ResourceLocation id(String key) {
        return ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, key);
    }

    /** Flat amount (e.g., +3 max health). */
    public static AttributeModifier add(String key, double value) {
        return new AttributeModifier(id(key), value, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Percentage of final total (e.g., 0.10 for +10%). */
    public static AttributeModifier multiply(String key, double factor) {
        return new AttributeModifier(id(key), factor, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // Vanilla attributes are Holders in 1.21+
    public static final Holder<Attribute> MAX_HEALTH = Attributes.MAX_HEALTH;
    public static final Holder<Attribute> MOVEMENT_SPEED = Attributes.MOVEMENT_SPEED;
    public static final Holder<Attribute> ATTACK_DAMAGE = Attributes.ATTACK_DAMAGE;
    public static final Holder<Attribute> ARMOR = Attributes.ARMOR;
    public static final Holder<Attribute> ARMOR_TOUGHNESS = Attributes.ARMOR_TOUGHNESS;
}