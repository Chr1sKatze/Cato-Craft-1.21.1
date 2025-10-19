package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.interfaces.ILayerLightSectionStorageMixin;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageMixin<M extends DataLayerStorageMap<M>> implements ILayerLightSectionStorageMixin {

    @Shadow
    abstract void setStoredLevel(long levelPos, int lightLevel);

    @Shadow
    abstract DataLayer getDataLayer(long sectionPos, boolean cached);

    @Override
    public DataLayer getLightDataLayer(long sectionPos, boolean cached) {
        return getDataLayer(sectionPos, cached);
    }

    @Override
    public void setLightLevel(long levelPos, int lightLevel) {
        setStoredLevel(levelPos, lightLevel);
    }

}
