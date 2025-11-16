package net.chriskatze.catocraftmod.creator.menu;

/**
 * Represents a logical group of slots within a MenuLayout.
 * Defines what kind of items are valid for all slots in this group.
 */
public class MenuGroupDefinition {

    private String name;               // Unique ID within the layout
    private String displayName;        // UI label
    private String validItemTag;       // Optional tag filter (e.g. "catocraftmod:earrings")
    private String validItemType;      // Optional class-based restriction (e.g. "armor", "weapon")
    private boolean isStrict;          // If true, items outside tag/type are rejected

    public MenuGroupDefinition(String name) {
        this.name = name;
        this.displayName = name;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getValidItemTag() { return validItemTag; }
    public void setValidItemTag(String validItemTag) { this.validItemTag = validItemTag; }

    public String getValidItemType() { return validItemType; }
    public void setValidItemType(String validItemType) { this.validItemType = validItemType; }

    public boolean isStrict() { return isStrict; }
    public void setStrict(boolean strict) { isStrict = strict; }
}