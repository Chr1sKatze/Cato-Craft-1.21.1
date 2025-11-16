package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.interfaces.ILevelRendererMixin;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements ILevelRendererMixin {
    @Shadow abstract void setBlockDirty(BlockPos pos, boolean reRenderOnMainThread);

    @Override
    public void setBlockDirty(BlockPos pos) {
        this.setBlockDirty(pos, false);
    }
}
