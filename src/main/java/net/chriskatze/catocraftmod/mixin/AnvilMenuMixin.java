package net.chriskatze.catocraftmod.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow @Final private DataSlot cost;

    // Override the value the GUI reads
    @Inject(method = "getCost", at = @At("HEAD"), cancellable = true)
    private void showZeroCost(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void allowFreePickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        if (!player.getAbilities().instabuild) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"))
    private void zeroXpCostEarly(CallbackInfo ci) {
        this.cost.set(0);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void preventXpLoss(Player player, ItemStack stack, CallbackInfo ci) {
        // XP deduction is effectively bypassed
    }
}