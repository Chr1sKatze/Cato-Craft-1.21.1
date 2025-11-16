package net.chriskatze.catocraftmod.menu.ui;

import com.google.gson.annotations.SerializedName;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Represents a character preview element in a menu layout.
 * Displays the player model or a dummy entity at a certain position and scale.
 */
public record CharacterViewElement(
        @SerializedName("x") int x,
        @SerializedName("y") int y,
        @SerializedName("scale") int scale
) implements UIElement {

    @Override
    public int width() {
        return scale; // or a fixed pixel size if you prefer
    }

    @Override
    public int height() {
        return scale;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // TODO: Render the character model here.
        // For now, leave empty so the class compiles cleanly.
    }
}