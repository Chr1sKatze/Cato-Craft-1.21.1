package net.chriskatze.catocraftmod.creatorhub.editor;

import java.util.ArrayList;
import java.util.List;

public class EditorState {
    // What the user is currently dragging from the palette (null = nothing)
    public FeatureType draggingType = null;

    // Current selection (index in features list)
    public int selectedIndex = -1;

    // All features currently placed on the canvas
    public final List<PlacedFeature> features = new ArrayList<>();

    // Grid settings (shared)
    public final GridSettings grid = new GridSettings();

    public void clearSelection() { selectedIndex = -1; }

    public PlacedFeature getSelected() {
        if (selectedIndex < 0 || selectedIndex >= features.size()) return null;
        return features.get(selectedIndex);
    }

    /** Basic DTO for a placed feature (grid coordinates, type, etc.). */
    public static class PlacedFeature {
        public FeatureType type;
        public int gridX; // cell coords (not pixels)
        public int gridY;
        public int w = 1; // cells (for future resize)
        public int h = 1;

        public PlacedFeature(FeatureType type, int gridX, int gridY) {
            this.type = type;
            this.gridX = gridX;
            this.gridY = gridY;
        }
    }
}