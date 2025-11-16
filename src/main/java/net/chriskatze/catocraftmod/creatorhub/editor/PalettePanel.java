package net.chriskatze.catocraftmod.creatorhub.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PalettePanel implements Renderable, GuiEventListener {
    private final EditorState state;
    private int x, y, w, h;

    private static class Item {
        final FeatureType type;
        final String label;
        int ix, iy, iw, ih; // bounds for clicks
        Item(FeatureType type, String label) {
            this.type = type; this.label = label;
        }
    }

    private final List<Item> items = new ArrayList<>();

    public PalettePanel(EditorState state, int x, int y, int w, int h) {
        this.state = state;
        this.x = x; this.y = y; this.w = w; this.h = h;
        buildItems();
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        layoutItems();
    }

    private void buildItems() {
        items.clear();
        items.add(new Item(FeatureType.SLOT_INVENTORY, "Slot (Inventory)"));
        items.add(new Item(FeatureType.SLOT_ARMOR, "Slot (Armor)"));
        items.add(new Item(FeatureType.SLOT_MAINHAND, "Slot (Mainhand)"));
        items.add(new Item(FeatureType.SLOT_OFFHAND, "Slot (Offhand)"));
        items.add(new Item(FeatureType.SLOT_INGREDIENT, "Slot (Ingredient)"));
        items.add(new Item(FeatureType.SLOT_RESULT, "Slot (Result)"));
        // later: TEXT, PROGRESS_BAR
    }

    private void layoutItems() {
        int pad = 6;
        int yCursor = y + pad + 12;
        for (Item it : items) {
            it.ix = x + pad;
            it.iy = yCursor;
            it.iw = w - pad * 2;
            it.ih = 18;
            yCursor += it.ih + 4;
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // panel bg
        gfx.fill(x, y, x + w, y + h, 0xAA0D0D0D);
        gfx.drawString(Minecraft.getInstance().font, Component.literal("Palette"), x + 6, y + 6, 0xFFFFFF, false);

        // items
        for (Item it : items) {
            boolean hover = mouseX >= it.ix && mouseX <= it.ix + it.iw && mouseY >= it.iy && mouseY <= it.iy + it.ih;
            int bg = hover ? 0xFF2A2A2A : 0xFF1A1A1A;
            gfx.fill(it.ix, it.iy, it.ix + it.iw, it.iy + it.ih, bg);
            gfx.drawString(Minecraft.getInstance().font, it.label, it.ix + 6, it.iy + 5, 0xDDDDDD, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        for (Item it : items) {
            if (mouseX >= it.ix && mouseX <= it.ix + it.iw && mouseY >= it.iy && mouseY <= it.iy + it.ih) {
                state.draggingType = it.type;
                return true;
            }
        }
        return false;
    }

    // unused
    @Override public boolean mouseReleased(double x, double y, int b) { return false; }
    @Override public boolean mouseDragged(double x, double y, int b, double dx, double dy) { return false; }
    @Override public void setFocused(boolean f) {}
    @Override public boolean isFocused() { return false; }
}