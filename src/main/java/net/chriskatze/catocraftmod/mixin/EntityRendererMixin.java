package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.render.ModRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void onGetBlockLight(T entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int vanilla = cir.getReturnValueI();
        int entityLuminance = 0; //((EntityLightSource) entity).getLuminance();
//        if (entityLuminance >= 15)
//            cir.setReturnValue(entityLuminance);
        Integer level = ModRenderer.savedLightLevels.get(pos.asLong());
        if (level == null) return;

        int lightLevel = Math.max(Math.max(0, level), Math.max(vanilla, entityLuminance ));
        cir.setReturnValue(lightLevel);
    }
}
