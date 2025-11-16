package net.chriskatze.catocraftmod.util;

import net.minecraft.world.item.*;

public class ItemTypeHelper {

    public static boolean matchesType(Item item, String typeName) {
        if (item == null || typeName == null || typeName.isBlank()) return false;

        String type = typeName.toLowerCase();

        // 1️⃣ Check data-driven registry first
        Class<? extends Item> custom = ItemTypeRegistry.get(type);
        if (custom != null && custom.isInstance(item)) return true;

        // 2️⃣ Vanilla categories
        if (type.contains("weapon") || type.contains("sword"))
            return item instanceof SwordItem;

        if (type.contains("tool") || type.equals("pickaxe") || type.equals("axe") || type.equals("shovel") || type.equals("hoe"))
            return item instanceof PickaxeItem
                    || item instanceof AxeItem
                    || item instanceof ShovelItem
                    || item instanceof HoeItem;

        if (type.equals("armor") || type.startsWith("armor_"))
            return item instanceof ArmorItem;

        if (item instanceof ArmorItem armor) {
            return switch (type) {
                case "helmet", "head_armor" -> armor.getType() == ArmorItem.Type.HELMET;
                case "chestplate", "body_armor" -> armor.getType() == ArmorItem.Type.CHESTPLATE;
                case "leggings", "leg_armor" -> armor.getType() == ArmorItem.Type.LEGGINGS;
                case "boots", "feet_armor" -> armor.getType() == ArmorItem.Type.BOOTS;
                default -> false;
            };
        }

        return false;
    }
}