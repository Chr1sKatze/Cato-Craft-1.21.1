package net.chriskatze.catocraftmod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.capability.util.EquipmentUtils;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.chriskatze.catocraftmod.menu.layout.SlotEquipValidator;
import net.chriskatze.catocraftmod.network.EquipmentSyncHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
/**
 * 🔹 Base class for all equippable items that use the unified equipment system.
 *
 * Handles:
 *  - Equip / Unequip (right-click or shift-right-click)
 *  - Slot validation & dependency rules via {@link SlotEquipValidator}
 *  - Automatic attribute application & synchronization
 *  - Sound feedback and optional polish hooks
 *
 * Subclasses must:
 *  ✅ Implement {@link #resolveGroupId(ItemStack)}
 *  ✅ Optionally override {@link #getTooltipTitleKey()} for custom tooltip section titles
 */
public abstract class EquippableItemBase extends Item {

    private final ImmutableMultimap<Holder<Attribute>, AttributeModifier> attributeModifiers;

    protected EquippableItemBase(Properties properties, ImmutableMultimap<Holder<Attribute>, AttributeModifier> modifiers) {
        super(properties);
        this.attributeModifiers = modifiers == null ? ImmutableMultimap.of() : modifiers;
    }

    protected EquippableItemBase(Properties properties) {
        this(properties, ImmutableMultimap.of());
    }

    /** Must return the lowercase equipment group key, e.g. "earrings" or "soulstones". */
    protected abstract String resolveGroupId(ItemStack stack);

    /** Override to customize the title in tooltips (default: generic stat section). */
    protected String getTooltipTitleKey() {
        return "item.modifiers.catocraftmod.generic";
    }

    /** Exposes attribute modifiers for this item (used by capability). */
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers() {
        return attributeModifiers;
    }

    // ────────────────────────────────────────────────
    // Equip / Unequip Core Logic
    // ────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.pass(stack);

        PlayerEquipmentCapability cap = EquipmentCapabilityHandler.get(player);
        if (cap == null) return InteractionResultHolder.pass(stack);

        EquipmentGroup group = EquipmentGroup.fromKey(resolveGroupId(stack));
        if (group == null) {
            player.displayClientMessage(Component.literal("Unknown equipment group!").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        ItemStackHandler handler = cap.getAllGroups().get(group);
        if (handler == null) {
            player.displayClientMessage(Component.literal("No handler for group '" + group.getKey() + "'.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        int slotIndex = EquipmentUtils.findFirstValidSlot(cap.getAllGroups(), group);
        if (slotIndex == -1) {
            player.displayClientMessage(Component.literal("No valid slot available!").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        ItemStack equipped = handler.getStackInSlot(slotIndex);

        // ────────────── Shift + Right-Click → Unequip ──────────────
        if (player.isShiftKeyDown()) {
            if (!equipped.isEmpty()) {
                unequipItem(player, cap, handler, slotIndex, equipped);
                return InteractionResultHolder.success(stack);
            } else {
                player.displayClientMessage(Component.literal("No item equipped in that slot.").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        // ────────────── Regular Right-Click → Equip ──────────────
        if (equipped.isEmpty()) {
            if (!SlotEquipValidator.canEquip((ServerPlayer) player, group, cap.getEquippedGroups())) {
                player.displayClientMessage(Component.literal("Cannot equip — conflicts or requirements not met.").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }

            if (!SlotEquipValidator.canEquipItem((ServerPlayer) player, group, stack)) {
                player.displayClientMessage(Component.literal("This item cannot be equipped in that slot.").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }

            equipItem(player, cap, handler, slotIndex, stack);
            stack.shrink(1);
            return InteractionResultHolder.success(stack);
        } else {
            player.displayClientMessage(Component.literal("That slot is already occupied.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
    }

    // ────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────

    private void equipItem(Player player, PlayerEquipmentCapability cap, ItemStackHandler handler, int slot, ItemStack stack) {
        handler.setStackInSlot(slot, stack.copy());
        cap.applyAllAttributes();
        EquipmentSyncHelper.syncToClient((ServerPlayer) player);

        playEquipSound(player);
        player.displayClientMessage(Component.literal("Equipped item!").withStyle(ChatFormatting.GRAY), true);
        CatocraftMod.LOGGER.debug("[EquippableItemBase] {} equipped {} in group {}.", player.getName().getString(), stack.getItem(), handler);
    }

    private void unequipItem(Player player, PlayerEquipmentCapability cap, ItemStackHandler handler, int slot, ItemStack equipped) {
        boolean added = player.getInventory().add(equipped.copy());
        if (!added) player.drop(equipped.copy(), false);
        handler.setStackInSlot(slot, ItemStack.EMPTY);

        cap.applyAllAttributes();
        EquipmentSyncHelper.syncToClient((ServerPlayer) player);

        playUnequipSound(player);
        player.displayClientMessage(Component.literal("Unequipped item.").withStyle(ChatFormatting.GRAY), true);
        CatocraftMod.LOGGER.debug("[EquippableItemBase] {} unequipped {}.", player.getName().getString(), equipped.getItem());
    }

    /** Plays a subtle equip sound (client-safe). */
    protected void playEquipSound(Player player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 0.6f, 1.1f);
    }

    protected void playUnequipSound(Player player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.5f, 0.9f);
    }

    // ────────────────────────────────────────────────
    // Tooltip Rendering
    // ────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (!attributeModifiers.isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable(getTooltipTitleKey()).withStyle(ChatFormatting.GRAY));

            attributeModifiers.forEach((attr, mod) -> {
                double amount = mod.amount();
                boolean positive = amount > 0;
                String sign = positive ? "+" : "";
                Component attrName = Component.translatable(attr.value().getDescriptionId());
                ChatFormatting color = positive ? ChatFormatting.BLUE : ChatFormatting.RED;

                tooltip.add(Component.literal("  " + sign + (amount % 1 == 0 ? (int) amount : String.format("%.2f", amount)) + " ")
                        .append(attrName).withStyle(color));
            });
        }
    }
}