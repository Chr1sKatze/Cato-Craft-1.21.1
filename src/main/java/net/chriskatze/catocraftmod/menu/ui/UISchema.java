package net.chriskatze.catocraftmod.menu.ui;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * JSON-driven UI definition for DynamicScreen.
 *
 * Example:
 * {
 *   "width": 194,
 *   "height": 182,
 *   "background": "catocraftmod:textures/gui/equipment_bg.png",
 *   "background_color": "#202020",
 *   "background_gradient": ["#303030", "#101010"],
 *   "elements": [...]
 * }
 */
public record UISchema(
        int width,
        int height,
        ResourceLocation background,
        String background_color,
        List<String> background_gradient,
        List<UIElement> elements
) {}