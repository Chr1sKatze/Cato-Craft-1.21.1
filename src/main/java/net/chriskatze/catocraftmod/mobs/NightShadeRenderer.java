package net.chriskatze.catocraftmod.mobs;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NightShadeRenderer extends MobRenderer<NightShade, NightShadeModel<NightShade>> {

    //  ModEntities.bakeTexture(context, NightShade.TEXTURE, "main")

    public NightShadeRenderer(EntityRendererProvider.Context context) {
        super(context, new NightShadeModel<>(), 1.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(NightShade entity) {
        return NightShade.TEXTURE;
    }
}
