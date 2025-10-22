package net.chriskatze.catocraftmod.creatorhub;

import net.chriskatze.catocraftmod.creator.menu.MenuCreatorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

/**
 * Main Creator Hub screen — entry point for creators.
 * Allows navigation to various editors (Menu Creator, Block Entity Creator, etc.)
 */
public class CreatorHubScreen extends Screen {

    public CreatorHubScreen() {
        super(Component.literal("Creator Hub"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        // 🧩 Menu Creator button
        this.addRenderableWidget(Button.builder(
                Component.literal("🧩 Menu Creator"),
                btn -> Minecraft.getInstance().setScreen(new CreatorMenuHubScreen(this)) // ✅ go to menu creator
        ).pos(centerX - 75, startY).size(150, 20).build());

        // ⚙ Block Entity Creator button (placeholder)
        this.addRenderableWidget(Button.builder(
                Component.literal("⚙ Block Entity Creator"),
                btn -> Minecraft.getInstance().setScreen(new PlaceholderScreen(this, "Block Entity Creator (WIP)"))
        ).pos(centerX - 75, startY + 25).size(150, 20).build());

        // 🐾 Entity Creator button (placeholder)
        this.addRenderableWidget(Button.builder(
                Component.literal("🐾 Entity Creator"),
                btn -> Minecraft.getInstance().setScreen(new PlaceholderScreen(this, "Entity Creator (WIP)"))
        ).pos(centerX - 75, startY + 50).size(150, 20).build());

        // ✖ Exit button
        this.addRenderableWidget(Button.builder(
                Component.literal("✖ Close"),
                btn -> Minecraft.getInstance().setScreen(null)
        ).pos(centerX - 75, startY + 90).size(150, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        this.renderBackground(gfx, mouseX, mouseY, partial);
        gfx.drawCenteredString(this.font, "Creator Hub", this.width / 2, 40, 0xFFFFFF);
        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ────────────────────────────────────────────────
    // 🧩 Internal helper class for WIP placeholders
    // ────────────────────────────────────────────────
    private static class PlaceholderScreen extends Screen {
        private final Screen parent;
        private final String label;

        protected PlaceholderScreen(Screen parent, String label) {
            super(Component.literal(label));
            this.parent = parent;
            this.label = label;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.addRenderableWidget(Button.builder(
                    Component.literal("← Back"),
                    btn -> Minecraft.getInstance().setScreen(parent)
            ).pos(centerX - 75, centerY + 40).size(150, 20).build());
        }

        @Override
        public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
            this.renderBackground(gfx, mouseX, mouseY, partial);
            gfx.drawCenteredString(this.font, label, this.width / 2, 40, 0xFFFFFF);
            super.render(gfx, mouseX, mouseY, partial);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}