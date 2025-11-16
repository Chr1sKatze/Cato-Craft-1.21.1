package net.chriskatze.catocraftmod.creatorhub.menu;

import net.chriskatze.catocraftmod.creatorhub.core.BaseCreatorScreen;
import net.chriskatze.catocraftmod.creatorhub.core.SystemFileHelper;
import net.chriskatze.catocraftmod.creatorhub.menu.editor.MenuEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static net.chriskatze.catocraftmod.creatorhub.CreatorHubTheme.*;

/**
 * 🧩 Menu Creator Hub — compact style matching CreatorHubScreen.
 * - “Maximum name length” warning is visual only, not blocking.
 * - Consistent feedback and cancel behavior across dialogs.
 */
public class CreatorMenuHubScreen extends BaseCreatorScreen {

    private final Screen parent;
    private boolean showNamingDialog = false;
    private EditBox nameField;
    private Button createButton;
    private String nameError = null;

    public CreatorMenuHubScreen(Screen parent) {
        super(Component.literal("Menu Creator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (showNamingDialog) {
            setupNamingDialog();
            return;
        }

        int spacing = 24;
        int startY = centerY - 33;

        addVerticalButton(centerX, startY, Component.literal("🧱 New Layout"), this::openNamingDialog);
        addVerticalButton(centerX, startY + spacing, Component.literal("📂 Folder"),
                () -> SystemFileHelper.openInExplorer(getMenusFolder()));
        addVerticalButton(centerX, startY + spacing * 2, Component.literal("📜 Browse"),
                () -> Minecraft.getInstance().setScreen(new MenuListScreen(this)));
        addVerticalButton(centerX, startY + spacing * 3 + 10, Component.literal("← Back"),
                () -> Minecraft.getInstance().setScreen(parent));
    }

    private void addVerticalButton(int centerX, int y, Component label, Runnable action) {
        int buttonWidth = 90;
        int buttonHeight = 18;
        this.addRenderableWidget(Button.builder(label, b -> action.run())
                .pos(centerX - (buttonWidth / 2), y)
                .size(buttonWidth, buttonHeight)
                .build());
    }

    private void openNamingDialog() {
        showNamingDialog = true;
        this.clearWidgets();
        setupNamingDialog();
    }

    private void setupNamingDialog() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        nameField = new EditBox(this.font, centerX - 75, centerY - 15, 150, 20, Component.literal("Layout Name"));
        nameField.setMaxLength(25);
        this.addRenderableWidget(nameField);

        // ✅ Create button
        createButton = Button.builder(
                Component.literal("✅ Create"),
                btn -> tryCreateLayout(nameField.getValue())
        ).pos(centerX - 75, centerY + 15).size(70, 20).build();
        createButton.active = false;
        this.addRenderableWidget(createButton);

        // ❌ Cancel button — unified with all other dialogs
        this.addRenderableWidget(Button.builder(
                Component.literal("❌ Cancel"),
                btn -> {
                    showNamingDialog = false;
                    nameError = null;
                    Minecraft.getInstance().setScreen(parent);
                }
        ).pos(centerX + 5, centerY + 15).size(70, 20).build());

        // Real-time validation feedback
        nameError = validateAndReturnWarning(nameField.getValue());
        boolean tooLong = (nameError != null && nameError.contains("Maximum"));
        createButton.active = (nameError == null || tooLong);

        nameField.setResponder(text -> {
            nameError = validateAndReturnWarning(text);
            boolean tooLongNow = (nameError != null && nameError.contains("Maximum"));
            createButton.active = (nameError == null || tooLongNow);
        });
    }

    private String validateAndReturnWarning(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return "⚠ Please enter a name";
        if (trimmed.length() >= 25)
            return "⚠ Maximum name length reached (25)";

        String safeName = trimmed.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        File file = new File(getMenusFolder(), safeName + ".json");
        if (file.exists()) return "⚠ A layout with this name already exists";
        return null;
    }

    private void tryCreateLayout(String name) {
        String warning = validateAndReturnWarning(name);

        // 🟡 Only block on real errors — allow “Maximum length” warning
        if (warning != null && !warning.contains("Maximum")) {
            nameError = warning;
            createButton.active = false;
            return;
        }

        String safeName = name.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        File file = new File(getMenusFolder(), safeName + ".json");
        writeLayoutFile(file);
    }

    private void writeLayoutFile(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\n  \"type\": \"catocraftmod:menu\",\n  \"title\": \"New Menu\",\n  \"width\": 9,\n  \"height\": 3,\n  \"slots\": []\n}");
            writer.flush();

            // 🟢 Show success toast immediately
            this.showSuccess("✅ Created layout: " + file.getName());

            // 🔁 Immediately open the Menu Editor for the new file
            Minecraft.getInstance().setScreen(new MenuEditorScreen(this, file));

        } catch (IOException e) {
            this.showError("⚠ Failed to write file!");
        }
    }

    private File getMenusFolder() {
        return new File(Minecraft.getInstance().gameDirectory, "catocraftmod_data/menus");
    }

    @Override
    protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        int centerY = this.height / 2;
        int boxLeft = this.width / 2 - 150;
        int boxTop = centerY - 90;
        int boxRight = this.width / 2 + 150;
        int boxBottom = centerY + 100;

        gfx.fill(boxLeft, boxTop, boxRight, boxBottom, PANEL_COLOR);
        gfx.renderOutline(boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop, PANEL_OUTLINE);

        int titleY = boxTop + 12;
        gfx.drawCenteredString(this.font, "Menu Creator", this.width / 2, titleY, TEXT_PRIMARY);
        gfx.drawCenteredString(this.font, "Manage and create your menu layouts.", this.width / 2, titleY + 14, TEXT_SECONDARY);

        if (showNamingDialog && nameError != null) {
            int color;
            if (nameError.contains("Maximum")) color = 0xFFFFAA00;
            else if (nameError.contains("exists")) color = 0xFFFFCC00;
            else color = 0xFFFF5555;
            gfx.drawCenteredString(this.font, nameError, this.width / 2, (this.height / 2) - 35, color);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (showNamingDialog && nameField != null && createButton != null) {
            periodicValidation(nameField, createButton, () -> {
                nameError = validateAndReturnWarning(nameField.getValue());
                return nameError;
            });
        }
    }
}