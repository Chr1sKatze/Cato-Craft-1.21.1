package net.chriskatze.catocraftmod.combat;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.util.ModAttributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Unified elemental damage calculator & applier.
 * Uses attacker’s elemental power and target’s resistances.
 */
public final class SpellDamage {
    private SpellDamage() {}

    /**
     * Computes scaled elemental damage using power & resist.
     */
    public static float computeFinalDamage(LivingEntity attacker, LivingEntity target, Element element, float base) {
        double power = getPower(attacker, element);
        double resist = getResist(target, element);

        // Clamp resist between -0.9 and +0.9 (never heals / never full immunity)
        double clampedResist = Math.max(-0.9, Math.min(0.9, resist));

        // Final = base * (1 + power/100) * (1 - resist)
        double scaled = base * (1.0 + power / 100.0) * (1.0 - clampedResist);
        return (float) Math.max(0.1, scaled);
    }

    /**
     * Applies the computed damage using the correct damage source.
     */
    public static boolean hurtElemental(LivingEntity attacker, LivingEntity target, Element element, float base) {
        if (attacker == null || target == null || target.level().isClientSide) return false;
        float amount = computeFinalDamage(attacker, target, element, base);

        DamageSource source = makeDamageSource(attacker, target, element);
        boolean success = target.hurt(source, amount);

        CatocraftMod.LOGGER.debug("[SpellDamage] {} dealt {} {} dmg to {} (power={}, resist={})",
                attacker.getName().getString(), amount, element.name().toLowerCase(),
                target.getName().getString(),
                getPower(attacker, element), getResist(target, element));

        return success;
    }

    // -------------------------------------------------------------
    // Internal utilities
    // -------------------------------------------------------------
    private static double getPower(LivingEntity entity, Element element) {
        return switch (element) {
            case FIRE -> getAttrValue(entity, ModAttributes.FIRE_POWER_HOLDER);
            case FROST -> getAttrValue(entity, ModAttributes.FROST_POWER_HOLDER);
            case ARCANE -> getAttrValue(entity, ModAttributes.ARCANE_POWER_HOLDER);
        };
    }

    private static double getResist(LivingEntity entity, Element element) {
        return switch (element) {
            case FIRE -> getAttrValue(entity, ModAttributes.FIRE_RESIST_HOLDER);
            case FROST -> getAttrValue(entity, ModAttributes.FROST_RESIST_HOLDER);
            case ARCANE -> getAttrValue(entity, ModAttributes.ARCANE_RESIST_HOLDER);
        };
    }

    private static double getAttrValue(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr) {
        if (attr == null) return 0.0;
        var instance = entity.getAttribute(attr);
        return instance != null ? instance.getValue() : 0.0;
    }

    private static DamageSource makeDamageSource(LivingEntity attacker, LivingEntity target, Element element) {
        if (!(target.level() instanceof ServerLevel server)) return null;
        return switch (element) {
            case FIRE -> server.damageSources().source(DamageTypes.ON_FIRE, attacker);
            case FROST -> server.damageSources().source(DamageTypes.FREEZE, attacker);
            case ARCANE -> server.damageSources().source(DamageTypes.MAGIC, attacker);
        };
    }
}