package net.chriskatze.catocraftmod.creatorhub.editor;

import net.minecraft.client.gui.GuiGraphics;

/**
 * GridCanvas — centered grid (2 px spacing, bold every 40 px), strict axis-locked panning,
 * and separate X/Y resize handles for menu width/height (in 18px cell units).
 */
public class GridCanvas {

    private final EditorState state;

    private int x, y, w, h;

    // panning
    private boolean panning = false;
    private double panStartMouseX, panStartMouseY;
    private double lastMouseX, lastMouseY;
    private AxisLock panLock = AxisLock.NONE;
    private static final double AXIS_LOCK_HYSTERESIS = 6.0; // pixels

    // resizing
    private boolean resizingWidth = false;
    private boolean resizingHeight = false;

    // handles visuals
    private static final int HANDLE_THICKNESS = 6; // edge grab thickness

    public GridCanvas(EditorState state, int x, int y, int w, int h) {
        this.state = state;
        setBounds(x, y, w, h);
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(10, w);
        this.h = Math.max(10, h);
    }

    // ────────────────────────────────────────────────
    // Rendering
    // ────────────────────────────────────────────────
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        final int cell = Math.max(1, state.grid.cellSizePx); // usually 18
        final int cols = Math.max(1, state.grid.cols);
        final int rows = Math.max(1, state.grid.rows);

        // total menu rectangle in pixels
        final int menuW = cols * cell;
        final int menuH = rows * cell;

        // centered grid origin, then nudged by offsets
        final int cx = x + w / 2 + (int) Math.round(state.grid.offsetX);
        final int cy = y + h / 2 + (int) Math.round(state.grid.offsetY);
        final int left = cx - menuW / 2;
        final int top  = cy - menuH / 2;

        // draw grid (no background fill)
        drawGrid(gfx, left, top, menuW, menuH);

        // draw menu border
        gfx.renderOutline(left, top, menuW, menuH, 0xFFFFFFFF);

        // draw resize handles (right edge + bottom edge)
        drawEdgeHandle(gfx, /*right*/ true, left, top, menuW, menuH, mouseX, mouseY);
        drawEdgeHandle(gfx, /*right*/ false, left, top, menuW, menuH, mouseX, mouseY);
    }

    private void drawGrid(GuiGraphics gfx, int left, int top, int width, int height) {
        // fine 2px grid, mid line every 10px, bold every 40px
        // verticals
        for (int gx = 0; gx <= width; gx += 2) {
            int x1 = left + gx;
            int color;
            if (gx % 40 == 0)      color = 0x55222222;  // bold
            else if (gx % 10 == 0) color = 0x33111111;  // mid
            else                   color = 0x22000000;  // hair
            gfx.fill(x1, top, x1 + 1, top + height, color);
        }
        // horizontals
        for (int gy = 0; gy <= height; gy += 2) {
            int y1 = top + gy;
            int color;
            if (gy % 40 == 0)      color = 0x55222222;
            else if (gy % 10 == 0) color = 0x33111111;
            else                   color = 0x22000000;
            gfx.fill(left, y1, left + width, y1 + 1, color);
        }
    }

    private void drawEdgeHandle(GuiGraphics gfx, boolean rightEdge,
                                int left, int top, int menuW, int menuH,
                                int mouseX, int mouseY) {
        if (rightEdge) {
            int hx1 = left + menuW - HANDLE_THICKNESS;
            int hx2 = left + menuW;
            int hy1 = top;
            int hy2 = top + menuH;
            int base = 0x66444444;
            int hover = 0x88FFFFFF;
            int color = isOverRightHandle(mouseX, mouseY, hx1, hy1, hx2 - hx1, hy2 - hy1) || resizingWidth
                    ? hover : base;
            gfx.fill(hx1, hy1, hx2, hy2, color);
        } else {
            int hx1 = left;
            int hx2 = left + menuW;
            int hy1 = top + menuH - HANDLE_THICKNESS;
            int hy2 = top + menuH;
            int base = 0x66444444;
            int hover = 0x88FFFFFF;
            int color = isOverBottomHandle(mouseX, mouseY, hx1, hy1, hx2 - hx1, hy2 - hy1) || resizingHeight
                    ? hover : base;
            gfx.fill(hx1, hy1, hx2, hy2, color);
        }
    }

    // ────────────────────────────────────────────────
    // Input Handling
    // ────────────────────────────────────────────────
    public boolean mouseClicked(double mx, double my, int button) {
        final int cell = Math.max(1, state.grid.cellSizePx);
        final int cols = Math.max(1, state.grid.cols);
        final int rows = Math.max(1, state.grid.rows);

        final int menuW = cols * cell;
        final int menuH = rows * cell;

        final int cx = x + w / 2 + (int) Math.round(state.grid.offsetX);
        final int cy = y + h / 2 + (int) Math.round(state.grid.offsetY);
        final int left = cx - menuW / 2;
        final int top  = cy - menuH / 2;

        // check handles first
        if (button == 0) {
            if (isOverRightHandle(mx, my, left + menuW - HANDLE_THICKNESS, top, HANDLE_THICKNESS, menuH)) {
                resizingWidth = true;
                lastMouseX = mx;
                return true;
            }
            if (isOverBottomHandle(mx, my, left, top + menuH - HANDLE_THICKNESS, menuW, HANDLE_THICKNESS)) {
                resizingHeight = true;
                lastMouseY = my;
                return true;
            }
        }

        // middle mouse panning
        if (button == 2) {
            panning = true;
            panLock = AxisLock.NONE;
            panStartMouseX = lastMouseX = mx;
            panStartMouseY = lastMouseY = my;
            return true;
        }

        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 2) {
            panning = false;
            panLock = AxisLock.NONE;
        }
        if (button == 0) {
            resizingWidth = false;
            resizingHeight = false;
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        final int cell = Math.max(1, state.grid.cellSizePx);

        // resizing — snap columns/rows by cell size
        if (resizingWidth) {
            double diffX = mx - lastMouseX;
            int deltaCols = (int) Math.round(diffX / cell);
            if (deltaCols != 0) {
                state.grid.cols = Math.max(1, state.grid.cols + deltaCols);
                lastMouseX = mx;
            }
            return true;
        }
        if (resizingHeight) {
            double diffY = my - lastMouseY;
            int deltaRows = (int) Math.round(diffY / cell);
            if (deltaRows != 0) {
                state.grid.rows = Math.max(1, state.grid.rows + deltaRows);
                lastMouseY = my;
            }
            return true;
        }

        // panning with strict axis lock
        if (panning) {
            double totalDx = mx - panStartMouseX;
            double totalDy = my - panStartMouseY;

            if (panLock == AxisLock.NONE) {
                if (Math.abs(totalDx) > AXIS_LOCK_HYSTERESIS || Math.abs(totalDy) > AXIS_LOCK_HYSTERESIS) {
                    panLock = (Math.abs(totalDx) >= Math.abs(totalDy)) ? AxisLock.HORIZONTAL : AxisLock.VERTICAL;
                }
            }

            double stepDx = mx - lastMouseX;
            double stepDy = my - lastMouseY;

            if (panLock == AxisLock.HORIZONTAL) {
                state.grid.offsetX += stepDx;
            } else if (panLock == AxisLock.VERTICAL) {
                state.grid.offsetY += stepDy;
            }
            lastMouseX = mx;
            lastMouseY = my;
            return true;
        }

        return false;
    }

    // ────────────────────────────────────────────────
    // Hit tests
    // ────────────────────────────────────────────────
    private boolean isOverRightHandle(double mx, double my, int hx, int hy, int hw, int hh) {
        return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
    }

    private boolean isOverBottomHandle(double mx, double my, int hx, int hy, int hw, int hh) {
        return mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh;
    }

    private enum AxisLock { NONE, HORIZONTAL, VERTICAL }
}