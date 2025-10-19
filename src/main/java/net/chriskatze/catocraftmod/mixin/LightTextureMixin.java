package net.chriskatze.catocraftmod.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.chriskatze.catocraftmod.interfaces.LightmapAccess;
import net.chriskatze.catocraftmod.manager.Darkness;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mao
 * @implNote original from True-Darkness-Refabricated forked from grondag/darkness
 * @see https://github.com/HaXrDEV/True-Darkness-Refabricated
 */
@Mixin(LightTexture.class)
public class LightTextureMixin implements LightmapAccess {
    @Shadow
    private NativeImage lightPixels;
    @Shadow
    private float blockLightRedFlicker;
    @Shadow
    private boolean updateLightTexture;

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    private void onUpload(CallbackInfo ci) {
        if (Darkness.enabled && lightPixels != null) {
            for (int b = 0; b < 16; b++) {
                for (int s = 0; s < 16; s++) {
                    final int color = Darkness.darken(lightPixels.getPixelRGBA(b, s), b, s);
                    lightPixels.setPixelRGBA(b, s, color);
                }
            }
        }
    }

    @Override
    public float darkness_prevFlicker() {
        return blockLightRedFlicker;
    }

    @Override
    public boolean darkness_isDirty() {
        return updateLightTexture;
    }
}
