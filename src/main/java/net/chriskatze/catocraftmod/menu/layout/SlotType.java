package net.chriskatze.catocraftmod.menu.layout;

/**
 * 📦 Canonical set of slot types usable in the Menu Editor and MenuLoader.
 * Each type represents a visual + functional category for the slot.
 *
 * Extended to carry behavioral metadata (insert/extract rules, uniqueness, etc.)
 */
public enum SlotType {

    // ────────────── Core Player Inventory ──────────────
    INVENTORY("Inventory Slot", true, true, true, 27),
    HOTBAR("Hotbar Slot", true, true, true, 9),

    // ────────────── Equipment ──────────────
    ARMOR("Armor Slot", true, true, true, 4),
    OFF_HAND("Offhand Slot", true, false, false, 1),

    // ────────────── Crafting ──────────────
    CRAFTING("Crafting Slot", true, false, true, -1),
    CRAFTING_RESULT("Crafting Result Slot", false, true, false, 1),

    // ────────────── Ingredient / Alchemy ──────────────
    INGREDIENT("Ingredient Slot", true, false, true, -1),
    INGREDIENT_RESULT("Ingredient Result Slot", false, true, false, 1),

    // ────────────── Custom / Modded Systems ──────────────
    JEWELLERY("Jewellery Slot", true, true, true, 6);

    private final String displayName;
    private final boolean canInsert;
    private final boolean canExtract;
    private final boolean allowsMultiple;
    private final int maxCount; // -1 means variable

    SlotType(String displayName, boolean canInsert, boolean canExtract, boolean allowsMultiple, int maxCount) {
        this.displayName = displayName;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
        this.allowsMultiple = allowsMultiple;
        this.maxCount = maxCount;
    }

    // ────────────── Getters ──────────────
    public String getDisplayName() { return displayName; }
    public boolean canInsert() { return canInsert; }
    public boolean canExtract() { return canExtract; }
    public boolean allowsMultiple() { return allowsMultiple; }
    public int getMaxCount() { return maxCount; }

    // ────────────── Logic helpers ──────────────
    /** Returns true if this type belongs to crafting systems (2×2, 3×3, custom). */
    public boolean isCraftingRelated() {
        return this == CRAFTING || this == CRAFTING_RESULT
                || this == INGREDIENT || this == INGREDIENT_RESULT;
    }

    /** Returns true if this slot type should mirror player inventory sections. */
    public boolean isPlayerLinked() {
        return this == INVENTORY || this == HOTBAR || this == ARMOR || this == OFF_HAND;
    }

    /** Returns enum constant by name (safe), or null if unknown. */
    public static SlotType fromString(String name) {
        for (SlotType t : values()) {
            if (t.name().equalsIgnoreCase(name) || t.displayName.equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }
}