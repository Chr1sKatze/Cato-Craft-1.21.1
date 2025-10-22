package net.chriskatze.catocraftmod.creator.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.creator.common.JsonIO;
import net.chriskatze.catocraftmod.creator.common.ProjectFileManager;
import net.chriskatze.catocraftmod.creator.common.UIFeedback;
import net.chriskatze.catocraftmod.creator.hub.CreatorHubScreen;
import net.chriskatze.catocraftmod.creator.hub.CreatorType;
import net.chriskatze.catocraftmod.menu.runtime.ClientMenuOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Path;

public class MenuCreatorScreen extends Screen {
    private final String fileName;
    private final Path filePath;
    private MenuLayout layout;

    public MenuCreatorScreen(String fileName) {
        super(Component.literal("Menu Creator"));
        this.fileName = fileName;
        this.filePath = ProjectFileManager.getFile(CreatorType.MENU, fileName);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // --- Load existing or create new layout ---
        try {
            if (ProjectFileManager.exists(CreatorType.MENU, fileName)) {
                layout = JsonIO.load(filePath, MenuLayout.class);
            } else {
                layout = new MenuLayout(fileName, 9, 3); // default 9x3 menu
                saveLayout();
            }
        } catch (IOException e) {
            UIFeedback.showError("Load Failed", e.getMessage());
            layout = new MenuLayout(fileName, 9, 3);
        }

        int buttonWidth = 150;
        int startY = cy - 10;

        // --- Save button ---
        this.addRenderableWidget(Button.builder(Component.literal("💾 Save"), b -> saveLayout())
                .bounds(cx - buttonWidth / 2, startY, buttonWidth, 20).build());

        // --- Open Folder button ---
        this.addRenderableWidget(Button.builder(Component.literal("📂 Open Folder"), b -> onOpenFolder())
                .bounds(cx - buttonWidth / 2, startY + 30, buttonWidth, 20).build());

        // --- 🧪 Test In-Game button ---
        this.addRenderableWidget(Button.builder(Component.literal("🧪 Test In-Game"), b -> onTestInGame())
                .bounds(cx - buttonWidth / 2, startY + 60, buttonWidth, 20).build());

        // --- Back button ---
        this.addRenderableWidget(Button.builder(Component.literal("⬅ Back"), b -> {
            Minecraft.getInstance().setScreen(new CreatorHubScreen());
        }).bounds(cx - buttonWidth / 2, startY + 100, buttonWidth, 20).build());
    }

    private void saveLayout() {
        try {
            JsonIO.save(filePath, layout);
            UIFeedback.showInfo("Saved", fileName + ".json updated successfully");
        } catch (IOException e) {
            UIFeedback.showError("Save Failed", e.getMessage());
        }
    }

    private void onOpenFolder() {
        try {
            ProjectFileManager.openFolder(CreatorType.MENU);
            UIFeedback.showInfo("Opened Folder", "Showing " + CreatorType.MENU.folderName + " directory");
        } catch (IOException e) {
            UIFeedback.showError("Folder Error", e.getMessage());
        }
    }

    private void onTestInGame() {
        try {
            // Always save latest changes first
            JsonIO.save(filePath, layout);

            // Build ResourceLocation from mod id + file name
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, fileName);

            // Open menu directly in-game
            ClientMenuOpener.open(id);
            UIFeedback.showInfo("Testing Menu", "Opened '" + fileName + "' in-game");
        } catch (Exception e) {
            UIFeedback.showError("Test Failed", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui, mouseX, mouseY, partialTick);

        gui.fill(this.width / 2 - 110, this.height / 2 - 80,
                this.width / 2 + 110, this.height / 2 + 90, 0x88000000);

        gui.drawCenteredString(this.font, "Menu Creator", this.width / 2,
                this.height / 2 - 65, 0xFFFFFF);

        gui.drawCenteredString(this.font, "Editing: " + fileName + ".json",
                this.width / 2, this.height / 2 - 50, 0xAAAAAA);

        // small layout info preview
        gui.drawCenteredString(this.font,
                "Layout: " + layout.getWidth() + "x" + layout.getHeight() +
                        " | Slots: " + layout.getSlots().size(),
                this.width / 2, this.height / 2 - 30, 0x77DD77);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}