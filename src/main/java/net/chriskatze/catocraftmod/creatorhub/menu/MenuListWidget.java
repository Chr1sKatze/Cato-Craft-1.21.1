package net.chriskatze.catocraftmod.creatorhub.menu;

import net.chriskatze.catocraftmod.creatorhub.core.BaseCreatorScreen;
import net.chriskatze.catocraftmod.creatorhub.core.ConfirmDeleteScreen;
import net.chriskatze.catocraftmod.creatorhub.CreatorHubTheme;
import net.chriskatze.catocraftmod.creator.menu.MenuFileManager;
import net.chriskatze.catocraftmod.creatorhub.menu.editor.MenuEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Files;

/**
 * 📜 Scrollable list for saved menu JSONs.
 * Refined for perfect alignment inside the Creator Hub dialog box.
 */
public class MenuListWidget extends AbstractSelectionList<MenuListWidget.MenuEntry> {

    private final Minecraft mc;
    private final BaseCreatorScreen parent;

    public MenuListWidget(Minecraft mc, BaseCreatorScreen parent, int width, int height, int top) {
        super(mc, width, height, top, 24);
        this.mc = mc;
        this.parent = parent;
        this.setRenderHeader(false, 0);
    }

    // 🚫 No background blur / vanilla fade
    @Override
    protected void renderListBackground(GuiGraphics gfx) {}

    @Override
    protected void renderDecorations(GuiGraphics gfx, int mouseX, int mouseY) {}

    /** Refreshes the list from menu folder. */
    public void refresh() {
        this.clearEntries();
        for (File file : MenuFileManager.listMenuFiles()) {
            this.addEntry(new MenuEntry(file, parent));
        }
    }

    @Override
    protected int getScrollbarPosition() {
        // Inside right edge
        return this.getX() + this.getWidth() - 6;
    }

    @Override
    public int getRowWidth() {
        return this.width - 12; // fit within box
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narration) {}

    // ────────────────────────────────────────────────
    // Tooltip rendering outside clipping region
    // ────────────────────────────────────────────────
    @Override
    public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(gfx, mouseX, mouseY, partialTick);

        // Render tooltips after list has drawn (outside scissor region)
        MenuEntry hovered = this.getHovered();
        if (hovered != null) {
            Button btn = hovered.getHoveredButton(mouseX, mouseY);
            if (btn != null) {
                Minecraft mc = Minecraft.getInstance();
                Component tooltip = switch (btn.getMessage().getString()) {
                    case "📂" -> Component.literal("Open containing folder");
                    case "🧬" -> Component.literal("Duplicate layout");
                    case "🗑" -> Component.literal("Delete layout");
                    case "✏" -> Component.literal("Rename layout");
                    default -> hovered.file != null
                            ? Component.literal("Open layout: " + hovered.file.getName())
                            : null;
                };
                if (tooltip != null)
                    gfx.renderTooltip(mc.font, tooltip, mouseX, mouseY);
            }
        }
    }

    // ────────────────────────────────────────────────
    // Entry
    // ────────────────────────────────────────────────
    public static class MenuEntry extends AbstractSelectionList.Entry<MenuEntry> {

        private final File file;
        private final BaseCreatorScreen parent;
        private final Button editBtn, openBtn, duplicateBtn, deleteBtn, renameBtn;

        public MenuEntry(File file, BaseCreatorScreen parent) {
            this.file = file;
            this.parent = parent;

            // 🖊 Edit
            this.editBtn = Button.builder(
                    Component.literal("🖊 " + file.getName().replace(".json", "")),
                    btn -> Minecraft.getInstance().setScreen(
                            new MenuEditorScreen((Screen) parent, file)
                    )
            ).pos(0, 0).size(110, 18).build();

            // ✏ Rename
            this.renameBtn = Button.builder(
                    Component.literal("✏"),
                    btn -> Minecraft.getInstance().setScreen(
                            new net.chriskatze.catocraftmod.creatorhub.core.RenameLayoutScreen(
                                    (Screen) parent, file
                            )
                    )
            ).pos(0, 0).size(26, 18).build();

            // 🧬 Duplicate
            this.duplicateBtn = Button.builder(
                    Component.literal("🧬"),
                    btn -> {
                        try {
                            File copy = duplicateFile(file);
                            BaseCreatorScreen.showSuccessOn(parent, "✅ Duplicated layout: " + copy.getName());
                            if (parent instanceof MenuListScreen listScreen) listScreen.onReopened();
                        } catch (Exception e) {
                            BaseCreatorScreen.showErrorOn(parent, "⚠ Failed to duplicate layout!");
                        }
                    }
            ).pos(0, 0).size(26, 18).build();

            // 🗑 Delete
            this.deleteBtn = Button.builder(
                    Component.literal("🗑"),
                    btn -> Minecraft.getInstance().setScreen(
                            new ConfirmDeleteScreen(
                                    (Screen) parent,
                                    file,
                                    () -> {
                                        boolean success = MenuFileManager.deleteMenu(file);
                                        if (success)
                                            BaseCreatorScreen.showErrorOn(parent, "🗑 Deleted layout: " + file.getName());
                                        else
                                            BaseCreatorScreen.showErrorOn(parent, "⚠ Failed to delete layout!");
                                        if (parent instanceof MenuListScreen listScreen) listScreen.onReopened();
                                    }
                            )
                    )
            ).pos(0, 0).size(26, 18).build();

            // 📂 Folder
            this.openBtn = Button.builder(
                    Component.literal("📂"),
                    btn -> openSystemPath(file.getParentFile())
            ).pos(0, 0).size(26, 18).build();
        }

        // Helper: return hovered button for tooltip detection
        public Button getHoveredButton(double mouseX, double mouseY) {
            if (editBtn.isMouseOver(mouseX, mouseY)) return editBtn;
            if (renameBtn.isMouseOver(mouseX, mouseY)) return renameBtn;
            if (duplicateBtn.isMouseOver(mouseX, mouseY)) return duplicateBtn;
            if (deleteBtn.isMouseOver(mouseX, mouseY)) return deleteBtn;
            if (openBtn.isMouseOver(mouseX, mouseY)) return openBtn;
            return null;
        }

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
                if (os.contains("win"))
                    new ProcessBuilder("explorer.exe", target.getAbsolutePath()).start();
                else if (os.contains("mac"))
                    new ProcessBuilder("open", target.getAbsolutePath()).start();
                else if (os.contains("nix") || os.contains("nux"))
                    new ProcessBuilder("xdg-open", target.getAbsolutePath()).start();
                else
                    throw new UnsupportedOperationException("Unsupported OS");
            } catch (Exception e) {
                Minecraft mc = Minecraft.getInstance();
                BaseCreatorScreen.showWarningOn(mc.screen, "📁 Path: " + target.getAbsolutePath());
            }
        }

        // ────────────────────────────────────────────────
        // Rendering
        // ────────────────────────────────────────────────
        @Override
        public void render(GuiGraphics gfx, int index, int y, int x, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partial) {

            int leftMargin = x + 6;
            int rightMargin = x + width - 6;

            // Centered hover highlight bar — perfectly aligned and trimmed
            if (hovered) {
                int barTop = y + (height - 18) / 2 + 1; // shift down 1px
                gfx.fill(leftMargin + 3, barTop, rightMargin - 3, barTop + 18, CreatorHubTheme.COLOR_ACCENT_HOVER);
            }

            // Layout
            int btnY = y + 2;
            int btnGap = 4;

            int right = rightMargin - 4;
            deleteBtn.setPosition(right - 26, btnY); right -= (26 + btnGap);
            duplicateBtn.setPosition(right - 26, btnY); right -= (26 + btnGap);
            renameBtn.setPosition(right - 26, btnY); right -= (26 + btnGap);
            openBtn.setPosition(right - 26, btnY); right -= (26 + btnGap);
            editBtn.setPosition(leftMargin + 2, btnY);

            // Render buttons
            editBtn.render(gfx, mouseX, mouseY, partial);
            renameBtn.render(gfx, mouseX, mouseY, partial);
            duplicateBtn.render(gfx, mouseX, mouseY, partial);
            deleteBtn.render(gfx, mouseX, mouseY, partial);
            openBtn.render(gfx, mouseX, mouseY, partial);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Minecraft mc = Minecraft.getInstance();
            mc.screen.setFocused(null);
            return editBtn.mouseClicked(mouseX, mouseY, button)
                    || renameBtn.mouseClicked(mouseX, mouseY, button)
                    || duplicateBtn.mouseClicked(mouseX, mouseY, button)
                    || deleteBtn.mouseClicked(mouseX, mouseY, button)
                    || openBtn.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            editBtn.mouseReleased(mouseX, mouseY, button);
            renameBtn.mouseReleased(mouseX, mouseY, button);
            duplicateBtn.mouseReleased(mouseX, mouseY, button);
            deleteBtn.mouseReleased(mouseX, mouseY, button);
            openBtn.mouseReleased(mouseX, mouseY, button);
            return false;
        }
    }
}