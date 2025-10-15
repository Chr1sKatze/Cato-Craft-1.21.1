package net.chriskatze.catocraftmod.enchantment.custom;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

/**
 * Custom AnvilScreen that hides the rename box and labels,
 * but keeps the vanilla textures (already replaced in assets).
 */
public class CleanAnvilScreen extends AnvilScreen {

    public CleanAnvilScreen(AnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void renderFg(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Skip all foreground text labels entirely
    }
}