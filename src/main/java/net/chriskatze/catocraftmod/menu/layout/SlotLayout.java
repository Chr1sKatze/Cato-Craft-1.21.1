package net.chriskatze.catocraftmod.menu.layout;

import java.util.List;

/**
 * Immutable UI layout spec for placing slots in a menu.
 * Supports two modes:
 *  - Procedural: origin + direction + spacing (+ optional wrap)
 *  - Grid mode: explicit cell(gridX, gridY) coordinates
 */
public record SlotLayout(
        int originX,
        int originY,
        int spacing,
        LayoutDirection direction,
        int wrapAfter,
        List<Point> cells,         // empty => procedural mode; non-empty => grid mode
        int groupOffsetX,
        int groupOffsetY,
        Integer canvasWidth,       // NEW: optional, may be null
        Integer canvasHeight       // NEW: optional, may be null
) {
    public boolean isGridMode() {
        return cells != null && !cells.isEmpty();
    }

    // Safe getters with defaults
    public int getCanvasWidthOrDefault(int fallback) {
        return canvasWidth != null && canvasWidth > 0 ? canvasWidth : fallback;
    }
    public int getCanvasHeightOrDefault(int fallback) {
        return canvasHeight != null && canvasHeight > 0 ? canvasHeight : fallback;
    }

    public record Point(int x, int y) {}
}