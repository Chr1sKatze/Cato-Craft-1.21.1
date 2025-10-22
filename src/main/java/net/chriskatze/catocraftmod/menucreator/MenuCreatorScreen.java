package net.chriskatze.catocraftmod.menucreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu Creator UI
 * - Grid step: 2px minors, 40px majors (1-18-2-18-1)
 * - Slot size 18x18, 2px minimum gap to slots & canvas border
 * - Dragged slots stop on collision and slide along edges
 * - Resize handle: shows near-cursor, gold on near, orange while dragging
 * - Left control panel with menu name + width/height + Features toggle
 * - Live autosave on all edits (with small debounce)
 */
public class MenuCreatorScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_DIR = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("catocraft/menu_creator");

    private final MenuCreatorDefinition def;
    private final boolean isMultiplayer = !Minecraft.getInstance().hasSingleplayerServer();

    // Canvas placement
    private int canvasX, canvasY;
    private int canvasW, canvasH;

    // Resize handle
    private static final int HANDLE_SIZE = 8;
    private boolean resizing = false;

    // Dragging a slot
    private MenuCreatorSlotDefinition draggingSlot = null;
    private int dragOffsetX, dragOffsetY;

    // Grid / slots
    private static final int CANVAS_GRID_STEP = 2;
    private static final int SLOT_GRID_STEP = 2;
    private static final int MIN_GAP = 2;
    private static final int SLOT_W = 18, SLOT_H = 18;

    private final List<SlotView> slotViews = new ArrayList<>();

    // UI fields
    private EditBox menuNameField;
    private EditBox widthField;
    private EditBox heightField;

    // UI toggles
    private boolean showFeatures = false;

    // Sidebar toggle & widgets
    private boolean showSidebar = false; // start hidden by default
    private final List<GuiEventListener> sidebarWidgets = new ArrayList<>();

    // Scroll state + feature list (for features popup)
    private int scrollOffset = 0;
    private final List<String> featureList = List.of(
            "Slot", "Result Slot", "Upgrade Slot", "Pet Slot",
            "Accessory Slot", "Armor Slot", "Trinket Slot",
            "Rune Slot", "Custom Slot", "Magic Slot", "Craft Slot", "Output Slot"
    );

    // Autosave debounce
    private long lastSaveTime = 0L;
    private static final long SAVE_INTERVAL_MS = 800;

    private boolean skipNextLoad = false;

    public MenuCreatorScreen(MenuCreatorDefinition def) {
        super(Component.literal("Menu Creator"));
        this.def = def;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // no blur/tint background
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.releaseMouse();
        showSidebar = false; // hidden at start

        this.canvasW = Math.max(64, def.width);
        this.canvasH = Math.max(64, def.height);
        layoutCanvas();

        slotViews.clear();
        if (def.slots.isEmpty()) {
            def.slots.add(new MenuCreatorSlotDefinition("slot_1", 8, 8));
        }
        for (MenuCreatorSlotDefinition s : def.slots)
            slotViews.add(new SlotView(this, s));

        // Create folder if missing
        try { Files.createDirectories(SAVE_DIR); } catch (IOException ignored) {}

        boolean existing = Files.exists(SAVE_DIR.resolve(def.menuId + ".json"));
        if (def.menuId == null || def.menuId.isBlank()) def.menuId = "custom_menu";

        // --- Menu name field ---
        menuNameField = new EditBox(this.font, 8, 26, 84, 12, Component.literal("Menu ID"));
        menuNameField.setValue(def.menuId);
        menuNameField.setResponder(newValue -> {
            if (!newValue.isBlank() && !newValue.equals(def.menuId)) {
                def.menuId = newValue;
                autoSave();
            }
        });
        addRenderableWidget(menuNameField);
        sidebarWidgets.add(menuNameField);

        // --- Width / Height fields ---
        widthField = new EditBox(this.font, 50, 68, 32, 12, Component.literal("W"));
        heightField = new EditBox(this.font, 50, 86, 32, 12, Component.literal("H"));
        widthField.setValue(String.valueOf(canvasW));
        heightField.setValue(String.valueOf(canvasH));
        widthField.setFilter(s -> s.matches("\\d*"));
        heightField.setFilter(s -> s.matches("\\d*"));
        widthField.setResponder(val -> {
            def.width = parseOr(canvasW, val);
            autoSave();
        });
        heightField.setResponder(val -> {
            def.height = parseOr(canvasH, val);
            autoSave();
        });
        addRenderableWidget(widthField);
        addRenderableWidget(heightField);
        sidebarWidgets.add(widthField);
        sidebarWidgets.add(heightField);

        // --- Features button ---
        Button featuresBtn = Button.builder(Component.literal("Features"), b -> showFeatures = !showFeatures)
                .bounds(8, 106, 84, 16)
                .build();
        addRenderableWidget(featuresBtn);
        sidebarWidgets.add(featuresBtn);

        // --- Reset button ---
        Button resetBtn = Button.builder(Component.literal("Reset"), b -> resetMenu())
                .bounds(8, 126, 84, 16)
                .build();
        addRenderableWidget(resetBtn);
        sidebarWidgets.add(resetBtn);

        // Hide sidebar widgets initially
        for (var w : sidebarWidgets)
            if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw)
                aw.visible = showSidebar;

        // Prompt for name if it's a new or reset menu
        if (!existing || def.menuId.equals("custom_menu")) {
            showSidebar = true;
            for (var w : sidebarWidgets)
                if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw)
                    aw.visible = true;

            menuNameField.setFocused(true);
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                    "§7Please enter a name for your new menu."));
        }

        // ✅ Skip loading old file after reset
        if (!skipNextLoad) {
            loadMenuState(def.menuId);
        } else {
            skipNextLoad = false; // consume flag so next open will work normally
        }
    }

    private void resetMenu() {
        try {
            // Delete old save file if it exists
            Path oldFile = SAVE_DIR.resolve(def.menuId + ".json");
            if (Files.exists(oldFile)) {
                Files.delete(oldFile);
                CatocraftMod.LOGGER.info("[MenuCreator] Deleted old save: " + oldFile);
            }
        } catch (IOException e) {
            CatocraftMod.LOGGER.warn("[MenuCreator] Could not delete old save file", e);
        }

        // Clear data
        def.menuId = "custom_menu";
        def.width = 128;
        def.height = 128;
        def.slots.clear();
        slotViews.clear();
        slotViews.add(new SlotView(this, new MenuCreatorSlotDefinition("slot_1", 8, 8)));

        // Update UI
        this.canvasW = def.width;
        this.canvasH = def.height;
        layoutCanvas();

        if (menuNameField != null) menuNameField.setValue(def.menuId);
        if (widthField != null) widthField.setValue(String.valueOf(canvasW));
        if (heightField != null) heightField.setValue(String.valueOf(canvasH));

        // Reopen sidebar
        showSidebar = true;
        for (var w : sidebarWidgets)
            if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw)
                aw.visible = true;

        // Prompt for new name
        menuNameField.setFocused(true);
        Minecraft.getInstance().player.sendSystemMessage(Component.literal(
                "§cMenu reset. Please enter a new name."));

        // Skip loading old file on next init
        skipNextLoad = true;

        // Save empty fresh definition
        autoSave();
    }

    // =================
    // Save / Load / Auto
    // =================

    private void autoSave() {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime > SAVE_INTERVAL_MS) {
            saveMenuState();
            lastSaveTime = now;
        }
    }

    private void saveMenuState() {
        try {
            Files.createDirectories(SAVE_DIR);

            // Pull latest UI values when present
            if (menuNameField != null) def.menuId = menuNameField.getValue();
            if (widthField != null) def.width = parseOr(canvasW, widthField.getValue());
            else def.width = canvasW;
            if (heightField != null) def.height = parseOr(canvasH, heightField.getValue());
            else def.height = canvasH;

            Path file = SAVE_DIR.resolve(def.menuId + ".json");
            Files.writeString(file, GSON.toJson(def));
            CatocraftMod.LOGGER.info("[MenuCreator] Saved menu to " + file);
        } catch (IOException e) {
            CatocraftMod.LOGGER.error("[MenuCreator] Failed to save menu: ", e);
        }
    }

    private void loadMenuState(String name) {
        if (name == null || name.isBlank()) return;
        try {
            Path file = SAVE_DIR.resolve(name + ".json");
            if (Files.exists(file)) {
                String json = Files.readString(file);
                MenuCreatorDefinition loaded = GSON.fromJson(json, MenuCreatorDefinition.class);
                if (loaded != null) {
                    def.menuId = loaded.menuId;
                    def.width = loaded.width;
                    def.height = loaded.height;
                    def.slots = loaded.slots;
                    slotViews.clear();
                    for (MenuCreatorSlotDefinition s : def.slots)
                        slotViews.add(new SlotView(this, s));
                    this.canvasW = def.width;
                    this.canvasH = def.height;
                    layoutCanvas();
                    CatocraftMod.LOGGER.info("[MenuCreator] Loaded existing menu: " + name);
                }
            }
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[MenuCreator] Failed to load menu: ", e);
        }
    }

    // =========
    // Keyboard
    // =========

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            // Save before toggling so user never loses edits
            saveMenuState();

            showSidebar = !showSidebar;

            // Close features panel when hiding sidebar
            if (!showSidebar) {
                showFeatures = false;
            }
            // Hide/show all sidebar widgets (no rebuild)
            for (GuiEventListener widget : sidebarWidgets) {
                if (widget instanceof net.minecraft.client.gui.components.AbstractWidget w) {
                    w.visible = showSidebar;
                }
            }
            // When reopening the sidebar, restore the last saved data
            if (showSidebar) {
                if (menuNameField != null) menuNameField.setValue(def.menuId);
                if (widthField != null) widthField.setValue(String.valueOf(def.width));
                if (heightField != null) heightField.setValue(String.valueOf(def.height));
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_A) {
            addTestSlot();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveMenuState(); // ensure safe save before closing
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (widthField != null && heightField != null &&
                    (widthField.isFocused() || heightField.isFocused())) {
                applyCanvasSizeFromFields();
                autoSave(); // save size confirm
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void layoutCanvas() {
        // Center the canvas in the window (independent of the left panel)
        this.canvasX = snapFloor((this.width - canvasW) / 2, CANVAS_GRID_STEP);
        this.canvasY = snapFloor((this.height - canvasH) / 2, CANVAS_GRID_STEP);
    }

    // ======
    // Render
    // ======

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);

        int panelWidth = 100;
        int white = 0xFFFFFF;
        int grey = 0xAAAAAA;
        int smallGrey = 0x777777;

        // --- Always visible top header ---
        g.fill(0, 0, panelWidth, 40, 0xAA000000); // header bg
        // --- Always visible top header ---
        g.fill(0, 0, panelWidth, 40, 0xAA000000); // header background
        g.drawString(this.font, "Menu Creator", 10, 10, white, false);

        // show current menu name under it
        if (def != null && def.menuId != null)
            g.drawString(this.font, "(" + def.menuId + ")", 10, 22, 0xFFAAAAAA, false);

        g.hLine(8, panelWidth - 8, 20, 0xFFFFFFFF);

        if (!showSidebar) {
            // Hint below title when sidebar is hidden (smaller + tucked under line)
            float scale = 0.7f;
            g.pose().pushPose();
            g.pose().scale(scale, scale, 1f);
            g.drawString(this.font, "Press TAB for settings",
                    (int)(10 / scale), (int)(30 / scale), smallGrey, false);
            g.pose().popPose();
        } else {
            // --- Sidebar visible ---
            g.fill(0, 40, panelWidth, this.height, 0xAA000000);

            float scale = 0.9f;
            g.pose().pushPose();
            g.pose().scale(scale, scale, 1f);

            // Naming convention
            g.drawString(this.font, "Naming convention:", (int)(8 / scale), (int)(44 / scale), smallGrey, false);
            g.drawString(this.font, "custom_menu",      (int)(8 / scale), (int)(55 / scale), smallGrey, false);

            // Labels
            g.drawString(this.font, "Width:",  (int)(8 / scale), (int)(74 / scale), grey, false);
            g.drawString(this.font, "Height:", (int)(8 / scale), (int)(92 / scale), grey, false);

            g.pose().popPose();

            // --- Features popup (fills to bottom, borders aligned perfectly) ---
            if (showFeatures) {
                int px = 0, py = 132;
                int pw = panelWidth;
                int ph = this.height - py;

                // background
                g.fill(px, py, px + pw, py + ph, 0xCC111111);

                // borders drawn INSIDE the fill area (inclusive end coords)
                g.hLine(px,         px + pw - 1, py,          0xFF666666);        // top
                g.hLine(px,         px + pw - 1, py + ph - 1, 0xFF666666);        // bottom
                g.vLine(px,         py,          py + ph - 1, 0xFF666666);        // left
                g.vLine(px + pw - 1,py,          py + ph - 1, 0xFF666666);        // right (flush with tint edge)

                g.drawString(this.font, "Slot Features", px + 6, py + 6, white, false);

                // Clip scrolling content inside the panel
                int contentTop = py + 20;
                int contentBottom = py + ph - 4;
                g.enableScissor(px, contentTop, px + pw, contentBottom);

                int y = contentTop - scrollOffset;
                for (int i = 0; i < featureList.size(); i++) {
                    String feature = featureList.get(i);
                    g.drawString(this.font, feature, px + 8, y, 0xFFDDDDDD, false);
                    y += 14;
                }

                g.disableScissor();
            }
        }

        // --- Canvas + slots ---
        drawCanvas(g);
        drawGrid(g);
        for (SlotView view : slotViews)
            view.render(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
        drawHandle(g, mouseX, mouseY);
    }

    // ==========
    // Mouse wheel
    // ==========

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double delta = deltaY; // vertical scroll
        if (showFeatures && mouseX < 100) { // scroll only inside left panel area
            scrollOffset -= delta * 10;
            scrollOffset = Mth.clamp(scrollOffset, 0,
                    Math.max(0, featureList.size() * 14 - (this.height - 150)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    // =======
    // Canvas
    // =======

    private void drawCanvas(GuiGraphics g) {
        g.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, 0xAA000000);
        int border = 0xFF666666;
        g.hLine(canvasX, canvasX + canvasW, canvasY, border);
        g.hLine(canvasX, canvasX + canvasW, canvasY + canvasH, border);
        g.vLine(canvasX, canvasY, canvasY + canvasH, border);
        g.vLine(canvasX + canvasW, canvasY, canvasY + canvasH, border);
    }

    private void drawGrid(GuiGraphics g) {
        int minor = 0x44333333;  // subtle minors
        int major = 0x88555555;  // strong majors

        int smallStep = 2;       // every 2 px
        int largeStep = 40;      // every 40 px (1 + 18 + 2 + 18 + 1)
        int offset = 1;          // centers the 40px blocks visually

        // Vertical
        for (int x = offset; x <= canvasW + offset; x += smallStep) {
            boolean isMajor = ((x - offset) % largeStep == 0);
            int color = isMajor ? major : minor;
            g.vLine(canvasX + x, canvasY + 1, canvasY + canvasH - 1, color);
        }
        // Horizontal
        for (int y = offset; y <= canvasH + offset; y += smallStep) {
            boolean isMajor = ((y - offset) % largeStep == 0);
            int color = isMajor ? major : minor;
            g.hLine(canvasX + 1, canvasX + canvasW - 1, canvasY + y, color);
        }
    }

    private void drawHandle(GuiGraphics g, int mouseX, int mouseY) {
        int baseSize = HANDLE_SIZE - 2;
        int size = baseSize - 1;

        // snug into the canvas corner (on top of the border), 1px inward for the smaller size
        int cx = canvasX + canvasW - baseSize + 1;
        int cy = canvasY + canvasH - baseSize + 1;

        // show only when cursor is close
        double dist = Math.hypot(mouseX - (cx + size / 2.0), mouseY - (cy + size / 2.0));
        if (dist > 10) return;

        boolean active = resizing;

        int goldColor   = 0xFFFFD45A; // near
        int orangeColor = 0xFFFFA500; // dragging
        int borderColor = 0xFF666666;

        int fill = active ? orangeColor : goldColor;

        // Fill
        g.fill(cx, cy, cx + size, cy + size, fill);

        // Outline only on top + left sides (avoid overlapping canvas right/bottom borders)
        g.fill(cx, cy, cx + size, cy + 1, borderColor); // top
        g.fill(cx, cy, cx + 1, cy + size, borderColor); // left
    }

    // =========================
    // Canvas sizing & dragging
    // =========================

    private void applyCanvasSizeFromFields() {
        try {
            if (widthField == null || heightField == null) return;

            int[] minSize = getMinCanvasSize();
            int minW = minSize[0];
            int minH = minSize[1];

            int newW = snapFloor(Mth.clamp(parseOr(canvasW, widthField.getValue()), minW, 512), CANVAS_GRID_STEP);
            int newH = snapFloor(Mth.clamp(parseOr(canvasH, heightField.getValue()), minH, 512), CANVAS_GRID_STEP);

            this.canvasW = newW;
            this.canvasH = newH;
            def.width = newW;
            def.height = newH;

            layoutCanvas();
            widthField.setValue(String.valueOf(canvasW));
            heightField.setValue(String.valueOf(canvasH));

            autoSave();
        } catch (Exception ignored) {}
    }

    private static int parseOr(int fallback, String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        boolean clickedOutside =
                (widthField == null || !widthField.isMouseOver(mouseX, mouseY)) &&
                        (heightField == null || !heightField.isMouseOver(mouseX, mouseY));

        if (clickedOutside) {
            if (widthField != null) widthField.setFocused(false);
            if (heightField != null) heightField.setFocused(false);
            applyCanvasSizeFromFields(); // autoSave happens inside
        }

        if (button == 0) {
            if (overHandle(mouseX, mouseY)) {
                resizing = true;
                return true;
            }
            if (insideCanvas(mouseX, mouseY)) {
                for (int i = slotViews.size() - 1; i >= 0; i--) {
                    SlotView view = slotViews.get(i);
                    if (view.over(mouseX, mouseY)) {
                        draggingSlot = view.data;
                        dragOffsetX = (int) mouseX - (canvasX + draggingSlot.x);
                        dragOffsetY = (int) mouseY - (canvasY + draggingSlot.y);
                        return true;
                    }
                }
                return true;
            }
        }

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            resizing = false;
            draggingSlot = null;
            applyCanvasSizeFromFields(); // includes autoSave
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Smallest allowed canvas size based on placed slots (+2px gap).
     * Base minimum is 22x22.
     */
    private int[] getMinCanvasSize() {
        int minW = 22; // base minimum canvas width
        int minH = 22; // base minimum canvas height

        for (SlotView view : slotViews) {
            int right = view.data.x + SLOT_W + MIN_GAP;
            int bottom = view.data.y + SLOT_H + MIN_GAP;
            minW = Math.max(minW, right);
            minH = Math.max(minH, bottom);
        }

        // Snap to grid for a clean edge
        minW = snapFloor(minW, CANVAS_GRID_STEP);
        minH = snapFloor(minH, CANVAS_GRID_STEP);
        return new int[]{minW, minH};
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0) {
            if (resizing) {
                int[] minSize = getMinCanvasSize();
                int minW = minSize[0];
                int minH = minSize[1];

                int newW = snapFloor((int) mouseX - canvasX, CANVAS_GRID_STEP);
                int newH = snapFloor((int) mouseY - canvasY, CANVAS_GRID_STEP);

                // Clamp so we can’t shrink smaller than placed slots (22 base)
                canvasW = Mth.clamp(newW, minW, 512);
                canvasH = Mth.clamp(newH, minH, 512);

                if (widthField != null) widthField.setValue(String.valueOf(canvasW));
                if (heightField != null) heightField.setValue(String.valueOf(canvasH));
                layoutCanvas();

                autoSave();
                return true;
            }
            if (draggingSlot != null) {
                int localX = (int) mouseX - canvasX - dragOffsetX;
                int localY = (int) mouseY - canvasY - dragOffsetY;
                localX = Mth.clamp(localX, 0, canvasW - SLOT_W);
                localY = Mth.clamp(localY, 0, canvasH - SLOT_H);

                int snappedX = snapFloor(localX, SLOT_GRID_STEP);
                int snappedY = snapFloor(localY, SLOT_GRID_STEP);

                int[] p = resolveBlockedDrag(snappedX, snappedY, draggingSlot, (int) dx, (int) dy);
                draggingSlot.x = p[0];
                draggingSlot.y = p[1];

                autoSave();
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    /**
     * Clean and predictable dragging:
     * - Move along intended axis until blocked; stop at collision; slide along free axis
     * - Enforces 2px gap to other slots and canvas edges
     */
    private int[] resolveBlockedDrag(int targetX, int targetY, MenuCreatorSlotDefinition moving, int dx, int dy) {
        int minX = MIN_GAP;
        int minY = MIN_GAP;
        int maxX = canvasW - SLOT_W - MIN_GAP;
        int maxY = canvasH - SLOT_H - MIN_GAP;

        int curX = moving.x;
        int curY = moving.y;
        int newX = Mth.clamp(targetX, minX, maxX);
        int newY = Mth.clamp(targetY, minY, maxY);

        // Horizontal first
        if (newX != curX) {
            int dir = Integer.signum(newX - curX);
            int tryX = curX;
            while (tryX != newX) {
                tryX += dir * SLOT_GRID_STEP;
                if (overlapsAny(tryX, curY, moving) || tryX < minX || tryX > maxX) {
                    tryX -= dir * SLOT_GRID_STEP; // back off to last free
                    break;
                }
            }
            newX = tryX;
        }

        // Then vertical
        if (newY != curY) {
            int dir = Integer.signum(newY - curY);
            int tryY = curY;
            while (tryY != newY) {
                tryY += dir * SLOT_GRID_STEP;
                if (overlapsAny(newX, tryY, moving) || tryY < minY || tryY > maxY) {
                    tryY -= dir * SLOT_GRID_STEP;
                    break;
                }
            }
            newY = tryY;
        }

        newX = snapFloor(Mth.clamp(newX, minX, maxX), SLOT_GRID_STEP);
        newY = snapFloor(Mth.clamp(newY, minY, maxY), SLOT_GRID_STEP);
        return new int[]{newX, newY};
    }

    /**
     * Checks overlap with any slot including the 2px gap (corner forgiveness = 1px).
     */
    private boolean overlapsAny(int x, int y, MenuCreatorSlotDefinition moving) {
        final int CORNER_TOLERANCE = 1;
        for (SlotView view : slotViews) {
            if (view.data == moving) continue;

            int ax1 = x, ay1 = y, ax2 = x + SLOT_W, ay2 = y + SLOT_H;
            int bx1 = view.data.x - MIN_GAP, by1 = view.data.y - MIN_GAP;
            int bx2 = view.data.x + SLOT_W + MIN_GAP, by2 = view.data.y + SLOT_H + MIN_GAP;

            boolean overlapX = ax1 < bx2 && ax2 > bx1;
            boolean overlapY = ay1 < by2 && ay2 > by1;
            if (overlapX && overlapY) {
                int dx = Math.min(Math.abs(ax2 - bx1), Math.abs(ax1 - bx2));
                int dy = Math.min(Math.abs(ay2 - by1), Math.abs(ay1 - by2));
                if (dx < CORNER_TOLERANCE && dy < CORNER_TOLERANCE) continue; // tiny diagonal touch forgiveness
                return true;
            }
        }
        return false;
    }

    private static int snapFloor(int v, int step) {
        if (step <= 1) return v;
        int q = v / step;
        if (v < 0 && v % step != 0) q -= 1;
        return q * step;
    }

    private boolean insideCanvas(double mx, double my) {
        return mx >= canvasX && mx <= canvasX + canvasW && my >= canvasY && my <= canvasY + canvasH;
    }

    private boolean overHandle(double mx, double my) {
        int size = HANDLE_SIZE - 2;
        int cx = canvasX + canvasW - size;
        int cy = canvasY + canvasH - size;
        return mx >= cx && mx <= cx + size && my >= cy && my <= cy + size;
    }

    // --- SlotView (draws at exact, non-smoothed positions) ---
    private static class SlotView implements GuiEventListener {
        static final int SLOT_W = 18, SLOT_H = 18;
        final MenuCreatorScreen parent;
        final MenuCreatorSlotDefinition data;

        SlotView(MenuCreatorScreen parent, MenuCreatorSlotDefinition data) {
            this.parent = parent;
            this.data = data;
        }

        void render(GuiGraphics g, int mouseX, int mouseY) {
            int ax = parent.canvasX + data.x;
            int ay = parent.canvasY + data.y;

            // Slot body + border
            g.fill(ax, ay, ax + SLOT_W, ay + SLOT_H, 0xFF2A2A2A);
            g.hLine(ax, ax + SLOT_W, ay, 0xFF888888);
            g.hLine(ax, ax + SLOT_W, ay + SLOT_H, 0xFF000000);
            g.vLine(ax, ay, ay + SLOT_H, 0xFF888888);
            g.vLine(ax + SLOT_W, ay, ay + SLOT_H, 0xFF000000);

            g.drawString(parent.font, data.id != null ? data.id : "(slot)", ax + 2, ay + 5, 0xFFDDDDDD, false);
        }

        boolean over(double mx, double my) {
            int ax = parent.canvasX + data.x;
            int ay = parent.canvasY + data.y;
            return mx >= ax && mx <= ax + SLOT_W && my >= ay && my <= ay + SLOT_H;
        }

        @Override public boolean isMouseOver(double mx, double my) { return over(mx, my); }
        @Override public void setFocused(boolean focused) {}
        @Override public boolean isFocused() { return false; }
    }

    private void addTestSlot() {
        int idx = def.slots.size() + 1;
        int px = snapFloor(8 + (idx * 12) % Math.max(8, canvasW - 24), SLOT_GRID_STEP);
        int py = snapFloor(8 + ((idx * 18) % Math.max(8, canvasH - 24)), SLOT_GRID_STEP);
        if (overlapsAny(px, py, null)) return;
        MenuCreatorSlotDefinition s = new MenuCreatorSlotDefinition("slot_" + idx, px, py);
        def.slots.add(s);
        slotViews.add(new SlotView(this, s));
        autoSave();
    }

    private void promptForMenuName() {
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            mc.player.sendSystemMessage(Component.literal(
                    "§7Enter a name for your menu in the text box (default: custom_menu)."));
        });
        if (menuNameField != null) menuNameField.setFocused(true);
    }

    @Override
    public void onClose() {
        saveMenuState();
        super.onClose();
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.grabMouse();
        mc.setScreen(null);
    }

    private static final class GLFW {
        static final int GLFW_KEY_TAB = 258;
        static final int GLFW_KEY_ESCAPE = 256;
        static final int GLFW_KEY_A = 65;
        static final int GLFW_KEY_ENTER = 257;
        static final int GLFW_KEY_KP_ENTER = 335;
    }
}