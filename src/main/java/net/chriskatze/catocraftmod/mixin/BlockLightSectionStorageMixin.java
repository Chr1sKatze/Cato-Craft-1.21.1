package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.interfaces.IBlockLightSectionStorageMixin;
import net.chriskatze.catocraftmod.interfaces.ILayerLightSectionStorageMixin;
import net.chriskatze.catocraftmod.render.ModRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLightSectionStorage.class)
public abstract class BlockLightSectionStorageMixin implements IBlockLightSectionStorageMixin {

    @Override
    public void setLightLevel(long levelPos, int lightLevel) {
        long i = SectionPos.blockToSection(levelPos);
        DataLayer datalayer = ((ILayerLightSectionStorageMixin) this).getLightDataLayer(i, false);
        if (datalayer == null) return;
        datalayer.set(SectionPos.sectionRelative(BlockPos.getX(levelPos)), SectionPos.sectionRelative(BlockPos.getY(levelPos)), SectionPos.sectionRelative(BlockPos.getZ(levelPos)), lightLevel);
    }

    @Inject(method = "getLightValue", at = @At("HEAD"), cancellable = true)
    protected void getLightValue(long levelPos, CallbackInfoReturnable<Integer> cir) {
        int vanilla = cir.getReturnValueI();
        Integer lightLevel = ModRenderer.savedLightLevels.get(levelPos);
        if (lightLevel != null && lightLevel > vanilla) {
            cir.setReturnValue(lightLevel);
        }
    }

}