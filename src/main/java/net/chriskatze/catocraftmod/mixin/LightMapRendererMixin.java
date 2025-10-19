package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.interfaces.LightmapAccess;
import net.chriskatze.catocraftmod.manager.Darkness;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
@Mixin(GameRenderer.class)
public class LightMapRendererMixin {
    @Shadow
    private Minecraft minecraft;
    @Shadow
    private LightTexture lightTexture;

    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    private void renderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        final LightmapAccess lightmap = (LightmapAccess) lightTexture;
        final GameRenderer renderer = (GameRenderer) (Object) this;
        if (lightmap.darkness_isDirty()) {
            minecraft.getProfiler().push("lightTex");
            Darkness.updateLuminance(deltaTracker.getGameTimeDeltaTicks(), minecraft, renderer, lightmap.darkness_prevFlicker());
            minecraft.getProfiler().pop();
        }
    }
}
