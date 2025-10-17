package net.chriskatze.catocraftmod.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.EarringMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Earring Menu with the extra earring slot and a small
 * visual safety guard against ghost crafting results.
 */
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

    // ------------------------------------------------------------
    //  🔹 Background Rendering
    // ------------------------------------------------------------
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

        int slotX = x + 78; // aligned with your menu slot
        int slotY = y + 8;
        gfx.blit(SLOT_TEXTURE, slotX, slotY, 0, 0, 18, 18, 18, 18);

        RenderSystem.disableBlend();
    }

    // ------------------------------------------------------------
    //  🔹 Ghost Item Cleanup (runs once per tick while screen is open)
    // ------------------------------------------------------------
    @Override
    public void containerTick() {
        super.containerTick();

        if (menu == null || menu.slots.size() <= EarringMenu.EARRING_SLOT_INDEX)
            return;

        try {
            Slot resultSlot = menu.slots.get(0); // crafting result slot
            Slot earringSlot = menu.slots.get(menu.EARRING_SLOT_INDEX);
            ItemStack resultStack = resultSlot.getItem();
            ItemStack earringStack = earringSlot.getItem();

            // Skip if there's no ghost risk
            if (resultStack.isEmpty() || earringStack.isEmpty()) return;

            // ✅ Only clear if:
            // 1. Result visually equals earring
            // 2. Crafting grid has no items (no valid recipe)
            boolean gridEmpty = true;
            for (Slot s : menu.slots) {
                // Crafting grid slots are 1–4
                if (s.index >= 1 && s.index <= 4 && !s.getItem().isEmpty()) {
                    gridEmpty = false;
                    break;
                }
            }

            if (gridEmpty && resultStack.is(earringStack.getItem())) {
                resultSlot.set(ItemStack.EMPTY);
                resultSlot.setChanged();
                CatocraftMod.LOGGER.debug("[EarringMenuScreen] Cleared ghost result (grid empty, matched earring)");
            }

        } catch (Exception ex) {
            // quietly ignore any slot misalignment
        }
    }

    // ------------------------------------------------------------
    //  🔹 Label + Tooltip Rendering
    // ------------------------------------------------------------
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