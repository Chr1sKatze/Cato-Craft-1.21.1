package net.chriskatze.catocraftmod.menucreator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Menu Creator UI:
 * - Draws a resizable canvas (width/height taken from MenuCreatorDefinition).
 * - Lets you drag slots within the canvas.
 * - Drag the bottom-right handle to resize the canvas.
 * - Press 'A' to add a test slot.
 * - Pauses in singleplayer only; uses a dark tinted background overlay.
 *
 * Persistence (save/load) is handled by your `/menucreator` commands via MenuCreatorManager.
 */
public class MenuCreatorScreen extends Screen {

    private final MenuCreatorDefinition def;

    // Detect multiplayer (game should not pause)
    private final boolean isMultiplayer = !Minecraft.getInstance().hasSingleplayerServer();

    // Canvas placement in screen space (we keep it centered; only the inner size is editable)
    private int canvasX, canvasY; // top-left of canvas
    private int canvasW, canvasH; // mirrors def.width / def.height

    // Resize handle
    private static final int HANDLE_SIZE = 8;
    private boolean resizing = false;

    // Dragging a slot
    private MenuCreatorSlotDefinition draggingSlot = null;
    private int dragOffsetX, dragOffsetY;

    // Simple list of slot "views" (1:1 with def.slots)
    private final List<SlotView> slotViews = new ArrayList<>();

    // Grid
    private static final int GRID_STEP = 8;
    private static final int GRID_BOLD_STEP = 32;

    public MenuCreatorScreen(MenuCreatorDefinition def) {
        super(Component.literal("Menu Creator: " + def.menuId));
        this.def = def;
    }

    @Override
    protected void init() {
        super.init();

        // Always release mouse for GUI
        Minecraft.getInstance().mouseHandler.releaseMouse();

        // Mirror canvas size from definition
        this.canvasW = Math.max(64, def.width);
        this.canvasH = Math.max(64, def.height);

        // Center canvas on screen
        layoutCanvas();

        // Build slot views
        slotViews.clear();
        if (def.slots.isEmpty()) {
            def.slots.add(new MenuCreatorSlotDefinition("slot_1", 8, 8));
        }
        for (MenuCreatorSlotDefinition s : def.slots) {
            slotViews.add(new SlotView(this, s));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Never pause, prevents blur from being applied
    }

    private void layoutCanvas() {
        this.canvasX = (this.width - canvasW) / 2;
        this.canvasY = (this.height - canvasH) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Draw a semi-transparent tint BEHIND the UI
        g.fill(0, 0, this.width, this.height, 0x88000000); // ~53% opacity

        // Title and info
        g.drawString(this.font, "Menu: " + def.menuId, 10, 10, 0xFFFFFF, false);
        g.drawString(this.font, "Canvas: " + canvasW + " x " + canvasH, 10, 22, 0xAAAAAA, false);
        g.drawString(this.font, "Tip: drag slots; drag corner to resize; press 'A' to add slot", 10, 34, 0xAAAAAA, false);

        // Canvas + grid + slots
        drawCanvas(g);
        drawGrid(g);
        for (SlotView view : slotViews) view.render(g, mouseX, mouseY);
        drawHandle(g);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawCanvas(GuiGraphics g) {
        // Background
        g.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, 0xAA000000);

        // Border
        int border = 0xFF666666;
        g.hLine(canvasX, canvasX + canvasW, canvasY, border);
        g.hLine(canvasX, canvasX + canvasW, canvasY + canvasH, border);
        g.vLine(canvasX, canvasY, canvasY + canvasH, border);
        g.vLine(canvasX + canvasW, canvasY, canvasY + canvasH, border);
    }

    private void drawGrid(GuiGraphics g) {
        int minor = 0x22333333;
        int major = 0x55333333;

        // vertical lines
        for (int x = 0; x <= canvasW; x += GRID_STEP) {
            int color = (x % GRID_BOLD_STEP == 0) ? major : minor;
            g.vLine(canvasX + x, canvasY + 1, canvasY + canvasH - 1, color);
        }
        // horizontal lines
        for (int y = 0; y <= canvasH; y += GRID_STEP) {
            int color = (y % GRID_BOLD_STEP == 0) ? major : minor;
            g.hLine(canvasX + 1, canvasX + canvasW - 1, canvasY + y, color);
        }
    }

    private void drawHandle(GuiGraphics g) {
        int hx = canvasX + canvasW - HANDLE_SIZE;
        int hy = canvasY + canvasH - HANDLE_SIZE;
        int bg = resizing ? 0xFF4A90E2 : 0xFF888888;
        g.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, bg);
        g.drawString(this.font, "⇲", hx - 1, hy - 2, 0xFFEEEEEE, false);
    }

    // --- Mouse input ---------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check resize handle first
            if (overHandle(mouseX, mouseY)) {
                resizing = true;
                return true;
            }
            // If click inside canvas, maybe start dragging a slot
            if (insideCanvas(mouseX, mouseY)) {
                for (int i = slotViews.size() - 1; i >= 0; i--) {
                    SlotView view = slotViews.get(i);
                    if (view.over(mouseX, mouseY)) {
                        draggingSlot = view.data;
                        dragOffsetX = (int) mouseX - (canvasX + draggingSlot.x);
                        dragOffsetY = (int) mouseY - (canvasY + draggingSlot.y);
                        return true;
                    }
                }
                return true; // consume click inside canvas
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (resizing) {
                resizing = false;
                def.width = canvasW;
                def.height = canvasH;
                return true;
            }
            if (draggingSlot != null) {
                draggingSlot = null;
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0) {
            if (resizing) {
                int newW = (int) mouseX - canvasX;
                int newH = (int) mouseY - canvasY;

                canvasW = Mth.clamp(newW, 64, 512);
                canvasH = Mth.clamp(newH, 64, 512);

                layoutCanvas();
                return true;
            }
            if (draggingSlot != null) {
                int localX = (int) mouseX - canvasX - dragOffsetX;
                int localY = (int) mouseY - canvasY - dragOffsetY;

                localX = Mth.clamp(localX, 0, canvasW - SlotView.SLOT_W);
                localY = Mth.clamp(localY, 0, canvasH - SlotView.SLOT_H);

                draggingSlot.x = snap(localX, GRID_STEP);
                draggingSlot.y = snap(localY, GRID_STEP);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private static int snap(int v, int step) {
        return (v / step) * step;
    }

    private boolean insideCanvas(double mx, double my) {
        return mx >= canvasX && mx <= canvasX + canvasW && my >= canvasY && my <= canvasY + canvasH;
    }

    private boolean overHandle(double mx, double my) {
        int hx = canvasX + canvasW - HANDLE_SIZE;
        int hy = canvasY + canvasH - HANDLE_SIZE;
        return mx >= hx && mx <= hx + HANDLE_SIZE && my >= hy && my <= hy + HANDLE_SIZE;
    }

    // --- Keyboard ------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_A) {
            addTestSlot();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void addTestSlot() {
        int idx = def.slots.size() + 1;
        int px = snap(8 + (idx * 12) % Math.max(8, canvasW - 24), GRID_STEP);
        int py = snap(8 + ((idx * 18) % Math.max(8, canvasH - 24)), GRID_STEP);
        MenuCreatorSlotDefinition s = new MenuCreatorSlotDefinition("slot_" + idx, px, py);
        def.slots.add(s);
        slotViews.add(new SlotView(this, s));
    }

    @Override
    public void onClose() {
        def.width = canvasW;
        def.height = canvasH;
        super.onClose();

        // Re-grab mouse when leaving the editor
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.grabMouse();
        mc.setScreen(null);
    }

    // --- SlotView inner class ------------------------------------------------

    private static class SlotView implements GuiEventListener {
        static final int SLOT_W = 18;
        static final int SLOT_H = 18;

        final MenuCreatorScreen parent;
        final MenuCreatorSlotDefinition data;

        SlotView(MenuCreatorScreen parent, MenuCreatorSlotDefinition data) {
            this.parent = parent;
            this.data = data;
        }

        void render(GuiGraphics g, int mouseX, int mouseY) {
            int ax = parent.canvasX + data.x;
            int ay = parent.canvasY + data.y;

            g.fill(ax, ay, ax + SLOT_W, ay + SLOT_H, 0xFF2A2A2A);
            g.hLine(ax, ax + SLOT_W, ay, 0xFF888888);
            g.hLine(ax, ax + SLOT_W, ay + SLOT_H, 0xFF000000);
            g.vLine(ax, ay, ay + SLOT_H, 0xFF888888);
            g.vLine(ax + SLOT_W, ay, ay + SLOT_H, 0xFF000000);

            String label = data.id != null ? data.id : "(slot)";
            g.drawString(parent.font, label, ax + 2, ay + 5, 0xFFDDDDDD, false);
        }

        boolean over(double mx, double my) {
            int ax = parent.canvasX + data.x;
            int ay = parent.canvasY + data.y;
            return mx >= ax && mx <= ax + SLOT_W && my >= ay && my <= ay + SLOT_H;
        }

        @Override public boolean isMouseOver(double mx, double my) { return over(mx, my); }
        @Override public void setFocused(boolean focused) {}
        @Override public boolean isFocused() { return false; }
    }

    // GLFW key codes (local constants)
    private static final class GLFW {
        static final int GLFW_KEY_ESCAPE = 256;
        static final int GLFW_KEY_A = 65;
    }
}