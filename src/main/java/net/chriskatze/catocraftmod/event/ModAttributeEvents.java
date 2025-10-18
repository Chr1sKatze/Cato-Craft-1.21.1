package net.chriskatze.catocraftmod.event;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.util.ModAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Registers CatoCraft custom attributes onto all living entities (including players and mobs).
 */
public class ModAttributeEvents {

    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        int count = 0;

        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            // --- Offensive Power ---
            if (!event.has(type, ModAttributes.FIRE_POWER))
                event.add(type, ModAttributes.FIRE_POWER);
            if (!event.has(type, ModAttributes.FROST_POWER))
                event.add(type, ModAttributes.FROST_POWER);
            if (!event.has(type, ModAttributes.ARCANE_POWER))
                event.add(type, ModAttributes.ARCANE_POWER);

            // --- Resistances ---
            if (!event.has(type, ModAttributes.FIRE_RESIST))
                event.add(type, ModAttributes.FIRE_RESIST);
            if (!event.has(type, ModAttributes.FROST_RESIST))
                event.add(type, ModAttributes.FROST_RESIST);
            if (!event.has(type, ModAttributes.ARCANE_RESIST))
                event.add(type, ModAttributes.ARCANE_RESIST);

            // --- Utility ---
            if (!event.has(type, ModAttributes.HEALING_POWER))
                event.add(type, ModAttributes.HEALING_POWER);
            if (!event.has(type, ModAttributes.MANA_REGEN))
                event.add(type, ModAttributes.MANA_REGEN);

            count++;
        }

        CatocraftMod.LOGGER.info("[ModAttributes] Attached custom attributes to {} living entity types.", count);
    }
}