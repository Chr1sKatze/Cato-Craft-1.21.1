package net.chriskatze.catocraftmod.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ModTooltips {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();

        // Only show tooltip for pickaxes, axes, shovels, and hoes
        if (item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem || item instanceof HoeItem) {
            TieredItem tiered = (TieredItem) item;

            int maxDurability = tiered.getMaxDamage(stack);
            int damageTaken = stack.getDamageValue();
            int remainingDurability = maxDurability - damageTaken;
            float miningSpeed = tiered.getTier().getSpeed();
            int enchantability = tiered.getTier().getEnchantmentValue();

            // Add a separation line **just above our custom tooltip**
            event.getToolTip().add(Component.literal("———————————")
                    .withStyle(style -> style.withColor(TextColor.fromRgb(0xAAAAAA)))); // light gray

            // Durability (dark green like vanilla)
            event.getToolTip().add(Component.literal(" Durability: " + remainingDurability + " / " + maxDurability)
                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x00AA00)))); // dark green

            // Mining speed (dark green)
            event.getToolTip().add(Component.literal(String.format(" Gathering Speed: %.2f", miningSpeed))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x00AA00)))); // dark green

            // Only show enchantable if enchantability > 0, in cyan
            if (enchantability > 0) {
                event.getToolTip().add(Component.literal(" Enchantable")
                        .withStyle(style -> style.withColor(TextColor.fromRgb(0x00FFFF)))); // cyan
            }
        }
    }
}