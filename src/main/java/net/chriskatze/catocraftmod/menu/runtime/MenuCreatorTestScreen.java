package net.chriskatze.catocraftmod.menu.runtime;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.client.Minecraft;

public class MenuCreatorTestScreen extends AbstractContainerScreen<MenuCreatorTestMenu> {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(
            CatocraftMod.MOD_ID, "textures/gui/test_menu.png");

    public MenuCreatorTestScreen(MenuCreatorTestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;
        g.blit(BG, left, top, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "Test Menu", 8, 6, 0x4040FF, false);
    }

    public static void openTestScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new MenuCreatorTestScreen(
                new MenuCreatorTestMenu(
                        net.chriskatze.catocraftmod.menu.ModMenus.TEST_MENU.get(),
                        0,
                        mc.player.getInventory()
                ),
                mc.player.getInventory(),
                Component.literal("Menu Test")
        ));
    }
}