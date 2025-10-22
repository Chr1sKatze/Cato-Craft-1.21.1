package net.chriskatze.catocraftmod.creator.common;


import net.chriskatze.catocraftmod.creator.hub.CreatorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OverwriteWarningScreen extends Screen {
    private final CreatorType type;
    private final String name;

    public OverwriteWarningScreen(CreatorType type, String name) {
        super(Component.literal("Overwrite Warning"));
        this.type = type;
        this.name = name;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Overwrite"), b -> {
            try {
                ProjectFileManager.create(type, name, "{}");
                CreatorNavigator.openEditor(type, name);
            } catch (Exception e) {
                UIFeedback.showError("Overwrite Failed", e.getMessage());
            }
        }).bounds(cx - 60, cy + 15, 120, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> {
            Minecraft.getInstance().setScreen(new NameInputScreen(type));
        }).bounds(cx - 60, cy + 40, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui, mouseX, mouseY, partialTick);

        // Background panel
        gui.fill(this.width / 2 - 110, this.height / 2 - 60, this.width / 2 + 110, this.height / 2 + 70, 0x88000000);

        // Titles
        gui.drawCenteredString(this.font, "File Already Exists!", this.width / 2, this.height / 2 - 45, 0xFF5555);
        gui.drawCenteredString(this.font, "Do you want to overwrite '" + name + ".json'?", this.width / 2, this.height / 2 - 30, 0xFFFFFF);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}