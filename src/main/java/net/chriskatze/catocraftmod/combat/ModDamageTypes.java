package net.chriskatze.catocraftmod.combat;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Clean custom damage type registry for spell damage.
 * Matches data/catocraftmod/damage_type/*.json
 */
public final class ModDamageTypes {
    private ModDamageTypes() {}

    // Direct registry keys (simpler than nested lookups)
    public static final ResourceKey<DamageType> FIRE_SPELL =
            ResourceKey.create(Registries.DAMAGE_TYPE, CatocraftMod.id("fire_spell"));
    public static final ResourceKey<DamageType> FROST_SPELL =
            ResourceKey.create(Registries.DAMAGE_TYPE, CatocraftMod.id("frost_spell"));
    public static final ResourceKey<DamageType> ARCANE_SPELL =
            ResourceKey.create(Registries.DAMAGE_TYPE, CatocraftMod.id("arcane_spell"));

    /**
     * Utility: Create a DamageSource directly from a key and attacker.
     */
    public static DamageSource source(ServerLevel level, ResourceKey<DamageType> key, LivingEntity attacker) {
        var registry = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE);

        // new API: use get() and unwrap the Optional
        var holder = registry.get(key)
                .orElseThrow(() -> new IllegalStateException("Missing damage type: " + key.location()));

        return new DamageSource(holder, attacker);
    }

    /**
     * Element-based helper for use in SpellDamage.
     */
    public static DamageSource elemental(ServerLevel level, LivingEntity attacker, Element element) {
        return switch (element) {
            case FIRE -> source(level, FIRE_SPELL, attacker);
            case FROST -> source(level, FROST_SPELL, attacker);
            case ARCANE -> source(level, ARCANE_SPELL, attacker);
        };
    }
}