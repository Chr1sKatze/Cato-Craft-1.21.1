package net.chriskatze.catocraftmod.menu.runtime;

import com.mojang.blaze3d.systems.RenderSystem;
import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.chriskatze.catocraftmod.creator.menu.MenuSlotDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI for DynamicMenu.
 * Renders background and slot layout visualization.
 */
public class DynamicMenuScreen extends AbstractContainerScreen<DynamicMenu> {

    // Uses "minecraft" automatically as the namespace
    private static final ResourceLocation DEFAULT_BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private final MenuLayout layout;

    public DynamicMenuScreen(DynamicMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.layout = menu.getLayout();

        // Adjust GUI size dynamically based on layout
        this.imageWidth = Math.max(176, layout.getWidth() * 18);
        this.imageHeight = Math.max(166, layout.getHeight() * 18 + 100);
    }

    // ────────────────────────────────────────────────
    // Rendering
    // ────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        // Background
        RenderSystem.setShaderTexture(0, DEFAULT_BG);
        gui.blit(DEFAULT_BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Slot outlines for visualization
        if (layout.getSlots() != null) {
            for (MenuSlotDefinition def : layout.getSlots()) {
                int x = leftPos + def.getX();
                int y = topPos + def.getY();

                int color = def.isOptional() ? 0x55FFAA00 : 0x55FFFFFF;
                gui.fill(x, y, x + def.getSize(), y + def.getSize(), color);
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);

        // Debug info overlay (optional)
        gui.drawString(
                this.font,
                Component.literal("Layout: " + layout.getName() +
                        " (" + layout.getWidth() + "x" + layout.getHeight() + ")"),
                this.leftPos + 8, this.topPos - 12, 0xAAAAAA, false
        );
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // Draw title and player inventory name
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        gui.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}