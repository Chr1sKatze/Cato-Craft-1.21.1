package net.chriskatze.catocraftmod.creator.menu;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one menu layout JSON file.
 * Serialized by JsonIO and synchronized over the network.
 */
public class MenuLayout {

    private String name;
    private int width;
    private int height;
    private List<MenuSlotDefinition> slots = new ArrayList<>();

    // ────────────────────────────────────────────────
    // Constructors
    // ────────────────────────────────────────────────
    public MenuLayout() {
        // Empty constructor for JSON
    }

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

        // Slots
        buf.writeVarInt(slots.size());
        for (MenuSlotDefinition def : slots) {
            def.toNetwork(buf);
        }
    }

    public static MenuLayout fromNetwork(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        int width = buf.readVarInt();
        int height = buf.readVarInt();
        int count = buf.readVarInt();

        MenuLayout layout = new MenuLayout(name, width, height);
        for (int i = 0; i < count; i++) {
            layout.getSlots().add(MenuSlotDefinition.fromNetwork(buf));
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
}