package net.chriskatze.catocraftmod.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenDisableRenameMixin {

    @Inject(method = "subInit", at = @At("TAIL"))
    private void disableRenameBox(CallbackInfo ci) {
        try {
            Field nameField = AnvilScreen.class.getDeclaredField("name");
            nameField.setAccessible(true);
            Object editBox = nameField.get(this);
            if (editBox instanceof EditBox e) {
                e.setEditable(false);
                e.setVisible(false);
            }
        } catch (Exception ignored) {}
    }
}