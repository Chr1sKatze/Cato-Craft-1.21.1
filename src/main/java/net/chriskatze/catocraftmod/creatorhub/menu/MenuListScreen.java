package net.chriskatze.catocraftmod.creatorhub.menu;

import net.chriskatze.catocraftmod.creatorhub.core.BaseCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.chriskatze.catocraftmod.creatorhub.CreatorHubTheme.*;

/**
 * 📜 Menu List Screen — unified look with Creator Menu Hub.
 * - Same 18 px button height
 * - Dialog box aligned with hub screen
 * - Perfect highlight & scroll area alignment
 */
public class MenuListScreen extends BaseCreatorScreen {

    private final Screen parent;
    private MenuListWidget listWidget;
    private int tickCounter = 0;

    private int boxLeft, boxTop, boxRight, boxBottom;

    public MenuListScreen(Screen parent) {
        super(Component.literal("Browse Menus"));
        this.parent = parent;
    }

    // ────────────────────────────────────────────────
    @Override
    protected void init() {
        int boxWidth = 300;
        int boxHeight = 190;

        // 🔽 Shift box down by ~6 px for alignment with other screens
        boxLeft = (this.width - boxWidth) / 2;
        boxTop = (this.height - boxHeight) / 2 + 5;
        boxRight = boxLeft + boxWidth;
        boxBottom = boxTop + boxHeight;

        // 📜 Scroll area inside dialog
        int listLeft = boxLeft + 12;
        int listRight = boxRight - 12;
        int listTop = boxTop + 44;
        int listBottom = boxBottom - 44;

        this.listWidget = new MenuListWidget(
                this.minecraft,
                this,
                listRight - listLeft,
                listBottom - listTop,
                listTop
        ) {
            @Override
            protected int getScrollbarPosition() {
                // Scrollbar inside right edge
                return this.getX() + this.getWidth() - 6;
            }
        };

        this.listWidget.setX(listLeft);
        this.listWidget.refresh();
        this.addRenderableWidget(this.listWidget);

        // ← Back button — same size as CreatorMenuHubScreen buttons
        int buttonWidth = 90;
        int buttonHeight = 18;
        int buttonY = boxBottom - 26;
        this.addRenderableWidget(Button.builder(
                        Component.literal("← Back"),
                        btn -> Minecraft.getInstance().setScreen(parent)
                ).pos(this.width / 2 - (buttonWidth / 2), buttonY)
                .size(buttonWidth, buttonHeight)
                .build());
    }

    // ────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        if (tickCounter >= 60) {
            tickCounter = 0;
            if (listWidget != null) listWidget.refresh();
        }
    }

    // ────────────────────────────────────────────────
    @Override
    protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Dialog box
        gfx.fill(boxLeft, boxTop, boxRight, boxBottom, PANEL_COLOR);
        gfx.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, PANEL_OUTLINE);

        // Header text centered within box
        int titleY = boxTop + 12;
        int textCenter = (boxLeft + boxRight) / 2;
        gfx.drawCenteredString(this.font, "Browse Menus", textCenter, titleY, TEXT_PRIMARY);
        gfx.drawCenteredString(this.font, "Manage your saved menu layouts.", textCenter, titleY + 14, TEXT_SECONDARY);

        // Inner highlight for list region
        int listTop = boxTop + 40;
        int listBottom = boxBottom - 38;
        gfx.fill(boxLeft + 10, listTop, boxRight - 10, listBottom, 0x22000000);
    }

    /** Ensures refreshed list when returning from editor. */
    public void onReopened() {
        if (listWidget != null) listWidget.refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}