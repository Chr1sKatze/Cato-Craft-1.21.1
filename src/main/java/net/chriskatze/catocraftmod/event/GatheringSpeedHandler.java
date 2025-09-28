package net.chriskatze.catocraftmod.event;

import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class GatheringSpeedHandler {

    @SubscribeEvent
    public static void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        // Get the player via getEntity()
        var player = event.getEntity();

        ItemStack tool = player.getMainHandItem();

        // Use player.level() method to access the world
        Holder<Enchantment> gatheringSpeedEnchantment = player.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.GATHERING_SPEED.getKey());

        int level = EnchantmentHelper.getItemEnchantmentLevel(gatheringSpeedEnchantment, tool);

        if (level > 0) {
            event.setNewSpeed(event.getOriginalSpeed() + 1.0f); // flat +1 mining speed
        }
    }
}