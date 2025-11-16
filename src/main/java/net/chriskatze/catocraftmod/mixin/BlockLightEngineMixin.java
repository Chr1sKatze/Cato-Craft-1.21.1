package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.interfaces.IBlockLightEngine;
import net.chriskatze.catocraftmod.interfaces.IBlockLightSectionStorageMixin;
import net.chriskatze.catocraftmod.interfaces.ILightEngineMixin;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin implements IBlockLightEngine, ILightEngineMixin {

    @Override
    public void setLightLevel(long levelPos, int lightLevel) {
        ((IBlockLightSectionStorageMixin) this.getAnyStorage()).setLightLevel(levelPos, lightLevel);
    }
}
