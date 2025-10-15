package net.chriskatze.catocraftmod.mixin;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenHideLabelsMixin {

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void hideLabels(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        // Cancel the method, preventing all foreground labels from being drawn
        ci.cancel();
    }
}