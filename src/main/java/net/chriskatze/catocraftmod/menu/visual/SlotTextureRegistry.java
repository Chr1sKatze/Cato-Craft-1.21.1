package net.chriskatze.catocraftmod.menu.visual;


import net.minecraft.resources.ResourceLocation;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.layout.SlotType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Central registry for slot textures used in DynamicMenuScreen and MenuEditorScreen.
 *
 * 🧠 Design:
 *  - One 18×18 texture per SlotType (kept simple, consistent with editor)
 *  - All stored under: assets/<modid>/textures/gui/slots/
 *  - Fallback: default gray slot if missing
 *
 * Example folder structure:
 *  └─ textures/gui/slots/
 *      ├─ default_slot.png
 *      ├─ inventory_slot.png
 *      ├─ hotbar_slot.png
 *      ├─ armor_slot.png
 *      ├─ offhand_slot.png
 *      ├─ crafting_slot.png
 *      ├─ crafting_result_slot.png
 *      ├─ ingredient_slot.png
 *      ├─ ingredient_result_slot.png
 *      └─ jewellery_slot.png
 */
public final class SlotTextureRegistry {

    private static final Map<SlotType, ResourceLocation> TEXTURES = new EnumMap<>(SlotType.class);

    // Default fallback texture
    private static final ResourceLocation DEFAULT_SLOT =
            ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "textures/gui/slots/default_slot.png");

    static {
        // ────────────── Core Player Inventory ──────────────
        register(SlotType.INVENTORY, "inventory_slot");
        register(SlotType.HOTBAR, "hotbar_slot");

        // ────────────── Equipment ──────────────
        register(SlotType.ARMOR, "armor_slot");
        register(SlotType.OFF_HAND, "offhand_slot");

        // ────────────── Crafting ──────────────
        register(SlotType.CRAFTING, "crafting_slot");
        register(SlotType.CRAFTING_RESULT, "crafting_result_slot");

        // ────────────── Ingredient / Alchemy ──────────────
        register(SlotType.INGREDIENT, "ingredient_slot");
        register(SlotType.INGREDIENT_RESULT, "ingredient_result_slot");

        // ────────────── Custom / Modded Systems ──────────────
        register(SlotType.JEWELLERY, "jewellery_slot");
    }

    private SlotTextureRegistry() {}

    private static void register(SlotType type, String fileName) {
        TEXTURES.put(type, ResourceLocation.fromNamespaceAndPath(
                CatocraftMod.MOD_ID, "textures/gui/slots/" + fileName + ".png"
        ));
    }

    /** Returns the texture for the given slot type, or the fallback default. */
    public static ResourceLocation get(SlotType type) {
        return TEXTURES.getOrDefault(type, DEFAULT_SLOT);
    }
}