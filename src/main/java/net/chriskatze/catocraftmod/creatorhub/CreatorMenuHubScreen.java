package net.chriskatze.catocraftmod.creatorhub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

/**
 * Menu Creator screen — opened from the Creator Hub.
 * Handles creating, overwriting, and opening layout JSONs.
 */
public class CreatorMenuHubScreen extends Screen {

    private final Screen parent;
    private boolean showNamingDialog = false;
    private boolean showOverwriteDialog = false;
    private EditBox nameField;
    private File pendingFile;

    public CreatorMenuHubScreen(Screen parent) {
        super(Component.literal("Menu Creator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        if (showNamingDialog) {
            setupNamingDialog();
            return;
        }

        if (showOverwriteDialog && pendingFile != null) {
            setupOverwriteDialog();
            return;
        }

        // 🧱 Create new layout
        this.addRenderableWidget(Button.builder(
                Component.literal("🧱 Create New Layout"),
                btn -> openNamingDialog()
        ).pos(centerX - 75, startY).size(150, 20).build());

        // 📂 Open menus folder
        this.addRenderableWidget(Button.builder(
                Component.literal("📂 Open Menus Folder"),
                btn -> openMenusFolder()
        ).pos(centerX - 75, startY + 25).size(150, 20).build());

        // 📜 Browse existing layouts
        this.addRenderableWidget(Button.builder(
                Component.literal("📜 Browse Layouts"),
                btn -> Minecraft.getInstance().setScreen(new MenuListScreen(this))
        ).pos(centerX - 75, startY + 50).size(150, 20).build());

        // ← Back
        this.addRenderableWidget(Button.builder(
                Component.literal("← Back"),
                btn -> Minecraft.getInstance().setScreen(parent)
        ).pos(centerX - 75, startY + 90).size(150, 20).build());
    }

    // ────────────────────────────────────────────────
    // Naming dialog
    // ────────────────────────────────────────────────
    private void openNamingDialog() {
        showNamingDialog = true;
        this.clearWidgets();
        setupNamingDialog();
    }

    private void setupNamingDialog() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameField = new EditBox(this.font, centerX - 75, centerY - 15, 150, 20, Component.literal("Layout Name"));
        nameField.setMaxLength(32);
        nameField.setValue("");
        this.addRenderableWidget(nameField);

        this.addRenderableWidget(Button.builder(
                Component.literal("✅ Create"),
                btn -> tryCreateLayout(nameField.getValue())
        ).pos(centerX - 75, centerY + 15).size(70, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("❌ Cancel"),
                btn -> {
                    showNamingDialog = false;
                    this.clearWidgets();
                    this.init();
                }
        ).pos(centerX + 5, centerY + 15).size(70, 20).build());
    }

    // ────────────────────────────────────────────────
    // Overwrite dialog
    // ────────────────────────────────────────────────
    private void setupOverwriteDialog() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("✅ Overwrite"),
                btn -> confirmOverwrite()
        ).pos(centerX - 75, centerY).size(70, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("❌ Cancel"),
                btn -> {
                    showOverwriteDialog = false;
                    pendingFile = null;
                    this.clearWidgets();
                    this.init();
                }
        ).pos(centerX + 5, centerY).size(70, 20).build());
    }

    private void confirmOverwrite() {
        if (pendingFile == null) return;

        writeLayoutFile(pendingFile);
        showOverwriteDialog = false;
        pendingFile = null;
        this.clearWidgets();
        this.init();
    }

    // ────────────────────────────────────────────────
    // Layout file creation
    // ────────────────────────────────────────────────
    private void tryCreateLayout(String name) {
        if (name == null || name.trim().isEmpty()) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("⚠ Please enter a name first."), false);
            return;
        }

        String safeName = name.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        File folder = getMenusFolder();

        File file = new File(folder, safeName + ".json");

        if (file.exists()) {
            pendingFile = file;
            showNamingDialog = false;
            showOverwriteDialog = true;
            this.clearWidgets();
            this.init();
            return;
        }

        writeLayoutFile(file);
    }

    private void writeLayoutFile(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            // 🧩 Write a useful JSON template
            writer.write("{\n");
            writer.write("  \"type\": \"catocraftmod:menu\",\n");
            writer.write("  \"title\": \"New Menu\",\n");
            writer.write("  \"width\": 9,\n");
            writer.write("  \"height\": 3,\n");
            writer.write("  \"slots\": []\n");
            writer.write("}");
            writer.flush();

            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("✅ Created layout: " + file.getName()), false);

            openFileSafe(file);

        } catch (IOException e) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("⚠ Failed to write file: " + e.getMessage()), false);
        }

        showNamingDialog = false;
        this.clearWidgets();
        this.init();
    }

    // ────────────────────────────────────────────────
    // Safe OS-aware file openers
    // ────────────────────────────────────────────────
    private void openMenusFolder() {
        File folder = getMenusFolder();
        folder.mkdirs();

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(folder);
                return;
            } catch (Exception ignored) {}
        }

        // fallback: show clickable file:// path
        Minecraft.getInstance().player.displayClientMessage(
                Component.literal("📁 Menus folder: " + folder.getAbsolutePath()), false);
        try {
            Desktop.getDesktop().browse(new URI("file://" + folder.getAbsolutePath()));
        } catch (Exception ignored) {}
    }

    private void openFileSafe(File file) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
                return;
            } catch (Exception ignored) {}
        }

        Minecraft.getInstance().player.displayClientMessage(
                Component.literal("📄 File created at: " + file.getAbsolutePath()), false);
    }

    private File getMenusFolder() {
        // 📦 Always resolve from game dir (not dev root)
        return new File(Minecraft.getInstance().gameDirectory, "catocraftmod_data/menus");
    }

    // ────────────────────────────────────────────────
    // Rendering
    // ────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        this.renderBackground(gfx, mouseX, mouseY, partial);

        if (showNamingDialog) {
            gfx.drawCenteredString(this.font, "Enter new layout name", this.width / 2, 40, 0xFFFFFF);
        } else if (showOverwriteDialog && pendingFile != null) {
            gfx.drawCenteredString(this.font,
                    "Layout '" + pendingFile.getName() + "' already exists!", this.width / 2, 40, 0xFFAAAA);
            gfx.drawCenteredString(this.font, "Overwrite existing file?", this.width / 2, 55, 0xFFFFFF);
        } else {
            gfx.drawCenteredString(this.font, "Menu Creator Hub", this.width / 2, 40, 0xFFFFFF);
            gfx.drawCenteredString(this.font, "Manage and create your menu layouts.", this.width / 2, 55, 0xAAAAAA);
        }

        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}