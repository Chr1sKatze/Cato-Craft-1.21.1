package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Represents a group of item slots in a menu layout.
 * Slots are arranged horizontally or vertically based on the "direction" property.
 */
public record SlotGroupElement(
        @SerializedName("id") String id,
        @SerializedName("x") int x,
        @SerializedName("y") int y,
        @SerializedName("slots") int slots,
        @SerializedName("direction") String direction,
        @SerializedName("valid_items") java.util.List<String> validItems
) implements UIElement {

    private static final int SLOT_SIZE = 18; // Standard slot spacing

    @Override
    public int width() {
        // Horizontal → width grows with slot count
        if ("horizontal".equalsIgnoreCase(direction)) {
            return slots * SLOT_SIZE;
        }
        return SLOT_SIZE; // Vertical layout
    }

    @Override
    public int height() {
        // Vertical → height grows with slot count
        if ("vertical".equalsIgnoreCase(direction)) {
            return slots * SLOT_SIZE;
        }
        return SLOT_SIZE; // Horizontal layout
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int startX = x;
        int startY = y;

        for (int i = 0; i < slots; i++) {
            int slotX = "horizontal".equalsIgnoreCase(direction) ? startX + i * SLOT_SIZE : startX;
            int slotY = "vertical".equalsIgnoreCase(direction) ? startY + i * SLOT_SIZE : startY;

            // Draw simple gray box for each slot (debug visualization)
            gfx.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF808080);

            // Draw slot index number in center
            gfx.drawCenteredString(font, String.valueOf(i + 1), slotX + 8, slotY + 4, 0xFFFFFFFF);
        }
    }
}