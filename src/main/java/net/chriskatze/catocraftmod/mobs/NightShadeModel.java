package net.chriskatze.catocraftmod.mobs;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NightShadeModel<T extends LivingEntity> extends HierarchicalModel<T> {
    public NightShadeModel() {
        super(RenderType::entityCutout);
    }

    @Override
    public ModelPart root() {
        return null;
    }

    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {
    }
}
