package net.chriskatze.catocraftmod.combat;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.util.ModAttributes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Applies elemental resistances (fire, frost, arcane)
 * to both vanilla and custom spell damage sources.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class ElementalResistanceHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        if (target.level().isClientSide()) return;

        Level level = target.level();

        // 🔥 FIRE Damage (lava, fireball, or custom fire spell)
        if (isFireDamage(source)) {
            double resist = target.getAttributeValue(ModAttributes.FIRE_RESIST);
            applyResistance(event, resist, "fire");
            return;
        }

        // ❄️ FROST Damage (freeze, custom frost spell)
        if (isFrostDamage(source)) {
            double resist = target.getAttributeValue(ModAttributes.FROST_RESIST);
            applyResistance(event, resist, "frost");
            return;
        }

        // 🪄 ARCANE Damage (magic, indirect magic, or custom arcane spell)
        if (isArcaneDamage(source)) {
            double resist = target.getAttributeValue(ModAttributes.ARCANE_RESIST);
            applyResistance(event, resist, "arcane");
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 Apply reduction safely
    // ---------------------------------------------------------------------
    private static void applyResistance(LivingIncomingDamageEvent event, double resist, String type) {
        double clamped = Math.max(-0.9, Math.min(0.9, resist)); // prevent healing/immunity
        float newAmount = (float) (event.getAmount() * (1.0 - clamped));

        if (newAmount != event.getAmount()) {
            event.setAmount(newAmount);
            CatocraftMod.LOGGER.debug("[Resistance] {} resist applied: new damage = {}", type, newAmount);
        }
    }

    // ---------------------------------------------------------------------
    // 🔹 Identify damage types (vanilla + custom)
    // ---------------------------------------------------------------------
    private static boolean isFireDamage(DamageSource src) {
        return src.is(DamageTypes.IN_FIRE)
                || src.is(DamageTypes.ON_FIRE)
                || src.is(DamageTypes.LAVA)
                || src.is(DamageTypes.HOT_FLOOR)
                || matches(src, ModDamageTypes.FIRE_SPELL);
    }

    private static boolean isFrostDamage(DamageSource src) {
        return src.is(DamageTypes.FREEZE)
                || matches(src, ModDamageTypes.FROST_SPELL);
    }

    private static boolean isArcaneDamage(DamageSource src) {
        return src.is(DamageTypes.MAGIC)
                || src.is(DamageTypes.INDIRECT_MAGIC)
                || matches(src, ModDamageTypes.ARCANE_SPELL);
    }

    // ---------------------------------------------------------------------
    // 🔹 Helper: checks if the damage source matches our custom type
    // ---------------------------------------------------------------------
    private static boolean matches(DamageSource src, ResourceKey<DamageType> key) {
        return src.is(key);
    }
}