package net.chriskatze.catocraftmod.creatorhub.editor;

public enum FeatureType {
    SLOT_INVENTORY,
    SLOT_ARMOR,
    SLOT_OFFHAND,
    SLOT_MAINHAND,
    SLOT_INGREDIENT,
    SLOT_RESULT,

    TEXT,            // (later)
    PROGRESS_BAR;    // (later)

    /** For quick “cycle” on right-click until we have a context menu. */
    public FeatureType nextSlotKind() {
        // only cycle among SLOT_* kinds
        return switch (this) {
            case SLOT_INVENTORY -> SLOT_ARMOR;
            case SLOT_ARMOR -> SLOT_MAINHAND;
            case SLOT_MAINHAND -> SLOT_OFFHAND;
            case SLOT_OFFHAND -> SLOT_INGREDIENT;
            case SLOT_INGREDIENT -> SLOT_RESULT;
            case SLOT_RESULT -> SLOT_INVENTORY;
            default -> this;
        };
    }

    public boolean isSlot() {
        return name().startsWith("SLOT_");
    }
}