package net.chriskatze.catocraftmod.creatorhub;


import net.chriskatze.catocraftmod.creatorhub.core.BaseCreatorScreen;
import net.chriskatze.catocraftmod.creatorhub.menu.CreatorMenuHubScreen;
import net.chriskatze.catocraftmod.menu.data.MenuLayoutLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.chriskatze.catocraftmod.creatorhub.CreatorHubTheme.*;

/**
 * 🌟 Main Creator Hub Screen — entry point for all creator tools.
 * Compact 3-column grid layout within a unified dialog box.
 */
public class CreatorHubScreen extends BaseCreatorScreen {

    public CreatorHubScreen() {
        super(TITLE_CREATOR_HUB);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Button geometry (slightly smaller and tighter)
        int btnWidth = 90;
        int btnHeight = 18;
        int spacingX = 8;
        int spacingY = 24;

        // Layout grid (3 per row)
        int totalWidth = btnWidth * 3 + spacingX * 2;
        int totalHeight = spacingY * 2 + btnHeight;
        int startX = centerX - totalWidth / 2;
        int startY = centerY - totalHeight / 2;

        int col = 0;
        int row = 0;

        // 🧩 Menu Creator
        addGridButton(startX, startY, col++, row,
                Component.literal("🧩 Menu"),
                () -> Minecraft.getInstance().setScreen(new CreatorMenuHubScreen(this)));

        // ⚙ Block Entity Creator (WIP)
        addGridButton(startX, startY, col++, row,
                Component.literal("⚙ Block Entity"),
                () -> Minecraft.getInstance().setScreen(new PlaceholderScreen(this, "Block Entity Creator (WIP)")));

        // 🐾 Entity Creator (WIP)
        addGridButton(startX, startY, col++, row,
                Component.literal("🐾 Entity"),
                () -> Minecraft.getInstance().setScreen(new PlaceholderScreen(this, "Entity Creator (WIP)")));

        // Reset for second row
        col = 0;
        row++;

        // 🗣 Dialogue Creator (future)
        addGridButton(startX, startY, col++, row,
                Component.literal("🗣 Dialogue"),
                () -> Minecraft.getInstance().setScreen(new PlaceholderScreen(this, "Dialogue Creator (WIP)")));

        // 📜 Quest Creator (future)
        addGridButton(startX, startY, col++, row,
                Component.literal("📜 Quest"),
                () -> Minecraft.getInstance().setScreen(new PlaceholderScreen(this, "Quest Creator (WIP)")));

        // ✖ Exit
        addGridButton(startX, startY, col++, row,
                Component.literal("✖ Close"),
                () -> Minecraft.getInstance().setScreen(null));

        // ────────────────────────────────────────────────
        // 🔄 Reload Layouts button (bottom right corner)
        // ────────────────────────────────────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("🔄 Reload Layouts"),
                b -> {
                    MenuLayoutLoader.reload();
                    // Use UIFeedback if available; fallback to chat message
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("🔄 Reloaded all menu layouts."), true);
                    }
                }
        ).pos(this.width - 140, this.height - 30).size(130, 20).build());
    }

    /** Helper to position buttons in a 3-column grid */
    private void addGridButton(int startX, int startY, int col, int row, Component label, Runnable action) {
        int btnWidth = 90;
        int btnHeight = 18;
        int spacingX = 8;
        int spacingY = 24;
        int x = startX + col * (btnWidth + spacingX);
        int y = startY + row * spacingY;

        this.addRenderableWidget(Button.builder(label, b -> action.run())
                .pos(x, y)
                .size(btnWidth, btnHeight)
                .build());
    }

    @Override
    protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Keep box dimensions from last version
        int centerY = this.height / 2;
        int boxLeft = this.width / 2 - 150;
        int boxTop = centerY - 90;
        int boxRight = this.width / 2 + 150;
        int boxBottom = centerY + 100;

        // Background panel
        gfx.fill(boxLeft, boxTop, boxRight, boxBottom, PANEL_COLOR);
        gfx.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, PANEL_OUTLINE);

        // Header text
        int titleY = boxTop + 12;
        gfx.drawCenteredString(this.font, "Creator Hub", this.width / 2, titleY, TEXT_PRIMARY);
        gfx.drawCenteredString(this.font, "Choose a creation tool to begin.", this.width / 2, titleY + 14, TEXT_SECONDARY);
    }

    // ─────────────────────────────
    // Placeholder (for WIP tools)
    // ─────────────────────────────
    private static class PlaceholderScreen extends BaseCreatorScreen {
        private final Screen parent;
        private final String label;

        protected PlaceholderScreen(Screen parent, String label) {
            super(Component.literal(label));
            this.parent = parent;
            this.label = label;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            this.addRenderableWidget(Button.builder(
                    Component.literal("← Back"),
                    btn -> Minecraft.getInstance().setScreen(parent)
            ).pos(centerX - 75, centerY + 70).size(150, 20).build()); // ⬅ same button baseline as in hub
        }

        @Override
        protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int centerY = this.height / 2;
            int boxLeft = this.width / 2 - 150;
            int boxTop = centerY - 90;
            int boxRight = this.width / 2 + 150;
            int boxBottom = centerY + 100;

            gfx.fill(boxLeft, boxTop, boxRight, boxBottom, PANEL_COLOR);
            gfx.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, PANEL_OUTLINE);

            int titleY = boxTop + 12;
            gfx.drawCenteredString(this.font, label, this.width / 2, titleY, TEXT_PRIMARY);
            gfx.drawCenteredString(this.font, "This feature is not yet implemented.", this.width / 2, titleY + 14, TEXT_SECONDARY);
        }
    }
}