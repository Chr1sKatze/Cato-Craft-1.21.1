package net.chriskatze.catocraftmod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Base item class for jewellery like earrings, rings, necklaces, etc.
 * Stores attribute bonuses and renders vanilla-style "When equipped" tooltips.
 * Also allows right-click to equip/unequip if used in hand.
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

    // 🪄 Right-click equip / Shift+Right-click unequip
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        var cap = player.getCapability(net.chriskatze.catocraftmod.capability.EarringCapabilityHandler.EARRING_CAP);
        if (cap == null) {
            return InteractionResultHolder.pass(stack);
        }

        ItemStack equipped = cap.getStackInSlot(0);

        // 🧷 Shift + Right-click → unequip
        if (player.isShiftKeyDown()) {
            if (!equipped.isEmpty()) {
                // Try to add the equipped item back to player inventory
                boolean added = player.getInventory().add(equipped.copy());
                if (!added) {
                    player.drop(equipped.copy(), false);
                }

                cap.setStackInSlot(0, ItemStack.EMPTY);
                cap.setChanged();

                net.chriskatze.catocraftmod.network.EarringSyncHelper.syncToClient((net.minecraft.server.level.ServerPlayer) player);
                net.chriskatze.catocraftmod.capability.EarringDataHandler.requestImmediateSave((net.minecraft.server.level.ServerPlayer) player);

                player.displayClientMessage(
                        Component.literal("Removed earring.")
                                .withStyle(ChatFormatting.GRAY),
                        true
                );
                return InteractionResultHolder.success(stack);
            } else {
                player.displayClientMessage(
                        Component.literal("You’re not wearing any earring.")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }
        }

        // 💍 Regular right-click → equip
        if (equipped.isEmpty()) {
            cap.setStackInSlot(0, stack.copy());
            cap.setChanged();

            // Sync + save
            net.chriskatze.catocraftmod.network.EarringSyncHelper.syncToClient((net.minecraft.server.level.ServerPlayer) player);
            net.chriskatze.catocraftmod.capability.EarringDataHandler.requestImmediateSave((net.minecraft.server.level.ServerPlayer) player);

            stack.shrink(1);

            player.displayClientMessage(
                    Component.literal("Equipped earring!")
                            .withStyle(ChatFormatting.GRAY),
                    true
            );

            return InteractionResultHolder.success(stack);
        } else {
            player.displayClientMessage(
                    Component.literal("You’re already wearing an earring.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }
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