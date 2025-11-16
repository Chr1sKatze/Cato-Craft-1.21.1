package net.chriskatze.catocraftmod.creator.menu;

import net.chriskatze.catocraftmod.menu.layout.SlotType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one slot’s configuration inside a menu.
 * JSON + network friendly. Includes legacy constructors for backward compatibility.
 */
public class MenuSlotDefinition {

    private String id;        // e.g. "slot_1"
    private int x, y;         // position inside GUI
    private int size;         // slot size (default 18)

    // ───────────────────────────────────────────────
    // Legacy single tag (still saved for compatibility)
    // ───────────────────────────────────────────────
    private String tag;

    // NEW: multi-tag support (preferred)
    private List<String> tags = new ArrayList<>();

    private String type;      // JSON-safe version of SlotType.name()
    private boolean optional;
    private String group = "default";

    // Optional per-slot texture override (independent of slot type)
    private String textureOverride = "";

    // Transient (runtime) enum form of type
    private transient SlotType slotType = SlotType.INVENTORY;

    // ───────────────────────────
    // Constructors
    // ───────────────────────────
    public MenuSlotDefinition() {
        this("slot", 0, 0, 18, "", SlotType.INVENTORY.name(), false, "default");
    }

    public MenuSlotDefinition(int x, int y) {
        this("slot_" + x + "_" + y, x, y, 18, "", SlotType.INVENTORY.name(), false, "default");
    }

    public MenuSlotDefinition(String id, int x, int y, int size,
                              String tag, String type, boolean optional, String group) {

        this.id = id;
        this.x = x;
        this.y = y;
        this.size = size;

        // Legacy tag support
        this.tag = tag == null ? "" : tag;
        if (tag != null && !tag.isBlank()) {
            this.tags.add(tag); // Legacy → new system sync
        }

        this.type = (type == null ? SlotType.INVENTORY.name() : type);
        this.optional = optional;
        this.group = (group == null || group.isBlank() ? "default" : group);

        this.slotType = SlotType.fromString(this.type);
        if (this.slotType == null)
            this.slotType = SlotType.INVENTORY;
    }

    // Legacy constructors
    public MenuSlotDefinition(String id, int x, int y, int size, String tag, String type, boolean optional) {
        this(id, x, y, size, tag, type, optional, "default");
    }

    public MenuSlotDefinition(String id, int x, int y, int size, String tag, boolean optional) {
        this(id, x, y, size, tag, SlotType.INVENTORY.name(), optional, "default");
    }

    public MenuSlotDefinition(String id, int x, int y, int size, boolean optional) {
        this(id, x, y, size, "", SlotType.INVENTORY.name(), optional, "default");
    }

    // ───────────────────────────
    // Copy
    // ───────────────────────────
    public MenuSlotDefinition copy() {
        MenuSlotDefinition def = new MenuSlotDefinition(
                this.id, this.x, this.y, this.size,
                this.tag, this.type, this.optional, this.group
        );

        def.slotType = this.slotType;
        def.textureOverride = this.textureOverride;

        // Deep copy tags
        def.tags = new ArrayList<>(this.tags);

        return def;
    }

    // ───────────────────────────
    // Network
    // ───────────────────────────
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(id != null ? id : "");
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(size);

        // Legacy tag field still sent for compatibility
        buf.writeUtf(tag != null ? tag : "");

        buf.writeUtf(type != null ? type : SlotType.INVENTORY.name());
        buf.writeBoolean(optional);
        buf.writeUtf(group != null ? group : "default");
        buf.writeUtf(textureOverride != null ? textureOverride : "");

        // NEW: send multi-tag list
        buf.writeVarInt(tags.size());
        for (String t : tags)
            buf.writeUtf(t);
    }

    public static MenuSlotDefinition fromNetwork(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int x = buf.readVarInt();
        int y = buf.readVarInt();
        int size = buf.readVarInt();

        String legacyTag = buf.readUtf();
        String type = buf.readUtf();
        boolean optional = buf.readBoolean();
        String group = buf.readableBytes() > 0 ? buf.readUtf() : "default";
        String textureOverride = buf.readableBytes() > 0 ? buf.readUtf() : "";

        MenuSlotDefinition def = new MenuSlotDefinition(id, x, y, size, legacyTag, type, optional, group);
        def.textureOverride = textureOverride;

        // Read multi-tags if present
        if (buf.readableBytes() > 0) {
            int count = buf.readVarInt();
            def.tags = new ArrayList<>();
            for (int i = 0; i < count; i++)
                def.tags.add(buf.readUtf());
        }

        // If no multi-tags were sent, pull legacy tag into tags list
        if (def.tags.isEmpty() && legacyTag != null && !legacyTag.isBlank())
            def.tags.add(legacyTag);

        return def;
    }

    // ───────────────────────────
    // Accessors
    // ───────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    // Legacy tag support (kept for compatibility)
    public String getTag() { return tag; }
    public void setTag(String v) {
        this.tag = (v == null ? "" : v);
        if (v != null && !v.isBlank())
            addTag(v); // sync into new system
    }

    // NEW multi-tag API
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) {
        this.tags = (tags == null ? new ArrayList<>() : tags);
    }

    public void addTag(String t) {
        if (t != null && !t.isBlank() && !tags.contains(t))
            tags.add(t);
    }

    public void removeTag(String t) {
        tags.remove(t);
    }

    public String getType() { return type; }
    public void setType(String v) {
        this.type = (v == null || v.isBlank()) ? SlotType.INVENTORY.name() : v;
        this.slotType = SlotType.fromString(this.type);
        if (this.slotType == null)
            this.slotType = SlotType.INVENTORY;
    }

    public SlotType getSlotType() { return slotType; }
    public void setSlotType(SlotType t) {
        this.slotType = (t == null ? SlotType.INVENTORY : t);
        this.type = this.slotType.name();
    }

    public boolean isInGroup(String groupName) {
        return groupName != null && groupName.equalsIgnoreCase(this.group);
    }

    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }

    public String getGroup() { return group; }
    public void setGroup(String group) {
        this.group = (group == null || group.isBlank()) ? "default" : group;
    }

    public String getTextureOverride() { return textureOverride; }
    public void setTextureOverride(String texture) {
        this.textureOverride = (texture == null ? "" : texture);
    }
}