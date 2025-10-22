package net.chriskatze.catocraftmod.creator.common;

import net.chriskatze.catocraftmod.creator.hub.CreatorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public class NameInputScreen extends Screen {
    private final CreatorType type;
    private EditBox nameField;

    public NameInputScreen(CreatorType type) {
        super(Component.literal("New " + type.displayName));
        this.type = type;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        nameField = new EditBox(this.font, cx - 75, cy - 10, 150, 20, Component.literal("Enter name"));
        this.addRenderableWidget(nameField);

        this.addRenderableWidget(Button.builder(Component.literal("Create"), b -> this.onConfirm())
                .bounds(cx - 40, cy + 20, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
            Minecraft.getInstance().setScreen(new net.chriskatze.catocraftmod.creator.hub.CreatorHubScreen());
        }).bounds(cx - 40, cy + 45, 80, 20).build());
    }

    private void onConfirm() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) {
            UIFeedback.showError("Invalid Name", "Please enter a name before continuing.");
            return;
        }

        if (ProjectFileManager.exists(type, name)) {
            Minecraft.getInstance().setScreen(new OverwriteWarningScreen(type, name));
        } else {
            createAndOpen(name);
        }
    }

    private void createAndOpen(String name) {
        try {
            ProjectFileManager.create(type, name, "{}");
            CreatorNavigator.openEditor(type, name);
        } catch (IOException e) {
            UIFeedback.showError("Create Failed", e.getMessage());
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui, mouseX, mouseY, partialTick);

        // Panel background
        gui.fill(this.width / 2 - 100, this.height / 2 - 60, this.width / 2 + 100, this.height / 2 + 70, 0x88000000);

        // Titles
        gui.drawCenteredString(this.font, "Create New " + type.displayName, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        gui.drawCenteredString(this.font, "Enter a unique name for your new file", this.width / 2, this.height / 2 - 35, 0xAAAAAA);

        super.render(gui, mouseX, mouseY, partialTick);
        nameField.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}