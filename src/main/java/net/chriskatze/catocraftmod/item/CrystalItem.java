package net.chriskatze.catocraftmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CrystalItem extends Item {

    private final int level;
    private final ResourceLocation enchantmentId;

    /**
     * Generic Crystal constructor.
     * @param level Level of the crystal (e.g., 1)
     * @param enchantmentId Enchantment ID this crystal applies
     * @param properties Item properties
     */
    public CrystalItem(int level, ResourceLocation enchantmentId, Properties properties) {
        super(properties);
        this.level = level;
        this.enchantmentId = enchantmentId;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Always glows
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Tooltip just shows the display name from lang file + optional description
        tooltip.add(Component.literal("  Level " + level).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
    }

    public int getLevel() {
        return level;
    }

    public ResourceLocation getEnchantmentId() {
        return enchantmentId;
    }
}