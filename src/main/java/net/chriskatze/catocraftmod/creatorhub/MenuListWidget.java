package net.chriskatze.catocraftmod.creatorhub;

import net.chriskatze.catocraftmod.creatorhub.data.MenuFileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Files;

/**
 * Scrollable list for displaying all menu JSON files.
 */
public class MenuListWidget extends AbstractSelectionList<MenuListWidget.MenuEntry> {

    private final Minecraft mc;
    private final ScreenWithRefresh parent;

    public MenuListWidget(Minecraft mc, ScreenWithRefresh parent, int width, int height, int top) {
        super(mc, width, height, top, 24);
        this.mc = mc;
        this.parent = parent;
    }

    // 🧱 Disable selection highlight
    @Override
    public void setSelected(MenuEntry entry) {
        // Do nothing to disable highlight box
    }

    // 🧱 Disable decorations / background
    @Override
    protected void renderDecorations(GuiGraphics gfx, int mouseX, int mouseY) {}
    @Override
    protected void renderListBackground(GuiGraphics gfx) {}

    public void refresh() {
        this.clearEntries();
        for (File file : MenuFileManager.listMenuFiles()) {
            this.addEntry(new MenuEntry(file, parent));
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width - 6;
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {}

    // ────────────────────────────────────────────────
    // Entry
    // ────────────────────────────────────────────────
    public static class MenuEntry extends AbstractSelectionList.Entry<MenuEntry> {
        private final File file;
        private final ScreenWithRefresh parent;
        private final Button editBtn;
        private final Button openBtn;
        private final Button duplicateBtn;
        private final Button deleteBtn;

        public MenuEntry(File file, ScreenWithRefresh parent) {
            this.file = file;
            this.parent = parent;

            // 🖊 Edit — open JSON file
            this.editBtn = Button.builder(
                    Component.literal("🖊 " + file.getName().replace(".json", "")),
                    btn -> openSystemPath(file)
            ).pos(0, 0).size(100, 20).build();

            // 📂 Open Folder
            this.openBtn = Button.builder(
                    Component.literal("📂"),
                    btn -> openSystemPath(file.getParentFile())
            ).pos(0, 0).size(30, 20).build();

            // 🧬 Duplicate File
            this.duplicateBtn = Button.builder(
                    Component.literal("🧬"),
                    btn -> {
                        try {
                            File copy = duplicateFile(file);
                            var mc = Minecraft.getInstance();
                            if (mc.player != null)
                                mc.player.displayClientMessage(
                                        Component.literal("✅ Duplicated to: " + copy.getName()), false);
                            parent.refresh();
                        } catch (Exception e) {
                            var mc = Minecraft.getInstance();
                            if (mc.player != null)
                                mc.player.displayClientMessage(
                                        Component.literal("⚠ Failed to duplicate: " + e.getMessage()), false);
                        }
                    }
            ).pos(0, 0).size(30, 20).build();

            // 🗑 Delete File
            this.deleteBtn = Button.builder(
                    Component.literal("🗑"),
                    btn -> Minecraft.getInstance().setScreen(
                            new ConfirmDeleteScreen(
                                    (Screen) parent,
                                    file,
                                    () -> {
                                        boolean success = MenuFileManager.deleteMenu(file);
                                        var mc = Minecraft.getInstance();
                                        if (mc.player != null)
                                            mc.player.displayClientMessage(
                                                    Component.literal(success
                                                            ? "🗑 Deleted " + file.getName()
                                                            : "⚠ Failed to delete " + file.getName()), false);
                                        parent.refresh();
                                    }
                            )
                    )
            ).pos(0, 0).size(30, 20).build();
        }

        // ────────────────────────────────────────────────
        // File helpers
        // ────────────────────────────────────────────────
        private static File duplicateFile(File original) throws Exception {
            String baseName = original.getName().replace(".json", "");
            File parent = original.getParentFile();

            File copy = new File(parent, baseName + "_copy.json");
            int i = 2;
            while (copy.exists()) {
                copy = new File(parent, baseName + "_copy" + i + ".json");
                i++;
            }

            Files.copy(original.toPath(), copy.toPath());
            return copy;
        }

        private static void openSystemPath(File target) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) new ProcessBuilder("explorer.exe", target.getAbsolutePath()).start();
                else if (os.contains("mac")) new ProcessBuilder("open", target.getAbsolutePath()).start();
                else if (os.contains("nix") || os.contains("nux"))
                    new ProcessBuilder("xdg-open", target.getAbsolutePath()).start();
            } catch (Exception e) {
                var mc = Minecraft.getInstance();
                if (mc.player != null)
                    mc.player.displayClientMessage(
                            Component.literal("📁 Path: " + target.getAbsolutePath()), false);
            }
        }

        // ────────────────────────────────────────────────
        // Rendering
        // ────────────────────────────────────────────────
        @Override
        public void render(GuiGraphics gfx, int index, int y, int x, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partial) {

            editBtn.setPosition(x + 10, y + 2);
            openBtn.setPosition(x + width - 115, y + 2);
            duplicateBtn.setPosition(x + width - 80, y + 2);
            deleteBtn.setPosition(x + width - 45, y + 2);

            editBtn.render(gfx, mouseX, mouseY, partial);
            openBtn.render(gfx, mouseX, mouseY, partial);
            duplicateBtn.render(gfx, mouseX, mouseY, partial);
            deleteBtn.render(gfx, mouseX, mouseY, partial);

            var mc = Minecraft.getInstance();
            if (editBtn.isMouseOver(mouseX, mouseY))
                gfx.renderTooltip(mc.font, Component.literal("Open layout file"), mouseX, mouseY);
            else if (openBtn.isMouseOver(mouseX, mouseY))
                gfx.renderTooltip(mc.font, Component.literal("Open folder"), mouseX, mouseY);
            else if (duplicateBtn.isMouseOver(mouseX, mouseY))
                gfx.renderTooltip(mc.font, Component.literal("Duplicate layout"), mouseX, mouseY);
            else if (deleteBtn.isMouseOver(mouseX, mouseY))
                gfx.renderTooltip(mc.font, Component.literal("Delete layout"), mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            var mc = Minecraft.getInstance();
            mc.screen.setFocused(null);

            if (editBtn.isMouseOver(mouseX, mouseY)) return editBtn.mouseClicked(mouseX, mouseY, button);
            if (openBtn.isMouseOver(mouseX, mouseY)) return openBtn.mouseClicked(mouseX, mouseY, button);
            if (duplicateBtn.isMouseOver(mouseX, mouseY)) return duplicateBtn.mouseClicked(mouseX, mouseY, button);
            if (deleteBtn.isMouseOver(mouseX, mouseY)) return deleteBtn.mouseClicked(mouseX, mouseY, button);
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            editBtn.mouseReleased(mouseX, mouseY, button);
            openBtn.mouseReleased(mouseX, mouseY, button);
            duplicateBtn.mouseReleased(mouseX, mouseY, button);
            deleteBtn.mouseReleased(mouseX, mouseY, button);
            return false;
        }
    }
}