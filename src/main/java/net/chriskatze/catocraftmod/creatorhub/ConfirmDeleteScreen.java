package net.chriskatze.catocraftmod.creatorhub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;

/**
 * Crisp confirmation dialog with soft dimmed background — no blur, no scissor errors.
 */
public class ConfirmDeleteScreen extends Screen {

    private final Screen parent;
    private final File file;
    private final Runnable onConfirm;

    public ConfirmDeleteScreen(Screen parent, File file, Runnable onConfirm) {
        super(Component.literal("Confirm Delete"));
        this.parent = parent;
        this.file = file;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // ✅ Confirm button
        this.addRenderableWidget(Button.builder(
                Component.literal("✅ Delete"),
                btn -> {
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(parent);
                }
        ).pos(centerX - 75, centerY + 20).size(70, 20).build());

        // ❌ Cancel button
        this.addRenderableWidget(Button.builder(
                Component.literal("❌ Cancel"),
                btn -> Minecraft.getInstance().setScreen(parent)
        ).pos(centerX + 5, centerY + 20).size(70, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        // 🌑 Soft translucent background — dim without blur
        gfx.fill(0, 0, this.width, this.height, 0xAA000000);

        // 🧱 Opaque dialog box
        int boxLeft = this.width / 2 - 110;
        int boxTop = this.height / 2 - 50;
        int boxRight = this.width / 2 + 110;
        int boxBottom = this.height / 2 + 65;

        gfx.fill(boxLeft, boxTop, boxRight, boxBottom, 0xFF1E1E1E);
        gfx.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, 0xFF777777);

        // ✨ Draw crisp text (fully opaque area)
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 200);

        gfx.drawCenteredString(this.font,
                "Are you sure you want to delete:",
                this.width / 2, this.height / 2 - 30, 0xFFFFFFFF);

        gfx.drawCenteredString(this.font,
                file.getName(),
                this.width / 2, this.height / 2 - 15, 0xFFFF6666);

        gfx.pose().popPose();

        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // disable world blur pipeline completely
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}