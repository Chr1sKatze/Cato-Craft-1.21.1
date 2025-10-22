package net.chriskatze.catocraftmod.creatorhub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Scrollable list of all menu layout files.
 */
public class MenuListScreen extends Screen implements ScreenWithRefresh {

    private final Screen parent;
    private MenuListWidget list;

    public MenuListScreen(Screen parent) {
        super(Component.literal("Menu Layouts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int top = 60;

        list = new MenuListWidget(Minecraft.getInstance(), this, this.width, this.height, top);
        this.addRenderableWidget(list);
        list.refresh();

        // Back button
        this.addRenderableWidget(Button.builder(
                Component.literal("← Back"),
                btn -> Minecraft.getInstance().setScreen(parent)
        ).pos(this.width / 2 - 75, this.height - 30).size(150, 20).build());
    }

    @Override
    public void refresh() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        this.renderBackground(gfx, mouseX, mouseY, partial);
        gfx.drawCenteredString(this.font, "Existing Menus", this.width / 2, 30, 0xFFFFFF);
        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}