package net.chriskatze.catocraftmod.creatorhub.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;

/**
 * 💾 Confirmation dialog for overwriting an existing file — unified with rename & delete.
 * Handles only user confirmation, feedback handled by caller.
 */
public class ConfirmOverwriteScreen extends BaseCreatorScreen {

    private final Screen parent;
    private final File targetFile;
    private final Runnable onConfirm;

    public ConfirmOverwriteScreen(Screen parent, File targetFile, Runnable onConfirm) {
        super(Component.literal("Confirm Overwrite"));
        this.parent = parent;
        this.targetFile = targetFile;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // ✅ Overwrite button
        this.addRenderableWidget(Button.builder(
                Component.literal("✅ Overwrite"),
                btn -> {
                    // Run callback (caller handles rename + toast)
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(parent);
                }
        ).pos(centerX - 75, centerY + 15).size(70, 20).build());

        // ❌ Cancel button — silent cancel, no toast
        this.addRenderableWidget(Button.builder(
                Component.literal("❌ Cancel"),
                btn -> Minecraft.getInstance().setScreen(parent)
        ).pos(centerX + 5, centerY + 15).size(70, 20).build());
    }

    @Override
    protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        int centerY = this.height / 2;
        drawDialogBox(gfx, centerY,
                "Confirm Overwrite",
                "Layout already exists. Overwrite file?",
                null);

        // File name — highlighted red for emphasis
        gfx.drawCenteredString(this.font, targetFile.getName(), this.width / 2, centerY - 50, 0xFFFF5555);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}