package net.chriskatze.catocraftmod.interfaces;

import net.minecraft.world.level.chunk.DataLayer;

public interface ILayerLightSectionStorageMixin {
    void setLightLevel(long levelPos, int lightLevel);
    DataLayer getLightDataLayer(long sectionPos, boolean cached);
}
