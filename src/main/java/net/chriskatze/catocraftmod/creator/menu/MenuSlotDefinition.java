package net.chriskatze.catocraftmod.creator.menu;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Represents one slot’s configuration inside a menu.
 * Used both for JSON serialization and network synchronization.
 */
public class MenuSlotDefinition {

    private String id;           // e.g. "earring_slot_left"
    private int x, y;            // position inside GUI
    private int size;            // slot size (default 18)
    private String tagFilter;    // allowed items/tag
    private boolean optional;    // if true, can be empty

    // ────────────────────────────────────────────────
    // Constructors
    // ────────────────────────────────────────────────

    /** Default constructor for JSON deserialization */
    public MenuSlotDefinition() {
        this("slot", 0, 0, 18, "", false);
    }

    /** Simple constructor for quick creation (used in networking) */
    public MenuSlotDefinition(int x, int y) {
        this("slot_" + x + "_" + y, x, y, 18, "", false);
    }

    public MenuSlotDefinition(String id, int x, int y, int size, String tagFilter, boolean optional) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.size = size;
        this.tagFilter = tagFilter;
        this.optional = optional;
    }

    // ────────────────────────────────────────────────
    // Network Serialization
    // ────────────────────────────────────────────────

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(id != null ? id : "");
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(size);
        buf.writeUtf(tagFilter != null ? tagFilter : "");
        buf.writeBoolean(optional);
    }

    public static MenuSlotDefinition fromNetwork(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int x = buf.readVarInt();
        int y = buf.readVarInt();
        int size = buf.readVarInt();
        String tagFilter = buf.readUtf();
        boolean optional = buf.readBoolean();
        return new MenuSlotDefinition(id, x, y, size, tagFilter, optional);
    }

    // ────────────────────────────────────────────────
    // Accessors
    // ────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getTagFilter() { return tagFilter; }
    public void setTagFilter(String tagFilter) { this.tagFilter = tagFilter; }

    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }
}