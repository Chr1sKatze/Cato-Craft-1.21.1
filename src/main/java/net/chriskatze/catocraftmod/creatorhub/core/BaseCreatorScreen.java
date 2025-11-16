package net.chriskatze.catocraftmod.creatorhub.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.chriskatze.catocraftmod.creatorhub.CreatorHubTheme.*;

/**
 * 🧱 Base class for all Creator Hub screens.
 * - Provides unified background, crisp text, and consistent rendering.
 * - Integrates the global CreatorHubToastManager for persistent, stackable toasts.
 */
public abstract class BaseCreatorScreen extends Screen {

    protected final Minecraft mc = Minecraft.getInstance();
    private int validationTickCounter = 0;

    protected BaseCreatorScreen(Component title) {
        super(title);
    }

    // ────────────────────────────────────────────────
    // Unified render pipeline (background, contents, toasts)
    // ────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // semi-transparent overlay background
        gfx.fill(0, 0, width, height, OVERLAY_COLOR);

        // render contents of subclass
        renderContents(gfx, mouseX, mouseY, partialTick);

        // 🔄 render global toasts (persistent across all screens)
        CreatorHubToastManager.renderAll(
                gfx,
                this.height,
                this.font.lineHeight
        );

        // render buttons and widgets
        for (Renderable widget : this.renderables) {
            widget.render(gfx, mouseX, mouseY, partialTick);
        }
    }

    // ────────────────────────────────────────────────
    // Global Toast Helpers (delegate to CreatorHubToastManager)
    // ────────────────────────────────────────────────
    protected void showSuccess(String text) { CreatorHubToastManager.showToast(text, 0xFF55FF55); }
    protected void showError(String text)   { CreatorHubToastManager.showToast(text, 0xFFFF5555); }
    protected void showWarning(String text) { CreatorHubToastManager.showToast(text, 0xFFFFAA00); }

    public static void showSuccessOn(Screen screen, String text) {
        CreatorHubToastManager.showToast(text, 0xFF55FF55);
    }
    public static void showErrorOn(Screen screen, String text) {
        CreatorHubToastManager.showToast(text, 0xFFFF5555);
    }
    public static void showWarningOn(Screen screen, String text) {
        CreatorHubToastManager.showToast(text, 0xFFFFAA00);
    }

    // ────────────────────────────────────────────────
    // Common UI utilities
    // ────────────────────────────────────────────────
    protected void drawDialogBox(GuiGraphics gfx, int centerY, String title, String subtitle, String fileName) {
        int boxLeft = this.width / 2 - 150;
        int boxTop = centerY - 90;
        int boxRight = this.width / 2 + 150;
        int boxBottom = centerY + 100;

        gfx.fill(boxLeft, boxTop, boxRight, boxBottom, PANEL_COLOR);
        gfx.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, PANEL_OUTLINE);

        int titleY = boxTop + 12;
        gfx.drawCenteredString(this.font, title, this.width / 2, titleY, TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isEmpty())
            gfx.drawCenteredString(this.font, subtitle, this.width / 2, titleY + 14, TEXT_SECONDARY);
        if (fileName != null && !fileName.isEmpty())
            gfx.drawCenteredString(this.font, fileName, this.width / 2, (this.height / 2) - 35, TEXT_WARNING);
    }

    /**
     * Periodically revalidates a text field and controls a button’s active state.
     * Treats “Maximum length” as a warning, not an error.
     */
    protected void periodicValidation(EditBox nameField, Button actionButton, java.util.function.Supplier<String> validator) {
        if (nameField == null || actionButton == null) return;

        validationTickCounter++;
        if (validationTickCounter >= 60) { // every ~3 seconds
            validationTickCounter = 0;
            String warning = validator.get();
            boolean nonBlocking = (warning != null && warning.contains("Maximum"));
            actionButton.active = (warning == null || nonBlocking);
        }
    }

    protected void drawHubPanel(GuiGraphics gfx, String title, String subtitle) {
        int centerY = this.height / 2;
        int boxLeft = this.width / 2 - 130;
        int boxTop = centerY - 80;
        int boxRight = this.width / 2 + 130;
        int boxBottom = centerY + 90;

        gfx.fill(boxLeft, boxTop, boxRight, boxBottom, PANEL_COLOR);
        gfx.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, PANEL_OUTLINE);

        int textY = boxTop + 10;
        gfx.drawCenteredString(this.font, title, this.width / 2, textY, TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isEmpty())
            gfx.drawCenteredString(this.font, subtitle, this.width / 2, textY + 14, TEXT_SECONDARY);
    }

    // ────────────────────────────────────────────────
    // Abstract + defaults
    // ────────────────────────────────────────────────
    protected abstract void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partialTick);

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {}
    @Override
    public boolean isPauseScreen() { return false; }
}