package net.chriskatze.catocraftmod.creatorhub.editor;

public class GridSettings {
    /** pixel size of each grid cell (vanilla slot is 18) */
    public int cellSizePx = 18;

    /** additional origin offsets (panning) in pixels */
    public int offsetX = 0;
    public int offsetY = 0;

    /** canvas (grid) size in cells — logical “workspace” */
    public int cols = 9;
    public int rows = 6;

    /** area where the grid is rendered (assigned by screen) */
    public int canvasX, canvasY, canvasW, canvasH;

    /** returns the pixel x of the canvas’ top-left, centered + offset */
    public int gridPixelLeft() {
        int totalW = cols * cellSizePx;
        return canvasX + (canvasW - totalW) / 2 + offsetX;
    }
    public int gridPixelTop() {
        int totalH = rows * cellSizePx;
        return canvasY + (canvasH - totalH) / 2 + offsetY;
    }

    public int toGridX(int pixelX) {
        int gx = (pixelX - gridPixelLeft()) / cellSizePx;
        return Math.max(0, Math.min(cols - 1, gx));
    }
    public int toGridY(int pixelY) {
        int gy = (pixelY - gridPixelTop()) / cellSizePx;
        return Math.max(0, Math.min(rows - 1, gy));
    }

    public int toPixelX(int gridX) {
        return gridPixelLeft() + gridX * cellSizePx;
    }
    public int toPixelY(int gridY) {
        return gridPixelTop() + gridY * cellSizePx;
    }
}