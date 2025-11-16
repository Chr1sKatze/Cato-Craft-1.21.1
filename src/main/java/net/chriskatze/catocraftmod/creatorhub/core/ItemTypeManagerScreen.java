package net.chriskatze.catocraftmod.creatorhub.core;

import net.chriskatze.catocraftmod.util.ItemTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static net.chriskatze.catocraftmod.creatorhub.CreatorHubTheme.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 📘 Item Type Manager — themed, scrollable version with manual auto-refresh.
 * Reloads list on open, add, or delete.
 */
public class ItemTypeManagerScreen extends BaseCreatorScreen {

    private final Screen parent;
    private List<String> types = new ArrayList<>();
    private EditBox newTypeBox;
    private Button addButton;
    private float scrollOffset = 0f;
    private final int entryHeight = 18;
    private final int maxVisible = 4;

    public ItemTypeManagerScreen(Screen parent) {
        super(Component.literal("Item Type Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshTypes(); // 🟦 Load from disk when screen opens

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int inputY = centerY - 45;

        // Input box
        newTypeBox = new EditBox(this.font, centerX - 100, inputY, 120, 18, Component.literal("New Type"));
        addRenderableWidget(newTypeBox);

        // Add button
        addButton = Button.builder(Component.literal("➕ Add"), b -> {
            String name = newTypeBox.getValue().trim();
            if (!name.isEmpty() && !types.contains(name)) {
                ItemTypeRegistry.addType(name);
                ItemTypeRegistry.save();
                refreshTypes(); // 🟦 Refresh after add
                newTypeBox.setValue("");
            }
        }).pos(centerX + 26, inputY).size(60, 18).build();
        addRenderableWidget(addButton);
    }

    // 🟦 Manual reload from JSON file
    private void refreshTypes() {
        ItemTypeRegistry.load(); // rereads JSON file
        types = new ArrayList<>(ItemTypeRegistry.getAllTypeNames());
        scrollOffset = 0f;
    }

    @Override
    protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        drawDialogBox(gfx, centerY, "Item Type Manager", "Manage item classification types:", null);

        // Themed EditBox background
        int ebx = newTypeBox.getX(), eby = newTypeBox.getY(), ebw = newTypeBox.getWidth(), ebh = newTypeBox.getHeight();
        gfx.fill(ebx - 2, eby - 2, ebx + ebw + 2, eby + ebh + 2, PANEL_DARKER);
        gfx.renderOutline(ebx - 2, eby - 2, ebw + 4, ebh + 4, PANEL_BORDER);

        // Scrollable list
        int listTop = centerY - 20;
        int listLeft = centerX - 100;
        int listWidth = 200;
        int totalHeight = types.size() * entryHeight;
        int visibleHeight = maxVisible * entryHeight;

        gfx.fill(listLeft, listTop, listLeft + listWidth, listTop + visibleHeight + 4, PANEL_BG);
        gfx.renderOutline(listLeft, listTop, listWidth, visibleHeight + 4, PANEL_BORDER);

        int startIndex = Math.max(0, (int) (scrollOffset / entryHeight));
        int endIndex = Math.min(types.size(), startIndex + maxVisible);

        int y = listTop + 4 - (int) (scrollOffset % entryHeight);
        for (int i = startIndex; i < endIndex; i++) {
            String type = types.get(i);
            gfx.drawString(this.font, "• " + type, listLeft + 6, y + 4, TEXT_PRIMARY, false);

            int delX = listLeft + listWidth - 26;
            int delY = y + 2;
            boolean hoverDel = mouseX >= delX && mouseX <= delX + 18 && mouseY >= delY && mouseY <= delY + 14;
            int delBg = hoverDel ? PANEL_LIGHTER : PANEL_DARKER;
            int delOutline = hoverDel ? ACCENT_RED : PANEL_BORDER;
            gfx.fill(delX, delY, delX + 18, delY + 14, delBg);
            gfx.renderOutline(delX, delY, 18, 14, delOutline);
            gfx.drawCenteredString(this.font, "🗑", delX + 9, delY + 3, TEXT_WARNING);
            y += entryHeight;
        }

        // Scrollbar
        if (totalHeight > visibleHeight) {
            float barHeight = (float) visibleHeight / totalHeight * visibleHeight;
            float barY = listTop + (scrollOffset / totalHeight) * visibleHeight;
            gfx.fill(listLeft + listWidth - 3, (int) barY, listLeft + listWidth - 1, (int) (barY + barHeight), 0x88FFFFFF);
        }

        // Themed buttons
        drawThemedButton(gfx, addButton, mouseX, mouseY, "➕ Add");

        int backY = listTop + visibleHeight + 10;
        drawThemedCenteredButton(gfx, backY, mouseX, mouseY, "← Back", () -> Minecraft.getInstance().setScreen(parent));
    }

    private void drawThemedButton(GuiGraphics gfx, Button btn, int mouseX, int mouseY, String label) {
        int bx = btn.getX(), by = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        int bg = hover ? PANEL_LIGHTER : PANEL_DARKER;
        int outline = hover ? ACCENT_BLUE : PANEL_BORDER;
        gfx.fill(bx, by, bx + bw, by + bh, bg);
        gfx.renderOutline(bx, by, bw, bh, outline);
        gfx.drawCenteredString(this.font, label, bx + bw / 2, by + 5, TEXT_PRIMARY);
    }

    private void drawThemedCenteredButton(GuiGraphics gfx, int y, int mouseX, int mouseY, String label, Runnable onClick) {
        int centerX = this.width / 2;
        int bw = 70, bh = 20, bx = centerX - bw / 2;
        boolean hover = mouseX >= bx && mouseX <= bx + bw && mouseY >= y && mouseY <= y + bh;
        int bg = hover ? PANEL_LIGHTER : PANEL_DARKER;
        int outline = hover ? ACCENT_BLUE : PANEL_BORDER;
        gfx.fill(bx, y, bx + bw, y + bh, bg);
        gfx.renderOutline(bx, y, bw, bh, outline);
        gfx.drawCenteredString(this.font, label, bx + bw / 2, y + 6, TEXT_PRIMARY);

        if (hover && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            onClick.run();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int listTop = centerY - 20;
            int listLeft = centerX - 100;
            int startIndex = Math.max(0, (int) (scrollOffset / entryHeight));
            int endIndex = Math.min(types.size(), startIndex + maxVisible);
            int y = listTop + 4 - (int) (scrollOffset % entryHeight);

            for (int i = startIndex; i < endIndex; i++) {
                int delX = listLeft + 200 - 26;
                int delY = y + 2;
                if (mouseX >= delX && mouseX <= delX + 18 && mouseY >= delY && mouseY <= delY + 14) {
                    String type = types.get(i);
                    ItemTypeRegistry.removeType(type);
                    ItemTypeRegistry.save();
                    refreshTypes(); // 🟦 Refresh after delete
                    return true;
                }
                y += entryHeight;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int totalHeight = types.size() * entryHeight;
        int visibleHeight = maxVisible * entryHeight;
        if (totalHeight > visibleHeight) {
            scrollOffset -= deltaY * 10;
            scrollOffset = Math.max(0, Math.min(scrollOffset, totalHeight - visibleHeight));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}