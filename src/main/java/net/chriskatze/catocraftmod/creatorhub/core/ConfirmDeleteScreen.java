package net.chriskatze.catocraftmod.creatorhub.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.chriskatze.catocraftmod.creatorhub.CreatorHubTheme.*;

import java.io.File;

/**
 * 🗑 Confirmation dialog for file deletion — unified with all Creator Hub dialogs.
 * Only handles confirmation UI; parent (MenuListWidget) handles actual deletion + toast.
 */
public class ConfirmDeleteScreen extends BaseCreatorScreen {

    private final Screen parent;
    private final File file;
    private final Runnable onConfirm;

    public ConfirmDeleteScreen(Screen parent, File file, Runnable onConfirm) {
        super(TITLE_CONFIRM_DELETE);
        this.parent = parent;
        this.file = file;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // ✅ Delete button — confirm only (no toast here)
        this.addRenderableWidget(Button.builder(
                Component.literal("✅ Delete"),
                btn -> {
                    onConfirm.run(); // caller handles toast + refresh
                    Minecraft.getInstance().setScreen(parent);
                }
        ).pos(centerX - 75, centerY + 15).size(70, 20).build());

        // ❌ Cancel button — silent cancel
        this.addRenderableWidget(Button.builder(
                Component.literal("❌ Cancel"),
                btn -> Minecraft.getInstance().setScreen(parent)
        ).pos(centerX + 5, centerY + 15).size(70, 20).build());
    }

    @Override
    protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        int centerY = this.height / 2;
        drawDialogBox(gfx, centerY,
                "Confirm Deletion",
                "Are you sure you want to delete:",
                null);

        // File name — highlighted yellow (warning)
        gfx.drawCenteredString(this.font, file.getName(), this.width / 2, centerY - 50, TEXT_WARNING);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
