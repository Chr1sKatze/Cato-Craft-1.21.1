package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Represents a text label element that can be defined in a menu JSON layout.
 * Supports custom position, color, and shadow rendering.
 */
public record LabelElement(
        @SerializedName("text") String text,
        @SerializedName("x") int x,
        @SerializedName("y") int y,
        @SerializedName("color") String color,
        @SerializedName("shadow") boolean shadow
) implements UIElement {

    @Override
    public int width() {
        // calculate based on font width
        return Minecraft.getInstance().font.width(text);
    }

    @Override
    public int height() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        int parsedColor;
        try {
            parsedColor = Integer.parseInt(color.replace("#", ""), 16);
        } catch (Exception e) {
            parsedColor = 0xFFFFFF; // fallback to white
        }

        gfx.drawString(
                Minecraft.getInstance().font,
                text,
                x,
                y,
                parsedColor,
                shadow
        );
    }
}