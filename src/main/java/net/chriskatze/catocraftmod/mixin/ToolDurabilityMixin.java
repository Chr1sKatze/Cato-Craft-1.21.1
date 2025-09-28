package net.chriskatze.catocraftmod.mixin;

import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Tiers.class)
public class ToolDurabilityMixin {

    // Durability
    @Inject(method = "getUses", at = @At("HEAD"), cancellable = true)
    private void changeDurability(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == Tiers.WOOD) {
            cir.setReturnValue(16); // vanilla: 59
        } else if ((Object) this == Tiers.STONE) {
            cir.setReturnValue(64); // vanilla: 131
        } else if ((Object) this == Tiers.IRON) {
            cir.setReturnValue(128); // vanilla: 250
        } else if ((Object) this == Tiers.GOLD) {
            cir.setReturnValue(96); // vanilla: 32
        } else if ((Object) this == Tiers.DIAMOND) {
            cir.setReturnValue(1200); // vanilla: 1561
        } else if ((Object) this == Tiers.NETHERITE) {
            cir.setReturnValue(1800); // vanilla: 2031
        }
    }

    // Enchantability
    @Inject(method = "getEnchantmentValue", at = @At("HEAD"), cancellable = true)
    private void changeEnchantability(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == Tiers.WOOD || (Object) this == Tiers.STONE || (Object) this == Tiers.IRON) {
            cir.setReturnValue(0); // not enchantable
        }
    }

    // Mining Speed
    @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)
    private void changeMiningSpeed(CallbackInfoReturnable<Float> cir) {
        if ((Object) this == Tiers.WOOD) {
            cir.setReturnValue(1.0f); // vanilla: 2.0
        } else if ((Object) this == Tiers.STONE) {
            cir.setReturnValue(2.00f); // vanilla: 4.0
        } else if ((Object) this == Tiers.IRON) {
            cir.setReturnValue(5.0f); // vanilla: 6.0
        } else if ((Object) this == Tiers.GOLD) {
            cir.setReturnValue(2.0f); // vanilla: 6.0
        } else if ((Object) this == Tiers.DIAMOND) {
            cir.setReturnValue(7.0f); // vanilla: 8.0
        }
    }
}