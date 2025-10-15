package net.chriskatze.catocraftmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.chriskatze.catocraftmod.menu.EarringMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class EarringMenuScreen extends AbstractContainerScreen<EarringMenu> {

    private static final ResourceLocation VANILLA_INVENTORY =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    private static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("catocraftmod", "textures/gui/inventory_earring.png");

    public EarringMenuScreen(EarringMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Draw vanilla inventory background
        RenderSystem.setShaderTexture(0, VANILLA_INVENTORY);
        gfx.blit(VANILLA_INVENTORY, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Draw the earring slot overlay
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, SLOT_TEXTURE);

        int slotX = x + 78;  // 7–8 pixels from the left edge (same as armor)
        int slotY = y + 8;  // aligned vertically with helmet slot
        gfx.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);

        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, this.title, 8, 6, 0x404040, false);
        gfx.drawString(font, this.playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }
}