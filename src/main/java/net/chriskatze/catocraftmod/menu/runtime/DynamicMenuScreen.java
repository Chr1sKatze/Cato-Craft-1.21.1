package net.chriskatze.catocraftmod.menu.runtime;

import com.mojang.blaze3d.systems.RenderSystem;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.chriskatze.catocraftmod.creator.menu.MenuSlotDefinition;
import net.chriskatze.catocraftmod.menu.layout.SlotType;
import net.chriskatze.catocraftmod.menu.visual.GuiTextureHelper;
import net.chriskatze.catocraftmod.menu.visual.SlotTextureRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-side GUI for DynamicMenu.
 * Renders background and slots exactly as defined by the MenuLayout,
 * using runtime slot data for accurate in-game visualization.
 */
public class DynamicMenuScreen extends AbstractContainerScreen<DynamicMenu> {

    private final MenuLayout layout;

    public DynamicMenuScreen(DynamicMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.layout = menu.getLayout();

        // Layout stores pixel dimensions already.
        this.imageWidth = layout.getWidth();
        this.imageHeight = layout.getHeight();
    }

    // ────────────────────────────────────────────────
    // 🎨 Rendering
    // ────────────────────────────────────────────────
    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int left = (this.width - this.imageWidth) / 2;
        int top = (this.height - this.imageHeight) / 2;

        // 1️⃣ Draw background
        try {
            String bgPath = layout.getBackgroundTexture();
            ResourceLocation tex = GuiTextureHelper.toGuiTexture(bgPath);

            if (tex != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderTexture(0, tex);

                gfx.blit(tex, left, top, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
            } else {
                gfx.fill(left, top, left + imageWidth, top + imageHeight, 0xFF1C1E25);
            }
        } catch (Exception e) {
            CatocraftMod.LOGGER.warn("[DynamicMenu] Invalid background texture: {}", e.toString());
            gfx.fill(left, top, left + imageWidth, top + imageHeight, 0xFF1C1E25);
        }

        // 2️⃣ Draw all slots
        for (MenuSlotDefinition def : layout.getSlots()) {
            SlotType type = SlotType.fromString(def.getType());
            if (type == null) type = SlotType.INVENTORY;

            ResourceLocation tex = null;

            // Per-slot override
            if (def.getTextureOverride() != null && !def.getTextureOverride().isBlank()) {
                tex = GuiTextureHelper.toGuiTexture(def.getTextureOverride());
            }

            // Layout-wide override
            if (tex == null && layout.getSlotTextures() != null) {
                String mapped = layout.getSlotTextures().get(type.name());
                if (mapped != null && !mapped.isBlank()) {
                    tex = GuiTextureHelper.toGuiTexture(mapped);
                }
            }

            // Registry fallback
            if (tex == null) {
                tex = SlotTextureRegistry.get(type);
            }

            int sx = left + def.getX() - 1;
            int sy = top + def.getY() - 1;

            if (tex != null) {
                try {
                    RenderSystem.setShaderTexture(0, tex);
                    gfx.blit(tex, sx, sy, 0, 0, 18, 18, 18, 18);
                } catch (Exception e) {
                    CatocraftMod.LOGGER.warn("[DynamicMenu] Failed to draw slot texture {}: {}", tex, e.toString());
                }
            } else {
                // Purple-black checker fallback
                int c1 = 0xFF6B1FFF;
                int c2 = 0xFF000000;
                int s = 9;
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        int cx = sx + i * s;
                        int cy = sy + j * s;
                        int col = ((i + j) % 2 == 0) ? c1 : c2;
                        gfx.fill(cx, cy, cx + s, cy + s, col);
                    }
                }
            }
        }
    }

    // ────────────────────────────────────────────────
    // 🧭 Overall render
    // ────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);

        gui.drawString(
                this.font,
                Component.literal("Layout: " + layout.getName() +
                        " (" + layout.getWidth() + "x" + layout.getHeight() + ")"),
                this.leftPos + 8, this.topPos - 12, 0xAAAAAA, false
        );
    }

    // ────────────────────────────────────────────────
    // 🏷️ Labels
    // ────────────────────────────────────────────────
    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, 8, 6, 0xE0E0E0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}