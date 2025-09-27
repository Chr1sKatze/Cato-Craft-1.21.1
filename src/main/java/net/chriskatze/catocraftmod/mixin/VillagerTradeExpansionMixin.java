package net.chriskatze.catocraftmod.mixin;

import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Villager.class)
public class VillagerTradeExpansionMixin {
    @ModifyConstant(
            method = "updateTrades",
            constant = @Constant(intValue = 2) // replaces the hardcoded 2
    )
    private int replaceTradesPerLevel(int original) {
        return 4; // 4 trades per level
    }
}