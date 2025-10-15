package net.chriskatze.catocraftmod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base item class for jewellery like earrings, rings, necklaces, etc.
 * Stores attribute bonuses and renders vanilla-style "When equipped" tooltips.
 */
public class JewelleryItem extends Item {

    private final ImmutableMultimap<Holder<Attribute>, AttributeModifier> attributeModifiers;

    public JewelleryItem(Properties properties, ImmutableMultimap<Holder<Attribute>, AttributeModifier> modifiers) {
        super(properties);
        this.attributeModifiers = modifiers == null ? ImmutableMultimap.of() : modifiers;
    }

    public JewelleryItem(Properties properties) {
        this(properties, ImmutableMultimap.of());
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getJewelleryModifiers() {
        return attributeModifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (!attributeModifiers.isEmpty()) {
            tooltip.add(Component.empty()); // blank line before bonuses

            // 👑 This line mimics vanilla armor "When on <slot>"
            tooltip.add(Component.translatable("item.modifiers.catocraftmod.earring")
                    .withStyle(ChatFormatting.GRAY));

            // Each attribute line (e.g., "+4 Max Health")
            for (var entry : attributeModifiers.entries()) {
                var attribute = entry.getKey();
                var modifier = entry.getValue();

                double amount = modifier.amount();
                boolean positive = amount > 0;
                String sign = positive ? "+" : "";

                // Localized name (e.g., "Max Health", "Armor Toughness")
                Component attrName = Component.translatable(attribute.value().getDescriptionId());
                ChatFormatting color = positive ? ChatFormatting.BLUE : ChatFormatting.RED;

                tooltip.add(Component.literal("  " + sign + (amount % 1 == 0 ? (int) amount : String.format("%.2f", amount)) + " ")
                        .append(attrName)
                        .withStyle(color));
            }
        }
    }
}