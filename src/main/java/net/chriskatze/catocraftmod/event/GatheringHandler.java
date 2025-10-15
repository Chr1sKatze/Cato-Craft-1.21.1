package net.chriskatze.catocraftmod.event;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class GatheringHandler {

    @SubscribeEvent
    public static void onBlockBreakSpeed(PlayerEvent.BreakSpeed event) {
        var player = event.getEntity();
        ItemStack tool = player.getMainHandItem();

        // ✅ Check if the tool is tagged for Gathering Speed
        if (!tool.is(ModTags.GATHERING_ITEMS)) {
            return;
        }

        // Get the enchantment holder
        Holder<Enchantment> gatheringEnchantment = player.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(ModEnchantments.GATHERING.getKey());

        int level = EnchantmentHelper.getItemEnchantmentLevel(gatheringEnchantment, tool);

        if (level > 0) {
            float baseSpeed = event.getOriginalSpeed();

            // Example: scale the bonus multiplicatively for better feel
            float newSpeed = baseSpeed * (1.0f + (0.2f * level));

            event.setNewSpeed(newSpeed);
        }
    }
}