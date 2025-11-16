package net.chriskatze.catocraftmod.menu.ui;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base interface for all dynamic-menu UI components.
 * Defines a render method and basic geometry data accessors.
 */
public interface UIElement {
    int x();
    int y();
    int width();
    int height();

    /** Draws this element each frame. */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
}