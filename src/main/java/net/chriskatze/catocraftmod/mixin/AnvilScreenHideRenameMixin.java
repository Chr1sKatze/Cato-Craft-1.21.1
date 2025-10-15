package net.chriskatze.catocraftmod.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenHideRenameMixin {

    @Redirect(
            method = "renderBg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private void skipRenameSprite(GuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height) {
        String path = sprite.getPath();
        if (!path.equals("container/anvil/text_field") &&
                !path.equals("container/anvil/text_field_disabled")) {
            instance.blitSprite(sprite, x, y, width, height);
        }
    }
}