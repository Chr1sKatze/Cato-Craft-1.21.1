package net.chriskatze.catocraftmod.menu.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for SlotLayout. Use either:
 *  - procedural: origin(...), direction(...), spacing(...), wrapAfter(...)
 *  - grid mode: origin(...), spacing(...), cell(x, y) per slot
 */
public class SlotLayoutBuilder {
    private int originX = 0, originY = 0;
    private int spacing = 18;
    private LayoutDirection direction = LayoutDirection.VERTICAL;
    private int wrapAfter = Integer.MAX_VALUE;  // no wrap by default
    private final List<SlotLayout.Point> cells = new ArrayList<>();
    private int groupOffsetX = 0, groupOffsetY = 0;

    // NEW: optional canvas size
    private Integer canvasWidth = null;
    private Integer canvasHeight = null;

    public SlotLayoutBuilder origin(int x, int y) {
        this.originX = x; this.originY = y; return this;
    }

    public SlotLayoutBuilder spacing(int px) {
        this.spacing = Math.max(1, px); return this;
    }

    public SlotLayoutBuilder direction(LayoutDirection dir) {
        this.direction = (dir == null) ? LayoutDirection.VERTICAL : dir; return this;
    }

    /**
     * Set wrap after N slots. Pass <= 0 for "no wrap".
     */
    public SlotLayoutBuilder wrapAfter(int n) {
        this.wrapAfter = (n <= 0) ? Integer.MAX_VALUE : Math.max(1, n);
        return this;
    }

    /** Enter grid mode: define an explicit cell position (column, row) for the next slot. */
    public SlotLayoutBuilder cell(int gridX, int gridY) {
        this.cells.add(new SlotLayout.Point(gridX, gridY)); return this;
    }

    /** Optional visual nudge when composing multiple groups. */
    public SlotLayoutBuilder groupOffset(int dx, int dy) {
        this.groupOffsetX = dx; this.groupOffsetY = dy; return this;
    }

    /** NEW: optional canvas size (screen area) in pixels. */
    public SlotLayoutBuilder canvas(int width, int height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
        return this;
    }

    public SlotLayout build() {
        return new SlotLayout(
                originX, originY, spacing, direction, wrapAfter,
                cells.isEmpty() ? List.of() : List.copyOf(cells),
                groupOffsetX, groupOffsetY,
                canvasWidth, canvasHeight      // <-- NEW args for the extended record
        );
    }
}