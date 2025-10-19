package net.chriskatze.catocraftmod.menu.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.menu.ui.LabelElement;
import net.chriskatze.catocraftmod.menu.ui.UIElement;
import net.chriskatze.catocraftmod.menu.ui.UISchema;
import net.chriskatze.catocraftmod.menu.ui.UISchemaLoader;
import net.chriskatze.catocraftmod.menu.layout.SlotLayout;
import net.chriskatze.catocraftmod.menu.layout.SlotLayoutLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

/**
 * 💠 DynamicScreen
 *
 * A fully modular, JSON-driven base class for GUIs.
 *
 * Now supports both image and color-based backgrounds:
 *   "background": "catocraftmod:textures/gui/example.png"
 *   or
 *   "background_color": "#202020"
 */
public abstract class DynamicScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected static final ResourceLocation DEFAULT_TEXTURE =
            CatocraftMod.id("textures/gui/default_container.png");

    protected SlotLayout layout;
    protected UISchema schema;
    protected ResourceLocation background;

    /** Optional JSON-defined solid or gradient color background */
    protected Integer backgroundColorTop = null;
    protected Integer backgroundColorBottom = null;

    protected int layoutWidth = 176;
    protected int layoutHeight = 166;

    protected ResourceLocation activeLayoutId;

    public DynamicScreen(T menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = layoutWidth;
        this.imageHeight = layoutHeight;
    }

    protected abstract ResourceLocation layoutId();

    // ────────────────────────────────────────────────
    // Initialization
    // ────────────────────────────────────────────────
    @Override
    protected void init() {
        super.init();
        this.loadDynamicResources();

        // Adjust from UISchema
        if (schema != null) {
            if (schema.width() > 0) this.imageWidth = schema.width();
            if (schema.height() > 0) this.imageHeight = schema.height();

            // Texture background
            this.background = schema.background();

            // Parse optional color background
            if (schema.background_color() != null && !schema.background_color().isBlank()) {
                this.backgroundColorTop = parseColor(schema.background_color(), 0xFF202020);
                this.backgroundColorBottom = this.backgroundColorTop;
            }
            if (schema.background_gradient() != null && schema.background_gradient().size() == 2) {
                this.backgroundColorTop = parseColor(schema.background_gradient().get(0), 0xFF202020);
                this.backgroundColorBottom = parseColor(schema.background_gradient().get(1), 0xFF101010);
            }
        }

        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;

        CatocraftMod.LOGGER.debug("[DynamicScreen] Initialized with layout={}, schema={}, bg={}, colorTop={}, colorBottom={}",
                layout != null ? activeLayoutId : "none",
                schema != null ? activeLayoutId : "none",
                background,
                backgroundColorTop,
                backgroundColorBottom);
    }

    protected void loadDynamicResources() {
        this.activeLayoutId = layoutId();

        if (activeLayoutId != null) {
            this.layout = SlotLayoutLoader.get(activeLayoutId);
            this.schema = UISchemaLoader.get(activeLayoutId);
        } else {
            this.layout = null;
            this.schema = null;
        }
    }

    // ────────────────────────────────────────────────
    // Rendering
    // ────────────────────────────────────────────────
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // 1️⃣ Priority: gradient or solid color background
        if (backgroundColorTop != null && backgroundColorBottom != null) {
            graphics.fillGradient(
                    leftPos, topPos,
                    leftPos + imageWidth, topPos + imageHeight,
                    backgroundColorTop,
                    backgroundColorBottom
            );
            return;
        }

        // 2️⃣ Otherwise use texture background
        ResourceLocation bg = background != null ? background : DEFAULT_TEXTURE;
        RenderSystem.setShaderTexture(0, bg);
        graphics.blit(bg, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (schema != null && schema.elements() != null) {
            renderUILabels(graphics, schema.elements());
        }

        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    protected void renderUILabels(GuiGraphics graphics, List<UIElement> elements) {
        for (UIElement e : elements) {
            if (e instanceof LabelElement label) {
                int color = 0xFFFFFF;
                try {
                    if (label.color() != null && label.color().startsWith("#")) {
                        color = Integer.parseInt(label.color().substring(1), 16);
                    }
                } catch (Exception ignored) {}

                graphics.drawString(
                        this.font,
                        Component.literal(label.text()),
                        leftPos + label.x(),
                        topPos + label.y(),
                        color,
                        label.shadow()
                );
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    // ────────────────────────────────────────────────
    // Utility
    // ────────────────────────────────────────────────
    private static int parseColor(String hex, int fallback) {
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            long parsed = Long.parseLong(clean, 16);
            if (clean.length() <= 6) parsed |= 0xFF000000L; // add full alpha
            return (int) parsed;
        } catch (Exception e) {
            return fallback;
        }
    }

    public void reloadUI() {
        loadDynamicResources();
        this.init();
        CatocraftMod.LOGGER.info("[DynamicScreen] Reloaded UI '{}'.", activeLayoutId);
    }
}