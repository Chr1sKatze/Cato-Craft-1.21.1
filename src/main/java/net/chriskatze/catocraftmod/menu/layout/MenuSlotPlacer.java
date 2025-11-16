package net.chriskatze.catocraftmod.menu.layout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧱 MenuSlotPlacer — handles rendering and interaction for the menu grid editor.
 *
 * Draws the grid background, shows existing slot positions,
 * and lets the user preview new ones visually (future drag-drop support).
 */
public class MenuSlotPlacer implements GuiEventListener, NarratableEntry {

    private final int originX;
    private final int originY;
    private final int gridWidth;
    private final int gridHeight;
    private final int cellSize = 16;

    private final List<SlotPreview> slots = new ArrayList<>();

    private boolean focused = false; // required by GuiEventListener

    public MenuSlotPlacer(int originX, int originY, int gridWidth, int gridHeight) {
        this.originX = originX;
        this.originY = originY;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }

    public void clearSlots() {
        slots.clear();
    }

    public void addSlot(int gridX, int gridY) {
        slots.add(new SlotPreview(gridX, gridY));
    }

    public List<SlotPreview> getSlots() {
        return slots;
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        // background panel
        gfx.fill(originX - 1, originY - 1, originX + gridWidth + 1, originY + gridHeight + 1, 0xAA000000);

        // vertical grid lines
        for (int x = 0; x <= gridWidth; x += cellSize) {
            gfx.fill(originX + x, originY, originX + x + 1, originY + gridHeight, 0x22000000);
        }

        // horizontal grid lines
        for (int y = 0; y <= gridHeight; y += cellSize) {
            gfx.fill(originX, originY + y, originX + gridWidth, originY + y + 1, 0x22000000);
        }

        // draw slot previews
        for (SlotPreview slot : slots) {
            int sx = originX + slot.x();
            int sy = originY + slot.y();

            int color = slot.isHovered(mouseX, mouseY, sx, sy, cellSize) ? 0x66FFFFFF : 0x33FFFFFF;
            gfx.fill(sx, sy, sx + cellSize, sy + cellSize, color);
            gfx.renderFakeItem(new ItemStack(net.minecraft.world.item.Items.CHEST), sx, sy);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // In future: add slot placement
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    // ────────────────────────────────────────────────
    // Focus handling (required by GuiEventListener)
    // ────────────────────────────────────────────────
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    // ────────────────────────────────────────────────
    // Narration (unused, but required)
    // ────────────────────────────────────────────────
    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narration) {}

    // ────────────────────────────────────────────────
    // Internal helper class
    // ────────────────────────────────────────────────
    public record SlotPreview(int x, int y) {
        public boolean isHovered(int mx, int my, int sx, int sy, int size) {
            return mx >= sx && mx < sx + size && my >= sy && my < sy + size;
        }
    }
}