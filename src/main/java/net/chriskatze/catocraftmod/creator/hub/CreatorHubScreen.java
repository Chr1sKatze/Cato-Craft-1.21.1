package net.chriskatze.catocraftmod.creator.hub;

import net.chriskatze.catocraftmod.creator.common.NameInputScreen;
import net.chriskatze.catocraftmod.creator.common.ProjectFileManager;
import net.chriskatze.catocraftmod.creator.common.UIFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public class CreatorHubScreen extends Screen {

    public CreatorHubScreen() {
        super(Component.literal("Creator Hub"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - (CreatorType.values().length * 25 / 2) - 10; // center buttons vertically
        int yOffset = 0;

        // Creator type buttons (Menu Creator, Block Entity Creator, etc.)
        for (CreatorType type : CreatorType.values()) {
            this.addRenderableWidget(Button.builder(Component.literal(type.displayName), b -> {
                Minecraft.getInstance().setScreen(new NameInputScreen(type));
            }).bounds(centerX - 75, startY + yOffset, 150, 20).build());

            yOffset += 25;
        }

        // Folder button
        this.addRenderableWidget(Button.builder(Component.literal("📂 Open Project Folder"), b -> {
            try {
                ProjectFileManager.openFolder(null); // open base folder
                UIFeedback.showInfo("Opened Folder", "Showing Catocraft data directory");
            } catch (IOException e) {
                UIFeedback.showError("Folder Error", e.getMessage());
            }
        }).bounds(centerX - 75, startY + yOffset + 10, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Soft dark background tint
        this.renderBackground(gui, mouseX, mouseY, partialTick);

        gui.fill(this.width / 2 - 100, this.height / 2 - 80, this.width / 2 + 100, this.height / 2 + 90, 0x88000000);

        // Centered title
        gui.drawCenteredString(this.font, "Catocraft Creator Hub", this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        gui.drawCenteredString(this.font, "Select a creation tool to begin:", this.width / 2, this.height / 2 - 55, 0xAAAAAA);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}