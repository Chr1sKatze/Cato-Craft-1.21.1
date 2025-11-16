package net.chriskatze.catocraftmod.creator.menu;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents one menu layout JSON file.
 * Serialized by JsonIO and synchronized over the network.
 *
 * Extended: now supports custom background + slot texture overrides.
 */
public class MenuLayout {

    private String name;
    private int width;
    private int height;
    private List<MenuSlotDefinition> slots = new ArrayList<>();

    // 🖼️ Optional custom background texture
    private String backgroundTexture = null;

    // 🎨 Optional per-slot-type texture overrides (e.g., {"JEWELLERY": "catocraftmod:textures/gui/slots/jewel_slot.png"})
    private Map<String, String> slotTextures = new HashMap<>();

    // ────────────────────────────────────────────────
    // Constructors
    // ────────────────────────────────────────────────
    public MenuLayout() {
        // Empty constructor for JSON
    }

    // 🧩 Slot groups (defines logical categories and item filters)
    private List<MenuGroupDefinition> groups = new ArrayList<>();

    public MenuLayout(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    // ────────────────────────────────────────────────
    // Network Serialization (Full slot data)
    // ────────────────────────────────────────────────
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(name != null ? name : "unnamed");
        buf.writeVarInt(width);
        buf.writeVarInt(height);

        // Background texture
        buf.writeBoolean(backgroundTexture != null);
        if (backgroundTexture != null)
            buf.writeUtf(backgroundTexture);

        // Slot texture overrides
        buf.writeVarInt(slotTextures.size());
        for (Map.Entry<String, String> entry : slotTextures.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }

        // Group definitions
        buf.writeVarInt(groups.size());
        for (MenuGroupDefinition g : groups) {
            buf.writeUtf(g.getName());
            buf.writeUtf(g.getDisplayName() != null ? g.getDisplayName() : g.getName());
            buf.writeUtf(g.getValidItemTag() != null ? g.getValidItemTag() : "");
            buf.writeUtf(g.getValidItemType() != null ? g.getValidItemType() : "");
            buf.writeBoolean(g.isStrict());
        }

        // Slot definitions
        buf.writeVarInt(slots.size());
        for (MenuSlotDefinition def : slots) {
            def.toNetwork(buf);
        }
    }

    public static MenuLayout fromNetwork(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        int width = buf.readVarInt();
        int height = buf.readVarInt();

        MenuLayout layout = new MenuLayout(name, width, height);

        // Background texture
        if (buf.readBoolean()) {
            layout.backgroundTexture = buf.readUtf();
        }

        // Slot texture overrides
        int mapSize = buf.readVarInt();
        for (int i = 0; i < mapSize; i++) {
            String key = buf.readUtf();
            String val = buf.readUtf();
            layout.slotTextures.put(key, val);
        }

        // Groups
        int groupCount = buf.readVarInt();
        for (int i = 0; i < groupCount; i++) {
            MenuGroupDefinition g = new MenuGroupDefinition(buf.readUtf());
            g.setDisplayName(buf.readUtf());
            String tag = buf.readUtf();
            if (!tag.isEmpty()) g.setValidItemTag(tag);
            String type = buf.readUtf();
            if (!type.isEmpty()) g.setValidItemType(type);
            g.setStrict(buf.readBoolean());
            layout.groups.add(g);
        }

        // Slots
        int slotCount = buf.readVarInt();
        for (int i = 0; i < slotCount; i++) {
            layout.slots.add(MenuSlotDefinition.fromNetwork(buf));
        }

        return layout;
    }

    // ────────────────────────────────────────────────
    // Accessors
    // ────────────────────────────────────────────────
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public List<MenuSlotDefinition> getSlots() { return slots; }
    public void setSlots(List<MenuSlotDefinition> slots) { this.slots = slots; }

    public void addSlot(MenuSlotDefinition def) { slots.add(def); }
    public void clearSlots() { this.slots.clear(); }

    // 🖼️ Background
    public String getBackgroundTexture() { return backgroundTexture; }
    public void setBackgroundTexture(String backgroundTexture) { this.backgroundTexture = backgroundTexture; }

    // 🎨 Slot texture overrides
    public Map<String, String> getSlotTextures() { return slotTextures; }
    public void setSlotTextures(Map<String, String> slotTextures) { this.slotTextures = slotTextures; }

    public void addSlotTexture(String slotType, String texturePath) {
        this.slotTextures.put(slotType, texturePath);
    }

    // ────────────────────────────────────────────────
    // Groups
    // ────────────────────────────────────────────────
    public List<MenuGroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(List<MenuGroupDefinition> groups) {
        this.groups = groups;
    }

    public MenuGroupDefinition findGroup(String name) {
        if (name == null) return null;
        for (MenuGroupDefinition g : groups) {
            if (g.getName().equalsIgnoreCase(name)) return g;
        }
        return null;
    }

    public void addGroup(MenuGroupDefinition group) {
        if (group == null) return;
        if (findGroup(group.getName()) == null) {
            groups.add(group);
        }
    }
}