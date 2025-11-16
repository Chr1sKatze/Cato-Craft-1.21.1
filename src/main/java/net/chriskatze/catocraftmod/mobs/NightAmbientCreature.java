package net.chriskatze.catocraftmod.mobs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;

public class NightAmbientCreature extends AmbientCreature {

    public NightAmbientCreature(EntityType<? extends Bat> entityType, Level level) {
        super(entityType, level);
    }
}
