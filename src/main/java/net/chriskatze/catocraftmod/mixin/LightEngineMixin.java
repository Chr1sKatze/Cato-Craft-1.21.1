package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.interfaces.ILightEngineMixin;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> implements ILightEngineMixin {

    @Accessor("storage")
    abstract S getStorage();

    @Override
    public Object getAnyStorage() {
        return getStorage();
    }

}
