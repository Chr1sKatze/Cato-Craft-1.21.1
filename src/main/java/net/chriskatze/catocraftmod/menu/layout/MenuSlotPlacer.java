package net.chriskatze.catocraftmod.menu.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates screen-space coordinates for inventory slots.
 * Supports both procedural and grid-based placement.
 * Automatically applies group offsets from SlotLayout.
 */
public class MenuSlotPlacer {

    public record SlotPosition(int x, int y) {}

    private final List<SlotPosition> positions = new ArrayList<>();

    /** Construct a placer directly from a SlotLayout definition. */
    public MenuSlotPlacer(SlotLayout layout) {
        int offsetX = layout.groupOffsetX();
        int offsetY = layout.groupOffsetY();

        if (layout.isGridMode()) {
            buildGrid(layout, offsetX, offsetY);
        } else {
            buildProcedural(layout, offsetX, offsetY);
        }
    }

    /** Procedural layout mode (direction + spacing + wrap). */
    private void buildProcedural(SlotLayout layout, int offsetX, int offsetY) {
        int x = layout.originX() + offsetX;
        int y = layout.originY() + offsetY;
        int spacing = layout.spacing();
        int wrapAfter = layout.wrapAfter();

        // Generate a fixed pattern of positions — Menu decides how many to use.
        int max = wrapAfter > 0 ? wrapAfter * 2 : 16;

        for (int i = 0; i < max; i++) {
            positions.add(new SlotPosition(x, y));

            switch (layout.direction()) {
                case HORIZONTAL -> x += spacing;
                case VERTICAL -> y += spacing;
            }

            if (wrapAfter > 0 && (i + 1) % wrapAfter == 0) {
                switch (layout.direction()) {
                    case HORIZONTAL -> {
                        x = layout.originX() + offsetX;
                        y += spacing;
                    }
                    case VERTICAL -> {
                        y = layout.originY() + offsetY;
                        x += spacing;
                    }
                }
            }
        }
    }

    /** Grid / pattern layout mode. */
    private void buildGrid(SlotLayout layout, int offsetX, int offsetY) {
        int baseX = layout.originX() + offsetX;
        int baseY = layout.originY() + offsetY;
        int spacing = layout.spacing();

        for (SlotLayout.Point p : layout.cells()) {
            int x = baseX + p.x() * spacing;
            int y = baseY + p.y() * spacing;
            positions.add(new SlotPosition(x, y));
        }
    }

    // Accessors
    public List<SlotPosition> getPositions() { return positions; }
    public SlotPosition get(int index) { return index < positions.size() ? positions.get(index) : null; }
    public int size() { return positions.size(); }
}