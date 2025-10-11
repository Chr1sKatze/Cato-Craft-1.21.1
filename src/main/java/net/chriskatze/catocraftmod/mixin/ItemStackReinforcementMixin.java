package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackReinforcementMixin {

    @Shadow public abstract int getDamageValue();
    @Shadow public abstract void setDamageValue(int damage);
    @Shadow public abstract int getMaxDamage();

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void applyReinforcement(int amount, ServerLevel level, @Nullable LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        if (entity == null || amount <= 0) return;

        ItemStack stack = (ItemStack) (Object) this;

        Holder<Enchantment> reinforcementHolder = (Holder<Enchantment>) ModEnchantments.REINFORCEMENT.getHolder();
        if (reinforcementHolder == null) return;

        int reinforcementLevel = EnchantmentHelper.getItemEnchantmentLevel(reinforcementHolder, stack);
        if (reinforcementLevel <= 0) return;

        RandomSource random = entity.getRandom();
        int remainingDamage = 0;

        // Each level = 5% chance per damage point to ignore
        for (int i = 0; i < amount; i++) {
            if (random.nextInt(100) >= reinforcementLevel * 5) {
                remainingDamage++;
            }
        }

        if (remainingDamage <= 0) {
            ci.cancel(); // all damage ignored
            return;
        }

        // Apply only the remaining damage manually (no recursion)
        int newDamage = getDamageValue() + remainingDamage;
        if (newDamage >= getMaxDamage()) {
            // trigger break callback
            onBreak.accept(stack.getItem());
            stack.shrink(1); // destroy the item
        } else {
            setDamageValue(newDamage);
        }

        ci.cancel(); // prevent vanilla duplicate handling
    }
}