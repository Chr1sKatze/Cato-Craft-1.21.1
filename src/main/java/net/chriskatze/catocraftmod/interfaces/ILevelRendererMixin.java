package net.chriskatze.catocraftmod.interfaces;

import net.minecraft.core.BlockPos;

public interface ILevelRendererMixin {
    void setBlockDirty(BlockPos pos);
}
