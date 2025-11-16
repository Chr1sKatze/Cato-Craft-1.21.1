package net.chriskatze.catocraftmod.creatorhub.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;

/**
 * ✏️ Rename layout screen — unified validation and feedback system.
 * - “Maximum length” warning is visual only, never blocks renaming.
 * - Consistent cancel/confirm behavior with all Creator Hub dialogs.
 */
public class RenameLayoutScreen extends BaseCreatorScreen {

    private final Screen parent;
    private final File layoutFile;
    private EditBox nameField;
    private Button saveButton;
    private String nameError = null;

    public RenameLayoutScreen(Screen parent, File layoutFile) {
        super(Component.literal("Rename Layout"));
        this.parent = parent;
        this.layoutFile = layoutFile;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Input field
        nameField = new EditBox(this.font, centerX - 75, centerY - 15, 150, 20, Component.literal("New name"));
        nameField.setValue(stripJson(layoutFile.getName()));
        nameField.setMaxLength(25);
        addRenderableWidget(nameField);
        setInitialFocus(nameField);

        // ✅ Save button
        saveButton = Button.builder(
                Component.literal("✅ Save"),
                btn -> performRename(nameField.getValue().trim())
        ).pos(centerX - 75, centerY + 15).size(70, 20).build();
        addRenderableWidget(saveButton);

        // ❌ Cancel button — unified: returns to parent screen
        addRenderableWidget(Button.builder(
                Component.literal("❌ Cancel"),
                btn -> {
                    nameError = null;
                    Minecraft.getInstance().setScreen(parent);
                }
        ).pos(centerX + 5, centerY + 15).size(70, 20).build());

        // Initial validation
        nameError = validateAndReturnWarning(nameField.getValue());
        boolean tooLong = (nameError != null && nameError.contains("Maximum"));
        saveButton.active = (nameError == null || tooLong);

        nameField.setResponder(text -> {
            nameError = validateAndReturnWarning(text);
            boolean tooLongNow = (nameError != null && nameError.contains("Maximum"));
            saveButton.active = (nameError == null || tooLongNow);
        });
    }

    private String validateAndReturnWarning(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return "⚠ Please enter a name";
        if (trimmed.length() >= 25)
            return "⚠ Maximum name length reached (25)";
        if (trimmed.equals(stripJson(layoutFile.getName())))
            return "⚠ Cannot overwrite a file with its same name";

        File newFile = new File(layoutFile.getParent(), trimmed + ".json");
        if (newFile.exists()) return "⚠ A layout with this name already exists";
        return null;
    }

    private void performRename(String newName) {
        String warning = validateAndReturnWarning(newName);

        // 🟡 Only block on real errors — allow “Maximum length” warning
        if (warning != null && !warning.contains("Maximum")) {
            nameError = warning;
            saveButton.active = false;
            return;
        }

        File newFile = new File(layoutFile.getParent(), newName + ".json");

        // ⚠ If the target file exists, ask for overwrite confirmation
        if (newFile.exists()) {
            Minecraft.getInstance().setScreen(new ConfirmOverwriteScreen(this, newFile, () -> {
                if (layoutFile.renameTo(newFile))
                    BaseCreatorScreen.showWarningOn(parent, "⚠ Overwritten: " + newName);
                else
                    BaseCreatorScreen.showErrorOn(parent, "⚠ Rename failed!");
                Minecraft.getInstance().setScreen(parent);
            }));
            return;
        }

        // ✅ Normal rename (no overwrite)
        if (layoutFile.renameTo(newFile))
            BaseCreatorScreen.showSuccessOn(parent, "✅ Renamed to " + newName);
        else
            BaseCreatorScreen.showErrorOn(parent, "⚠ Rename failed!");

        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    protected void renderContents(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        int centerY = this.height / 2;
        drawDialogBox(gfx, centerY, "Rename Layout", "Enter a new name for:", null);
        gfx.drawCenteredString(this.font, layoutFile.getName(), this.width / 2, centerY - 50, 0xFFDDDDDD);

        if (nameError != null && !nameError.isEmpty()) {
            int color;
            if (nameError.contains("Maximum"))
                color = 0xFFFFAA00;
            else if (nameError.contains("same name"))
                color = 0xFFFFCC00;
            else
                color = 0xFFFF5555;
            gfx.drawCenteredString(this.font, nameError, this.width / 2, centerY - 35, color);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (nameField != null && saveButton != null) {
            periodicValidation(nameField, saveButton, () -> {
                nameError = validateAndReturnWarning(nameField.getValue());
                return nameError;
            });
        }
    }

    private static String stripJson(String name) {
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}