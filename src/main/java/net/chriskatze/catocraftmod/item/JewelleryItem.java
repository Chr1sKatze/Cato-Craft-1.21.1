package net.chriskatze.catocraftmod.item;

import com.google.common.collect.ImmutableMultimap;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Specific equippable for jewellery (earrings, rings, necklaces).
 */
public class JewelleryItem extends EquippableItemBase {

    public JewelleryItem(Properties properties, ImmutableMultimap<Holder<Attribute>, AttributeModifier> modifiers) {
        super(properties, modifiers);
    }

    public JewelleryItem(Properties properties) {
        super(properties);
    }

    @Override
    protected String resolveGroupId(ItemStack stack) {
        EquipmentGroup group = EquipmentGroup.fromStack(stack);
        return group == null ? "earrings" : group.getKey();
    }

    @Override
    protected String getTooltipTitleKey() {
        return "item.modifiers.catocraftmod.earring";
    }
}