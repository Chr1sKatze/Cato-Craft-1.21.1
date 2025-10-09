package net.chriskatze.catocraftmod.mixin;

import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ApplyBonusCount.class)
public abstract class ApplyBonusCountMixin {

    @Inject(
            method = "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void adjustProsperity(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        System.out.println("[ProsperityMixin] Called ApplyBonusCount.run for stack: " + stack);

        // Get the tool used
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool == null) {
            System.out.println("[ProsperityMixin] Tool is null, cannot apply Prosperity.");
            return;
        }

        // Fetch Prosperity holder from live registry
        Holder<Enchantment> prosperityHolder;
        try {
            prosperityHolder = context.getLevel()
                    .registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(ModEnchantments.PROSPERITY.getKey());
        } catch (Exception e) {
            System.out.println("[ProsperityMixin] Could not fetch Prosperity holder: " + e);
            return;
        }

        // Check if this ApplyBonusCount instance is for Prosperity
        if (!((ApplyBonusCount)(Object)this).getType().equals(stack.getItem().getDefaultInstance().getItem())) {
            // This is not strictly necessary; we want to apply whenever the tool has Prosperity
        }

        // Get enchantment level
        int prosperityLevel = EnchantmentHelper.getItemEnchantmentLevel(prosperityHolder, tool);
        if (prosperityLevel <= 0) {
            System.out.println("[ProsperityMixin] Tool has no Prosperity enchantment.");
            return;
        }

        System.out.println("[ProsperityMixin] Prosperity level: " + prosperityLevel);

        RandomSource random = context.getRandom();
        int count = stack.getCount();

        // 5% per level, capped at 100% chance
        float bonusChance = Math.min(0.05f * prosperityLevel, 1.0f);
        System.out.println("[ProsperityMixin] Calculated bonus chance: " + bonusChance);

        if (random.nextFloat() < bonusChance) {
            count++;
            System.out.println("[ProsperityMixin] Extra drop applied!");
        } else {
            System.out.println("[ProsperityMixin] No extra drop this time.");
        }

        stack.setCount(count);
        cir.setReturnValue(stack);

        System.out.println("[ProsperityMixin] Final drop count: " + count);
    }
}