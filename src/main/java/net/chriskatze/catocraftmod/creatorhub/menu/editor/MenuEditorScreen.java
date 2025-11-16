package net.chriskatze.catocraftmod.creatorhub.menu.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.chriskatze.catocraftmod.creator.menu.MenuLayoutSerializer;
import net.chriskatze.catocraftmod.creator.menu.MenuSlotDefinition;
import net.chriskatze.catocraftmod.creatorhub.core.BaseCreatorScreen;
import net.chriskatze.catocraftmod.creatorhub.core.ItemTypeManagerScreen;
import net.chriskatze.catocraftmod.creatorhub.menu.CreatorMenuHubScreen;
import net.chriskatze.catocraftmod.creatorhub.menu.MenuListScreen;
import net.chriskatze.catocraftmod.menu.layout.SlotType;
import net.chriskatze.catocraftmod.menu.visual.GuiTextureHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.AbstractSliderButton;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu Editor:
 * - Grid: 2px minors, 40px majors (1-18-2-18-1)
 * - Slot size: 18x18; 2px min gap to slots & canvas border
 * - Dragging: collision-aware sliding (edge-slide)
 * - Ghost Mode: click to pick up; follows cursor; green if placeable, red if overlapping;
 *               left-click places (nearest free if needed), right-click cancels
 * - Resize handle: gold when near, orange while dragging
 * - Sidebar (TAB) for name + W/H + Feature list (scrollable)
 * - Right-click a slot: open right info panel (Type/Tag + Delete) — no rename here
 * - Autosave (debounced) to the selected layout JSON
 * - Back returns to the same MenuListScreen (no double-back)
 * - Shift+Click multi-select, Ctrl+Click duplicate → ghost
 * - Grid snap guide lines while dragging/ghosting
 * - Toasts: only on Save and Delete
 */

// ---------------------------------------------------------------------
// 🧩 Popup Overlay System (Type / Group selectors)
// ---------------------------------------------------------------------
/**
 * Unified popup overlay layer for the right-side slot settings panel.
 *
 * 🩵 Behaviour Overview:
 *  - Two popups exist: Type and Group (exclusive toggles).
 *  - The right panel's "Type" and "Group" buttons toggle their respective popups.
 *  - Clicking an option immediately applies it, closes the popup, and autosaves.
 *  - Clicking outside the popup also closes it.
 *
 * 🧠 Rendering & Depth:
 *  - Popups draw *after* all panels and dropdowns, using `g.flush()` + `pose().translate(0,0,500)`
 *    to force a new depth layer (same principle as vanilla tooltips).
 *  - This ensures the popup background is truly top-layer and blocks any text or blending behind it,
 *    regardless of Minecraft’s batching order.
 *  - The background uses a fully opaque double-fill (black + tinted overlay) to completely mask
 *    any underlying UI elements.
 *
 * 🖱️ Click Handling:
 *  - Visual drawing and click logic are separated:
 *      → `drawTypePopup()` / `drawGroupPopup()` only render visuals.
 *      → Actual click resolution and option selection are handled in `mouseClicked()`.
 *  - Popup rectangles are synchronized using `getPopupRect()`,
 *    keeping drawing, hit detection, and click blocking perfectly aligned.
 *
 * ✅ Key Notes:
 *  - This overlay system is self-contained — no dependency on legacy `drawPopupList()`.
 *  - Fully resistant to text-bleed and depth-order artifacts.
 *  - Safe for expansion (future popups like “Slot Filters” or “Tags” can reuse the same base).
 *
 * 🪄 Visual Style:
 *  - PANEL_DARKER + PANEL_LIGHTER for item rows
 *  - ACCENT_BLUE outline when hovered
 *  - TEXT_MAIN labels centered per row
 *
 * 🔧 Future Enhancements (optional):
 *  - Add scroll support if options > 10
 *  - Add dimmed panel overlay (0x88000000) for a modal feel (currently disabled)
 */

public class MenuEditorScreen extends BaseCreatorScreen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Screen parent;  // MenuListScreen or Hub
    private final File layoutFile;
    private MenuLayout layout;

    // ────────────────────────────────────────────────
    // 🎨 Creator Hub UI Theme Constants (shared palette)
    // ────────────────────────────────────────────────
    private static final int PANEL_BG      = 0xFF1C1E25;  // deep blue-gray
    private static final int PANEL_BORDER  = 0xFF3C4450;  // soft steel-blue outline
    private static final int TEXT_MAIN     = 0xFFE0F0FF;  // icy white-blue
    private static final int TEXT_SUB      = 0xFFA0B0C0;  // soft muted blue-gray
    private static final int ACCENT_BLUE   = 0xFF55AAFF;  // hub accent (hover/active)
    private static final int ACCENT_HOVER  = 0xFF3399FF;  // hover highlight
    private static final int PANEL_DARKER  = 0xFF252A33;  // inner darker shade
    private static final int PANEL_LIGHTER = 0xFF2C3C55;  // hover background

    // Canvas (work area you resize)
    private int canvasX, canvasY;
    private int canvasW, canvasH;

    // Right-panel layout cache
    private int cachedTagSectionBottom = 0;

    // Background & slot override editing
    private EditBox bgTextureBox;
    private EditBox slotTextureBox;
    private Button slotTypeSelectBtn;
    private SlotType selectedTextureType = SlotType.INVENTORY;

    // Resize handle
    private static final int HANDLE_SIZE = 8;
    private boolean resizing = false;
    private boolean wasResized = false;

    // Resize anchor correction
    private int resizeGrabOffsetX = 0;
    private int resizeGrabOffsetY = 0;

    // Dragging (hold-drag)
    private MenuSlotDefinition draggingSlot = null;
    private int dragOffsetX, dragOffsetY;

    // Pending click → used to distinguish click vs drag
    private MenuSlotDefinition pendingClickSlot = null;
    private int mouseDownX, mouseDownY;
    private boolean mouseDown = false;
    private static final int DRAG_THRESHOLD = 3; // px

    // Ghost Mode
    private boolean ghostMode = false;
    private MenuSlotDefinition ghostSlot = null;   // the same object as in layout list
    private int ghostOrigX = 0, ghostOrigY = 0;    // position to revert to on cancel
    private int lastMouseX = 0, lastMouseY = 0;    // tracked from render

    // Grid / slots
    private static final int CANVAS_GRID_STEP = 2;
    private static final int SLOT_GRID_STEP   = 2;
    private static final int OUTER_GAP = 1;   // distance from canvas edge
    private static final int INNER_GAP = 2;   // spacing between slots
    private static final int SLOT_W = 18, SLOT_H = 18;

    // ─────────────────── Toolbox Panel ───────────────────
    private static final int TOOLBOX_WIDTH = 80;
    private String draggingTool = null;        // "slot" or "text"
    private boolean hoveringToolSlot = false;
    private boolean hoveringToolText = false;

    // Cached rects for toolbox buttons (so clicks match draws exactly)
    private int itemTypesBtnX=-1, itemTypesBtnY=-1, itemTypesBtnW=0, itemTypesBtnH=0;
    private int bgBtnX=-1, bgBtnY=-1, bgBtnW=0, bgBtnH=0;

    // Editor-only background transparency
    private float backgroundAlpha = 0.45f; // 45% default
    private AbstractSliderButton backgroundAlphaSlider;

    // Click edge detection for dropdowns
    private boolean wasLeftDown = false;

    // Right Info Panel (slot settings)
    private boolean showRightPanel = false;
    private MenuSlotDefinition contextSlot = null; // highlighted by right-click

    // Selection
    private final List<MenuSlotDefinition> selectedSlots = new ArrayList<>();

    private Button slotTagDropdownBtn;
    private boolean tagDropdownOpen = false;
    private int tagDropdownX, tagDropdownY;
    private static final List<String> TAG_OPTIONS = List.of(
            "catocraftmod:earrings",
            "catocraftmod:rings",
            "catocraftmod:necklaces",
            "catocraftmod:soul_stones",
            "catocraftmod:revelation_items",
            "catocraftmod:reinforcement_items",
            "catocraftmod:gathering_items",
            "catocraftmod:prosperity_tools",
            "catocraftmod:prosperity_swords",
            "catocraftmod:attraction_tools",
            "catocraftmod:attraction_swords"
    );

    private Button slotTypeDropdownBtn;
    private boolean typeDropdownOpen = false;
    private int typeDropdownX, typeDropdownY;
    private static final List<String> TYPE_OPTIONS = List.of(
            "slot",
            "result_slot",
            "upgrade_slot",
            "pet_slot",
            "accessory_slot",
            "armor_slot",
            "trinket_slot",
            "rune_slot",
            "custom_slot",
            "magic_slot",
            "craft_slot",
            "output_slot"
    );

    // Popup for Type selection
    private boolean typePopupOpen = false;
    private boolean groupPopupOpen = false;

    private static class TagChip {
        final String tag;
        final int x, y, w, h;

        TagChip(String tag, int x, int y, int w, int h) {
            this.tag = tag;
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
    private final List<TagChip> tagChipBounds = new ArrayList<>();

    private static final List<String> GROUP_OPTIONS = new ArrayList<>(); // filled dynamically

    // Tag editor popup
    private boolean tagPopupOpen = false;
    private EditBox tagInputBox;
    private static final int TAG_CHIP_H = 14;

    // Texture selector popups
    private boolean slotTexturePopupOpen = false;
    private boolean backgroundTexturePopupOpen = false;

    private static final List<ResourceLocation> SLOT_TEXTURES = new ArrayList<>();
    private static final List<ResourceLocation> BACKGROUND_TEXTURES = new ArrayList<>();

    // Autosave debounce
    private long lastSaveTime = 0L;
    private static final long SAVE_INTERVAL_MS = 800;

    public MenuEditorScreen(Screen parent, File layoutFile) {
        super(Component.literal("Editing: " + layoutFile.getName()));
        this.parent = parent;
        this.layoutFile = layoutFile;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft.getInstance().mouseHandler.releaseMouse();

        // ─────────────────── Load or create layout ───────────────────
        boolean isNewFile = !layoutFile.exists();
        this.layout = MenuLayoutSerializer.load(layoutFile);

        if (this.layout == null) {
            CatocraftMod.LOGGER.warn("[MenuEditor] Failed to load {}, creating default.", layoutFile.getName());
            this.layout = new MenuLayout(stripJson(layoutFile.getName()), 176, 166);
            isNewFile = true; // ensure flag covers this too
        }

        // Ensure every slot has a valid type fallback
        for (MenuSlotDefinition s : layout.getSlots()) {
            String t = getSlotTypeReflect(s);
            if (t == null || t.isBlank()) {
                setSlotTypeReflect(s, "inventory_slot");
            }
        }

        // Canvas from layout (pixels in this editor)
        this.canvasW = Math.max(64, layout.getWidth());
        this.canvasH = Math.max(64, layout.getHeight());
        layoutCanvas();

        // ─────────────────── Default slot for new layouts ───────────────────
        MenuSlotDefinition defaultSlot = null;
        if (layout.getSlots().isEmpty()) {
            defaultSlot = new MenuSlotDefinition("slot_1", 8, 8, 18, "", "", false);
            setSlotTypeReflect(defaultSlot, "inventory_slot"); // ✅ explicitly set field using reflection
            layout.getSlots().add(defaultSlot);
            saveLayoutToFile();

            if (isNewFile) {
                this.contextSlot = defaultSlot;
                this.showRightPanel = true;
                // ❌ don’t call syncRightPanelFields yet — delay until widgets exist
            }
        }

        // ─────────────────── Bottom Buttons ───────────────────
        this.addRenderableWidget(Button.builder(
                Component.literal("← Back"),
                b -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (parent instanceof MenuListScreen list) {
                        mc.setScreen(list);
                        list.onReopened();
                    } else {
                        mc.setScreen(new CreatorMenuHubScreen(mc.screen));
                    }
                }
        ).pos(10, this.height - 28).size(80, 20).build());

        // ─────────────────── Dropdown Buttons (Type / Tag) ───────────────────
        slotTypeDropdownBtn = Button.builder(Component.literal("Select Type"), b -> {
            typeDropdownOpen = !typeDropdownOpen;
        }).bounds(-1000, -1000, 120, 14).build();
        slotTypeDropdownBtn.visible = false;
        addRenderableWidget(slotTypeDropdownBtn);

        slotTagDropdownBtn = Button.builder(Component.literal("Select Tag"), b -> {
            tagDropdownOpen = !tagDropdownOpen;
        }).bounds(-1000, -1000, 120, 14).build();
        slotTagDropdownBtn.visible = false;
        addRenderableWidget(slotTagDropdownBtn);

        // ─────────────────── Right Panel Fields ───────────────────
        bgTextureBox = new EditBox(this.font, -1000, -1000, TOOLBOX_WIDTH - 12, 14, Component.literal("Background Texture"));
        bgTextureBox.setMaxLength(256);
        bgTextureBox.setResponder(path -> {
            layout.setBackgroundTexture(path.isBlank() ? null : path);
            autoSave();
        });
        addRenderableWidget(bgTextureBox);
        bgTextureBox.visible = false;

        // ─────────────────── Tag Input Box (popup) ───────────────────
        tagInputBox = new EditBox(
                this.font,
                -1000, -1000,                    // position is dynamic in popup
                TOOLBOX_WIDTH - 12,
                14,
                Component.literal("Tag")
        );
        tagInputBox.setMaxLength(64);
        tagInputBox.setVisible(false);
        this.addRenderableWidget(tagInputBox);

        // ─────────────────── Background Transparency Slider ───────────────────
        int boxW = TOOLBOX_WIDTH;
        int entryW = boxW - 12;
        int entryX = 6;
        int sliderH = 20;

        backgroundAlphaSlider = new AlphaSlider(entryX, -1000, entryW, sliderH, backgroundAlpha);
        backgroundAlphaSlider.visible = true;
        addRenderableWidget(backgroundAlphaSlider);

        // ─────────────────── Slot Type Selector ───────────────────
        slotTypeSelectBtn = Button.builder(Component.literal(selectedTextureType.name()), b -> {
            int next = (selectedTextureType.ordinal() + 1) % SlotType.values().length;
            selectedTextureType = SlotType.values()[next];
            slotTypeSelectBtn.setMessage(Component.literal(selectedTextureType.name()));
        }).bounds(-1000, -1000, 60, 14).build();
        addRenderableWidget(slotTypeSelectBtn);
        slotTypeSelectBtn.visible = false;

        // ─────────────────── Slot Texture Path Box ───────────────────
        slotTextureBox = new EditBox(this.font, -1000, -1000, TOOLBOX_WIDTH - 72, 14, Component.literal("Slot Texture Path"));
        slotTextureBox.setMaxLength(256);
        slotTextureBox.setResponder(path -> {
            layout.addSlotTexture(selectedTextureType.name(), path);
            autoSave();
        });
        addRenderableWidget(slotTextureBox);
        slotTextureBox.visible = false;

        // ─────────────────── Dynamic Texture Discovery ───────────────────
        SLOT_TEXTURES.clear();
        BACKGROUND_TEXTURES.clear();

        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();

            var slotRes = resourceManager.listResources("textures/gui/slots", path -> path.getPath().endsWith(".png"));
            for (ResourceLocation loc : slotRes.keySet()) {
                String path = loc.getPath();
                if (path.startsWith("textures/")) path = path.substring("textures/".length());
                if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);

                ResourceLocation normalized = GuiTextureHelper.toGuiTexture(loc.getNamespace() + ":" + path);
                if (normalized != null) SLOT_TEXTURES.add(normalized);
            }

            var bgRes = resourceManager.listResources("textures/gui/backgrounds", path -> path.getPath().endsWith(".png"));
            for (ResourceLocation loc : bgRes.keySet()) {
                String path = loc.getPath();
                if (path.startsWith("textures/")) path = path.substring("textures/".length());
                if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);

                ResourceLocation normalized = GuiTextureHelper.toGuiTexture(loc.getNamespace() + ":" + path);
                if (normalized != null) BACKGROUND_TEXTURES.add(normalized);
            }

            CatocraftMod.LOGGER.info("[MenuEditor] Discovered {} slot textures and {} background textures.",
                    SLOT_TEXTURES.size(), BACKGROUND_TEXTURES.size());

        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[MenuEditor] Failed to dynamically discover textures", e);
        }

        // ─────────────────── Panel State ───────────────────
        showRightPanel = (isNewFile && defaultSlot != null);
        typePopupOpen = false;
        groupPopupOpen = false;
        tagDropdownOpen = false;

        // ✅ Finally: now safe to sync right panel after widgets exist
        if (isNewFile && defaultSlot != null) {
            syncRightPanelFields(defaultSlot);
        }
    }

    private static String stripJson(String name) {
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    // ─────────────────── Save / Auto ───────────────────

    private void autoSave() {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime > SAVE_INTERVAL_MS) {
            saveLayoutToFile();
            lastSaveTime = now;
        }
    }

    private void saveLayoutToFile() {
        try {
            layout.setName(stripJson(layoutFile.getName()));
            layout.setWidth(canvasW);
            layout.setHeight(canvasH);
            MenuLayoutSerializer.save(layout, layoutFile);
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[MenuEditor] Save failed: {}", e.toString());
        }
    }

    // ─────────────────── Keyboard ───────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ───────────── Close dropdowns first ─────────────
        if (tagDropdownOpen || typeDropdownOpen) {
            tagDropdownOpen = false;
            typeDropdownOpen = false;
            return true;
        }

        // ───────────── ESC handling ─────────────
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (ghostMode) { // cancel ghost placement
                cancelGhost();
                return true;
            }
            if (showRightPanel) { // close right panel
                showRightPanel = false;
                typePopupOpen = false;
                contextSlot = null;
                syncRightPanelFields(null);
                tagDropdownOpen = false;
                typeDropdownOpen = false;
                return true;
            }
            saveLayoutToFile();
            onClose();
            return true;
        }

        // ───────────── Delete selected slots ─────────────
        if (keyCode == GLFW.GLFW_KEY_DELETE && !selectedSlots.isEmpty()) {
            for (MenuSlotDefinition s : new ArrayList<>(selectedSlots)) {
                layout.getSlots().remove(s);
            }
            showError("Deleted " + selectedSlots.size() + " slot(s)");
            selectedSlots.clear();
            autoSave();
            return true;
        }

        // ───────────── Quick-save (Ctrl + S) ─────────────
        if (keyCode == GLFW.GLFW_KEY_S && Screen.hasControlDown()) {
            saveLayoutToFile();
            showSuccess("Layout saved.");
            return true;
        }

        // ───────────── Undo / Redo placeholders ─────────────
        if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasControlDown()) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_Y && Screen.hasControlDown()) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }

    private void layoutCanvas() {
        this.canvasX = snapFloor((this.width - canvasW) / 2, CANVAS_GRID_STEP);
        this.canvasY = snapFloor((this.height - canvasH) / 2, CANVAS_GRID_STEP);
    }

    private void drawCanvas(GuiGraphics g) {
        int left = canvasX;
        int top = canvasY;
        int w = canvasW;
        int h = canvasH;

        // ────────────────────────────────────────────────
        // Inner shade & border (kept from original)
        // ────────────────────────────────────────────────
        g.fill(left + 1, top + 1, left + w - 1, top + h - 1, 0xFF181A20);
        g.renderOutline(left, top, w, h, 0xFF3C4450);

        // ────────────────────────────────────────────────
        // 🧭 Canvas size label above top edge
        // ────────────────────────────────────────────────
        String sizeLabel = w + " × " + h;
        int textWidth = this.font.width(sizeLabel);
        int labelX = left + (w / 2) - (textWidth / 2);
        int labelY = top - this.font.lineHeight - 2;
        g.drawString(this.font, sizeLabel, labelX, labelY, 0xFFA0B0C0, false);
    }

    private void drawGrid(GuiGraphics g) {
        final int majorStep = 40; // covers 1 + 18 + 2 + 18 + 1 = 40 px
        final int minorStep = 2;

        int minorColor = 0x1180A0B0;
        int majorColor = 0x33A0B0C0;

        int left   = canvasX;
        int top    = canvasY;
        int right  = canvasX + canvasW - 1;
        int bottom = canvasY + canvasH - 1;

        // ── Minor grid (2 px)
        for (int x = left; x <= right; x += minorStep)
            g.vLine(x, top, bottom, minorColor);
        for (int y = top; y <= bottom; y += minorStep)
            g.hLine(left, right, y, minorColor);

        // ── Major grid (every 40 px, starting at canvas edge)
        for (int x = left; x <= right; x += majorStep)
            g.vLine(x, top, bottom, majorColor);
        for (int y = top; y <= bottom; y += majorStep)
            g.hLine(left, right, y, majorColor);
    }

    private void drawGhostSlot(GuiGraphics g, MenuSlotDefinition s, int mouseX, int mouseY) {
        int[] pos = ghostSnapped(mouseX, mouseY);
        int gx = pos[0], gy = pos[1];
        boolean blocked = overlapsAny(gx, gy, s);
        int fill = blocked ? 0x66FF2222 : 0x6622FF22;
        int border = blocked ? 0xFFFF4444 : 0xFF44FF44;

        g.fill(canvasX + gx, canvasY + gy, canvasX + gx + SLOT_W, canvasY + gy + SLOT_H, fill);
        g.renderOutline(canvasX + gx, canvasY + gy, SLOT_W, SLOT_H, border);
    }

    private void drawGuidesIfAny(GuiGraphics g) {
        // optional: snap/guide visuals can stay empty for now
    }

    /** Draws the resize handle in Creator Hub theme */
    private void drawHandle(GuiGraphics g, int mouseX, int mouseY) {
        int size = HANDLE_SIZE;
        int hx = canvasX + canvasW - size;
        int hy = canvasY + canvasH - size;

        boolean hover = mouseX >= hx && mouseX <= hx + size && mouseY >= hy && mouseY <= hy + size;
        int color = resizing ? ACCENT_BLUE : (hover ? ACCENT_HOVER : PANEL_LIGHTER);

        g.fill(hx, hy, hx + size, hy + size, color);
        g.renderOutline(hx, hy, size, size, PANEL_BORDER);
    }

    // ─────────────────── Render ───────────────────
    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float partial) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // ─────── Frame-based click tracking ───────
        boolean leftDownNow = Minecraft.getInstance().mouseHandler.isLeftPressed();
        boolean clickedThisFrame = leftDownNow && !wasLeftDown;

        // ───────────────────── Left Toolbox ─────────────────────
        drawToolbox(g, mouseX, mouseY);

        // ───────────────────── Canvas & Grid ─────────────────────
        drawCanvas(g);
        drawGrid(g);

        // ───────────────────── Background Overlay (semi-transparent for editor) ─────────────────────
        if (layout.getBackgroundTexture() != null && !layout.getBackgroundTexture().isBlank()) {
            try {
                ResourceLocation tex = GuiTextureHelper.toGuiTexture(layout.getBackgroundTexture());
                if (tex != null) {
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

                    // 👇 apply editor-only transparency
                    g.setColor(1f, 1f, 1f, Mth.clamp(backgroundAlpha, 0f, 1f));
                    g.blit(tex, canvasX, canvasY, 0, 0, canvasW, canvasH, canvasW, canvasH);
                    g.setColor(1f, 1f, 1f, 1f);

                    com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                }
            } catch (Exception e) {
                // ignore bad path
            }
        }

        // ───────────────────── Existing Slots ─────────────────────
        for (MenuSlotDefinition s : layout.getSlots()) {
            if (ghostMode && s == ghostSlot) continue;

            int ax = canvasX + s.getX();
            int ay = canvasY + s.getY();

            // Determine slot type
            SlotType type = SlotType.fromString(s.getType());
            if (type == null) type = SlotType.INVENTORY;

            // ───────────── Resolve texture (with per-slot override) ─────────────
            ResourceLocation tex = null;
            try {
                // 1️⃣ Per-slot override
                if (s.getTextureOverride() != null && !s.getTextureOverride().isBlank()) {
                    tex = ResourceLocation.tryParse(s.getTextureOverride());
                }

                // 2️⃣ Layout-wide override
                if (tex == null) {
                    String overridePath = layout.getSlotTextures().get(type.name());
                    if (overridePath != null && !overridePath.isBlank()) {
                        tex = ResourceLocation.tryParse(overridePath);
                    }
                }

                // 3️⃣ Registry fallback (normalized via GuiTextureHelper)
                if (tex != null) {
                    ResourceLocation resolved = GuiTextureHelper.toGuiTexture(tex.toString());
                    if (resolved != null) {
                        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, resolved);
                        g.blit(resolved, ax + 1, ay + 1, 0, 0, SLOT_W - 2, SLOT_H - 2, SLOT_W - 2, SLOT_H - 2);
                    } else {
                        drawMissingTexture(g, ax, ay);
                    }
                } else {
                    drawMissingTexture(g, ax, ay);
                }

            } catch (Exception e) {
                drawMissingTexture(g, ax, ay);
            }

            // ───────────── Outline & context highlight ─────────────
            g.hLine(ax, ax + SLOT_W - 1, ay, 0xFF3C4450);
            g.hLine(ax, ax + SLOT_W - 1, ay + SLOT_H - 1, 0xFF3C4450);
            g.vLine(ax, ay, ay + SLOT_H - 1, 0xFF3C4450);
            g.vLine(ax + SLOT_W - 1, ay, ay + SLOT_H - 1, 0xFF3C4450);

            if (s == contextSlot) {
                int border = ACCENT_BLUE;
                g.hLine(ax, ax + SLOT_W - 1, ay, border);
                g.hLine(ax, ax + SLOT_W - 1, ay + SLOT_H - 1, border);
                g.vLine(ax, ay, ay + SLOT_H - 1, border);
                g.vLine(ax + SLOT_W - 1, ay, ay + SLOT_H - 1, border);
            }
        }

        // ───────────────────── Ghosts / Tools ─────────────────────
        if (ghostMode && ghostSlot != null) {
            drawGhostSlot(g, ghostSlot, mouseX, mouseY);
        } else if (draggingTool != null) {
            drawGhostTool(g, mouseX, mouseY, draggingTool);
        }

        drawGuidesIfAny(g);

        // ───────────────────── Panels ─────────────────────
        drawRightPanel(g, 160);

        // ───────────────────── Popups (draw once per frame) ─────────────────────
        if (backgroundTexturePopupOpen) {
            drawTexturePopup(g, clickedThisFrame, BACKGROUND_TEXTURES, "Select Background", selectedTex -> {
                layout.setBackgroundTexture(selectedTex.toString());
                backgroundTexturePopupOpen = false;
                showSuccess("Background set!");
                autoSave();
            });
        }

        if (slotTexturePopupOpen && contextSlot != null) {
            drawTexturePopup(g, clickedThisFrame, SLOT_TEXTURES, "Select Slot Texture", selectedTex -> {
                contextSlot.setTextureOverride(selectedTex.toString());
                slotTexturePopupOpen = false;
                showSuccess("Slot texture changed!");
                autoSave();
            });
        }

        // ───────────────────── Dropdowns & Overlays ─────────────────────
        drawDropdowns(g, lastMouseX, lastMouseY);
        drawTypePopup(g, clickedThisFrame);
        drawGroupPopup(g, clickedThisFrame);
        drawTagPopup(g, clickedThisFrame);

        // ───────────────────── Resize Handle ─────────────────────
        drawHandle(g, mouseX, mouseY);

        // ✅ Update click state at very end of frame
        wasLeftDown = leftDownNow;
    }

    /** Draws a simple purple/black checkerboard for missing textures. */
    private void drawMissingTexture(GuiGraphics g, int x, int y) {
        int size = 9;
        int color1 = 0xFF6B1FFF; // purple
        int color2 = 0xFF000000; // black

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                int cx = x + i * size;
                int cy = y + j * size;
                int color = ((i + j) % 2 == 0) ? color1 : color2;
                g.fill(cx, cy, cx + size, cy + size, color);
            }
        }
    }

    private class AlphaSlider extends AbstractSliderButton {
        AlphaSlider(int x, int y, int w, int h, double value) {
            super(x, y, w, h, Component.empty(), value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int pct = (int)Math.round(this.value * 100.0);
            this.setMessage(Component.literal("Opacity: " + pct + "%"));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            boolean hover = mouseX >= getX() && mouseX <= getX() + width &&
                    mouseY >= getY() && mouseY <= getY() + height;

            int bg = hover ? PANEL_LIGHTER : PANEL_DARKER;
            int border = hover ? ACCENT_BLUE : PANEL_BORDER;
            int textColor = TEXT_MAIN;

            // Fill + outline
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);
            g.renderOutline(getX(), getY(), width, height, border);

            // Track (thin groove)
            int trackY = getY() + height / 2 - 1;
            g.fill(getX() + 6, trackY, getX() + width - 6, trackY + 2, 0xFF14171C);

            // Thumb (smaller accent rectangle)
            int thumbWidth = 8;
            int thumbHeight = 4; // thinner
            int thumbX = (int)(getX() + 6 + (width - 12 - thumbWidth) * this.value);
            int thumbY = getY() + (height - thumbHeight) / 2;
            g.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, ACCENT_BLUE);

            // Perfectly centered smaller text
            String msg = getMessage().getString();
            var font = Minecraft.getInstance().font;
            float scale = 0.85f;

            // Scale-aware centering
            int textW = (int)(font.width(msg) * scale);
            int textH = (int)(font.lineHeight * scale);
            float centerX = getX() + (width - textW) / 2f;
            float centerY = getY() + (height - textH) / 2f;

            g.pose().pushPose();
            g.pose().translate(centerX, centerY, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, msg, 0, 0, textColor, false);
            g.pose().popPose();
        }

        @Override
        protected void applyValue() {
            backgroundAlpha = (float)this.value;
        }
    }

    /** Toolbox panel (left side, Creator Hub–themed) */
    private void drawToolbox(GuiGraphics g, int mouseX, int mouseY) {
        int boxW = TOOLBOX_WIDTH;
        int boxH = this.height;
        int baseX = 0;
        int baseY = 0;

        // Background panel
        g.fill(baseX, baseY, baseX + boxW, baseY + boxH, PANEL_BG);
        g.renderOutline(baseX, baseY, boxW, boxH, PANEL_BORDER);

        int y = 14;
        g.drawCenteredString(this.font, "Tools", baseX + boxW / 2, y, TEXT_MAIN);
        y += 14;

        int entryH = 20;
        int entryW = boxW - 12;
        int entryX = baseX + 6;

        // Slot tool
        hoveringToolSlot = mouseX >= entryX && mouseX <= entryX + entryW && mouseY >= y && mouseY <= y + entryH;
        int slotBg = hoveringToolSlot ? PANEL_LIGHTER : PANEL_DARKER;
        int slotOutline = hoveringToolSlot ? ACCENT_BLUE : PANEL_BORDER;
        g.fill(entryX, y, entryX + entryW, y + entryH, slotBg);
        g.renderOutline(entryX, y, entryW, entryH, slotOutline);
        g.drawCenteredString(this.font, "🟦 Slot", entryX + entryW / 2, y + 6, TEXT_MAIN);
        y += entryH + 6;

        // Text tool
        hoveringToolText = mouseX >= entryX && mouseX <= entryX + entryW && mouseY >= y && mouseY <= y + entryH;
        int textBg = hoveringToolText ? PANEL_LIGHTER : PANEL_DARKER;
        int textOutline = hoveringToolText ? ACCENT_BLUE : PANEL_BORDER;
        g.fill(entryX, y, entryX + entryW, y + entryH, textBg);
        g.renderOutline(entryX, y, entryW, entryH, textOutline);
        g.drawCenteredString(this.font, "🧾 Text", entryX + entryW / 2, y + 6, TEXT_MAIN);

        // ───────────── Background selector ─────────────
        y += entryH + 12;
        int bgW = entryW;
        int bgX = entryX;
        boolean hoverBg = mouseX >= bgX && mouseX <= bgX + bgW && mouseY >= y && mouseY <= y + entryH;
        int bgBg = hoverBg ? PANEL_LIGHTER : PANEL_DARKER;
        int bgOutline = hoverBg ? ACCENT_BLUE : PANEL_BORDER;

        g.fill(bgX, y, bgX + bgW, y + entryH, bgBg);
        g.renderOutline(bgX, y, bgW, entryH, bgOutline);
        g.drawCenteredString(this.font, "🖼 Background", bgX + bgW / 2, y + 6, TEXT_MAIN);

        // ✅ Cache Background button rect for precise click hitbox
        bgBtnX = bgX;
        bgBtnY = y;
        bgBtnW = bgW;
        bgBtnH = entryH;

        // ───────────── Background Transparency Slider ─────────────
        int sliderY = y + entryH + 6; // below the background button
        int sliderH = 20;

        backgroundAlphaSlider.setX(entryX);
        backgroundAlphaSlider.setY(sliderY);
        backgroundAlphaSlider.setWidth(entryW);
        backgroundAlphaSlider.visible = true;

        // move y below the slider
        y = sliderY + sliderH + 6;

        // ───────────── Item Types Button ─────────────
        boolean hoveringItemType = mouseX >= entryX && mouseX <= entryX + entryW &&
                mouseY >= y && mouseY <= y + entryH;
        int itemBg = hoveringItemType ? PANEL_LIGHTER : PANEL_DARKER;
        int itemOutline = hoveringItemType ? ACCENT_BLUE : PANEL_BORDER;

        g.fill(entryX, y, entryX + entryW, y + entryH, itemBg);
        g.renderOutline(entryX, y, entryW, entryH, itemOutline);
        g.drawCenteredString(this.font, "📘 Item Types", entryX + entryW / 2, y + 6, TEXT_MAIN);

        // ✅ Cache Item Types button rect for precise click hitbox
        itemTypesBtnX = entryX;
        itemTypesBtnY = y;
        itemTypesBtnW = entryW;
        itemTypesBtnH = entryH;
    }

    /** When dragging a new tool (Slot or Text) */
    private void drawGhostTool(GuiGraphics g, int mouseX, int mouseY, String toolType) {
        if (toolType.equals("slot")) {
            int[] pos = ghostSnapped(mouseX, mouseY);
            int gx = pos[0], gy = pos[1];
            int fill = 0x6622CC22;
            int border = 0xFF55FF55;
            g.fill(canvasX + gx, canvasY + gy, canvasX + gx + SLOT_W, canvasY + gy + SLOT_H, fill);
            g.hLine(canvasX + gx, canvasX + gx + SLOT_W, canvasY + gy, border);
            g.hLine(canvasX + gx, canvasX + gx + SLOT_W, canvasY + gy + SLOT_H, border);
            g.vLine(canvasX + gx, canvasY + gy, canvasY + gy + SLOT_H, border);
            g.vLine(canvasX + gx + SLOT_W, canvasY + gy, canvasY + gy + SLOT_H, border);
        } else if (toolType.equals("text")) {
            int w = 40, h = 14;
            int gx = mouseX - w / 2;
            int gy = mouseY - h / 2;
            g.fill(gx, gy, gx + w, gy + h, 0x88333333);
            g.renderOutline(gx, gy, w, h, 0xFF888888);
            g.drawCenteredString(this.font, "Text", gx + w / 2, gy + 3, 0xFFFFFFFF);
        }
    }

// ─────────────────── Right Panel ───────────────────
    /** Compact, readable right-side panel for slot settings */
    private void drawRightPanel(GuiGraphics g, int panelWidthRight) {
        panelWidthRight = TOOLBOX_WIDTH;

        if (!showRightPanel || contextSlot == null) {
            slotTypeDropdownBtn.visible = false;
            bgTextureBox.visible = false;
            slotTypeSelectBtn.visible = false;
            slotTextureBox.visible = false;
            return;
        }

        tagChipBounds.clear();

        int px = this.width - panelWidthRight;
        int py = 0;
        int pw = panelWidthRight;
        int ph = this.height;

        // Background
        g.fill(px, py, px + pw, py + ph, PANEL_BG);
        g.renderOutline(px, py, pw, ph, PANEL_BORDER);

        int y = py + 14;
        g.drawCenteredString(this.font, "Settings", px + pw / 2, y, TEXT_MAIN);
        y += 16;

        int btnW = pw - 12;
        int btnH = 18;
        int btnX = px + 6;

        double mx = lastMouseX, my = lastMouseY;
        List<ButtonLabel> labels = new ArrayList<>();

        // helper function
        java.util.function.BiConsumer<String, Integer> centeredLabel = (text, yy) ->
                g.drawCenteredString(this.font, text, px + pw / 2, yy, TEXT_MAIN);

        // Current values
        String curType = prettifyTypeName(getSlotTypeReflect(contextSlot));
        String curGroup = contextSlot.getGroup() == null ? "" : contextSlot.getGroup();

        // ───────────── Type ─────────────
        boolean hType = isHover(mx, my, btnX, y, btnW, btnH);
        g.fill(btnX, y, btnX + btnW, y + btnH, hType ? PANEL_LIGHTER : PANEL_DARKER);
        g.renderOutline(btnX, y, btnW, btnH, hType ? ACCENT_HOVER : PANEL_BORDER);
        labels.add(new ButtonLabel("Type", btnX + btnW / 2, y + 5));
        centeredLabel.accept(curType, y + btnH + 3);
        y += btnH + 14;

        g.hLine(px + 4, px + pw - 4, y + 1, PANEL_BORDER);

        // ───────────── Group ─────────────
        y += 6;
        boolean hGroup = isHover(mx, my, btnX, y, btnW, btnH);
        g.fill(btnX, y, btnX + btnW, y + btnH, hGroup ? PANEL_LIGHTER : PANEL_DARKER);
        g.renderOutline(btnX, y, btnW, btnH, hGroup ? ACCENT_HOVER : PANEL_BORDER);
        labels.add(new ButtonLabel("Group", btnX + btnW / 2, y + 5));
        centeredLabel.accept(curGroup, y + btnH + 3);
        y += btnH + 14;

        g.hLine(px + 4, px + pw - 4, y + 1, PANEL_BORDER);

        // ───────────── Tag (multi-tag chip system) ─────────────
        y += 6;

        boolean hTag = isHover(mx, my, btnX, y, btnW, btnH);
        g.fill(btnX, y, btnX + btnW, y + btnH, hTag ? PANEL_LIGHTER : PANEL_DARKER);
        g.renderOutline(btnX, y, btnW, btnH, hTag ? ACCENT_HOVER : PANEL_BORDER);
        labels.add(new ButtonLabel("Tag", btnX + btnW / 2, y + 5));

        int tagButtonY = y;
        y += btnH + 6;

        // Now draw the list of tags ("chips")
        List<String> tags = contextSlot.getTags();
        int chipX = btnX;
        int chipY = y;
        int chipH = 14;
        int maxChipW = btnW;

        for (String tag : tags) {
            String shortName = shortenTag(tag);   // 🔥 shorten here

            int chipW = this.font.width(shortName) + 12;

            // wrap to next line if needed
            if (chipX + chipW > btnX + maxChipW) {
                chipX = btnX;
                chipY += chipH + 4;
            }

            // chip background
            g.fill(chipX, chipY, chipX + chipW, chipY + chipH, PANEL_DARKER);
            g.renderOutline(chipX, chipY, chipW, chipH, PANEL_BORDER);

            // tag text
            g.drawString(this.font, shortName, chipX + 3, chipY + 3, TEXT_MAIN, false);

            // remove icon
            int xX = chipX + chipW - 8;
            int xY = chipY + 2;
            g.drawString(this.font, "✕", xX, xY, 0xFFDD5555, false);

            // store clickable chip bounds
            tagChipBounds.add(new TagChip(tag, chipX, chipY, chipW, chipH));

            chipX += chipW + 4;
        }

        y = chipY + chipH + 8;
        cachedTagSectionBottom = y;  // <-- NEW
        g.hLine(px + 4, px + pw - 4, y - 3, PANEL_BORDER);

        // ───────────── Slot Texture Override ─────────────
        y += 8;
        g.drawCenteredString(this.font, "Slot Texture", px + pw / 2, y, TEXT_MAIN);
        y += 12;

        boolean hoverSlotTex = isHover(mx, my, btnX, y, btnW, btnH);
        int slotTexBg = hoverSlotTex ? PANEL_LIGHTER : PANEL_DARKER;
        int slotTexOutline = hoverSlotTex ? ACCENT_BLUE : PANEL_BORDER;

        g.fill(btnX, y, btnX + btnW, y + btnH, slotTexBg);
        g.renderOutline(btnX, y, btnW, btnH, slotTexOutline);
        g.drawCenteredString(this.font, "🎨 Slot Texture", btnX + btnW / 2, y + 5, TEXT_MAIN);
        y += btnH + 14;

        g.hLine(px + 4, px + pw - 4, y - 3, PANEL_BORDER);

        // ────────────── Delete Button ──────────────
        int delY = getDeleteButtonY();
        boolean hoveringDel = isHover(mx, my, btnX, delY, btnW, btnH);
        int delBg = hoveringDel ? 0xFF5A1C1C : 0xFF3C2525;
        int delOutline = hoveringDel ? 0xFFFF5555 : PANEL_BORDER;

        g.fill(btnX, delY, btnX + btnW, delY + btnH, delBg);
        g.renderOutline(btnX, delY, btnW, btnH, delOutline);
        labels.add(new ButtonLabel("Delete", btnX + btnW / 2, delY + 6));

        for (ButtonLabel bl : labels)
            g.drawCenteredString(this.font, bl.text(), bl.x(), bl.y(), TEXT_MAIN);
    }

    private String shortenTag(String full) {
        int idx = full.indexOf(':');
        if (idx >= 0) return full.substring(idx + 1);
        return full;
    }

    /** Small helper record for deferred button text */
    private record ButtonLabel(String text, int x, int y) {}

    /** Utility: simple hover region check using GUI-space coordinates */
    private boolean isHover(double mx, double my, int x, int y, int w, int h) {
        // extend bottom edge by 1 pixel so click aligns with drawn rects
        return mx >= x && mx <= x + w && my >= y && my <= y + h + 1;
    }

    /** Utility: simple hover region check for right panel buttons */
    private boolean isMouseOverButton(int x, int y, int w, int h) {
        double mx = Minecraft.getInstance().mouseHandler.xpos() * (double) this.width / Minecraft.getInstance().getWindow().getScreenWidth();
        double my = Minecraft.getInstance().mouseHandler.ypos() * (double) this.height / Minecraft.getInstance().getWindow().getScreenHeight();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void syncRightPanelFields(MenuSlotDefinition s) {
        if (s == null) {
            slotTypeSelectBtn.visible = false;
            slotTextureBox.visible = false;
            return;
        }

        // Update right panel fields from slot + layout
        String curType = prettifyTypeName(getSlotTypeReflect(s));
        String curTag  = getSlotTagReflect(s);

        // Update text boxes + buttons
        slotTypeSelectBtn.setMessage(Component.literal(curType.isEmpty() ? "Inventory" : curType));
        slotTextureBox.setValue(layout.getSlotTextures().getOrDefault(selectedTextureType.name(), ""));
    }

// ---------------------------------------------------------------------
// Unified popup layout helper
// ---------------------------------------------------------------------
    /**
     * Returns popup coordinates and size info as [x, y, width].
     * Keeps drawing, click, and click-block logic perfectly in sync.
     */
    /** Popup rectangle helper — perfectly aligned with actual button positions. */
    private int[] getPopupRect(String popupType) {
        int pw = TOOLBOX_WIDTH;
        int px = this.width - pw;

        int btnH = 20;
        int gap = 6;
        int base = 14 + 14; // top padding + title height

        // Get current slot values to calculate spacing offsets
        String curType = contextSlot != null ? getSlotTypeReflect(contextSlot) : "";
        String curGroup = (contextSlot != null && contextSlot.getGroup() != null && !contextSlot.getGroup().isBlank())
                ? contextSlot.getGroup() : "";
        String curTag = contextSlot != null ? getSlotTagReflect(contextSlot) : "";

        // Prettify names (so same logic as drawRightPanel)
        curType = prettifyTypeName(curType);

        // Compute vertical positions matching drawRightPanel’s logic
        int yType = base;
        int yAfterType = yType + btnH + (curType.isBlank() ? gap : 16) + 3;   // +3 divider
        int yAfterGroup = yAfterType + btnH + (curGroup.isBlank() ? gap : 16) + 3; // +3 divider
        int yAfterTag = yAfterGroup + btnH + (curTag.isBlank() ? gap : 16) + 3;   // +3 divider

        int py = switch (popupType) {
            case "type" -> yType + btnH;
            case "group" -> yAfterType + btnH;
            case "tag" -> yAfterGroup + btnH;
            default -> base;
        };

        return new int[]{px, py + 2, pw}; // +2 slight visual gap for breathing room
    }

    /** Popup for type selection — purely visual (click handled in mouseClicked) */
    private void drawTypePopup(GuiGraphics g, boolean clickedThisFrame) {
        if (!typePopupOpen || contextSlot == null || !showRightPanel) return;

        int[] rect = getPopupRect("type");
        int px = rect[0], py = rect[1], pw = rect[2];

        // Collect both raw names (for logic) and pretty names (for display)
        List<String> typeNames = new ArrayList<>();
        for (SlotType type : SlotType.values()) {
            typeNames.add(prettifyTypeName(type.getDisplayName()));
        }

        drawPopupListVisual(g, typeNames, px, py, "Type");
    }

    /** Popup for group selection — clean list, no headers */
    private void drawGroupPopup(GuiGraphics g, boolean clickedThisFrame) {
        if (!groupPopupOpen || contextSlot == null || !showRightPanel) return;

        int[] rect = getPopupRect("group");
        int px = rect[0], py = rect[1], pw = rect[2];
        int optionH = 18;

        // Combine static + registry item types (no divider, no header)
        List<String> options = new ArrayList<>(GROUP_OPTIONS);
        options.addAll(net.chriskatze.catocraftmod.util.ItemTypeRegistry.getAllTypeNames());

        int totalH = options.size() * optionH + 4;

        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        // Background panel
        g.fill(px, py, px + pw, py + totalH, PANEL_DARKER);
        g.renderOutline(px, py, pw, totalH, PANEL_BORDER);

        int y = py + 2;

        for (String opt : options) {
            boolean hover = isHover(lastMouseX, lastMouseY, px, y, pw, optionH);
            int bg = hover ? PANEL_LIGHTER : PANEL_BG;
            int border = hover ? ACCENT_BLUE : PANEL_BORDER;

            g.fill(px + 2, y, px + pw - 2, y + optionH, bg);
            g.renderOutline(px + 2, y, pw - 4, optionH, border);
            g.drawCenteredString(this.font, opt, px + pw / 2, y + 5, TEXT_MAIN);

            y += optionH;
        }

        g.pose().popPose();
    }

    /** Unified texture selection popup (backgrounds & slots). */
    private void drawTexturePopup(GuiGraphics g, boolean clickedThisFrame,
                                  List<ResourceLocation> textures,
                                  String title,
                                  java.util.function.Consumer<ResourceLocation> onSelect) {
        if (textures == null || textures.isEmpty()) return;

        int pw = 200;
        int ph = 200;
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;

        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        // Panel background
        g.fill(px, py, px + pw, py + ph, 0xFF101214);
        g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFF1C1E25);
        g.renderOutline(px, py, pw, ph, 0xFF3C4450);
        g.drawCenteredString(this.font, title, px + pw / 2, py + 6, 0xFFE0F0FF);

        // grid layout
        int cols = 4;
        int thumbSize = 32;
        int padding = 8;
        int startX = px + padding;
        int startY = py + 22;

        int mx = lastMouseX, my = lastMouseY;
        int index = 0;
        for (ResourceLocation tex : textures) {
            int cx = startX + (index % cols) * (thumbSize + padding);
            int cy = startY + (index / cols) * (thumbSize + padding);

            boolean hover = mx >= cx && mx <= cx + thumbSize && my >= cy && my <= cy + thumbSize;
            int outline = hover ? 0xFF55AAFF : 0xFF3C4450;

            // tile background
            g.fill(cx - 1, cy - 1, cx + thumbSize + 1, cy + thumbSize + 1, 0xFF000000);
            g.renderOutline(cx - 1, cy - 1, thumbSize + 2, thumbSize + 2, outline);

            // ✅ load and draw texture preview properly
            try {
                ResourceLocation previewTex = GuiTextureHelper.toGuiTexture(tex.toString());
                if (previewTex != null) {
                    com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, previewTex);
                    g.blit(previewTex, cx, cy, 0, 0, thumbSize, thumbSize, thumbSize, thumbSize);
                } else {
                    g.fill(cx, cy, cx + thumbSize, cy + thumbSize, 0xFFFF00FF);
                }

            } catch (Exception e) {
                CatocraftMod.LOGGER.warn("[MenuEditor] Missing texture preview for {}", tex);
                g.fill(cx, cy, cx + thumbSize, cy + thumbSize, 0xFFFF00FF); // magenta fallback
            }

            // ✅ click to select
            if (hover && clickedThisFrame) {
                applySelectedTexture(tex, title);
                onSelect.accept(tex);
                g.pose().popPose();
                return;
            }

            index++;
        }

        // ✅ click outside to close popup
        if (clickedThisFrame && (mx < px || mx > px + pw || my < py || my > py + ph)) {
            backgroundTexturePopupOpen = false;
            slotTexturePopupOpen = false;
        }

        g.pose().popPose();
    }

    /** Applies the selected texture immediately for feedback + autosave + live refresh. */
    private void applySelectedTexture(ResourceLocation tex, String title) {
        try {
            boolean isBackground = title.toLowerCase().contains("background");

            if (isBackground) {
                layout.setBackgroundTexture(tex.toString());
                backgroundTexturePopupOpen = false;
                showSuccess("✅ Background set to " + tex.getPath());
            } else if (contextSlot != null) {
                contextSlot.setTextureOverride(tex.toString());
                slotTexturePopupOpen = false;
                showSuccess("✅ Slot texture set to " + tex.getPath());
            }

            autoSave();

            // ✅ Safe resource refresh & delayed UI rebuild
            Minecraft.getInstance().execute(() -> {
                try {
                    var mgr = Minecraft.getInstance().getResourceManager();
                    mgr.listResources("textures", p -> true);
                    CatocraftMod.LOGGER.debug("[MenuEditor] Texture refreshed: {}", tex);
                } catch (Exception ignored) {}
                Minecraft.getInstance().tell(this::rebuildWidgets);
            });

        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[MenuEditor] Failed to apply selected texture", e);
            showError("Texture apply failed");
        }
    }

    // ─────────────────── Mouse & Size ───────────────────

    /** Creates a deep copy of a slot and enters ghost placement for it. */
    private void duplicateSlot(MenuSlotDefinition original) {
        if (original == null) return;

        MenuSlotDefinition clone = original.copy();

        String base = (original.getId() == null || original.getId().isBlank()) ? "slot" : original.getId();
        String newId = makeUniqueId(base + "_copy");
        clone.setId(newId);

        int nx = Math.min(original.getX() + SLOT_GRID_STEP, canvasW - SLOT_W - OUTER_GAP);
        int ny = Math.min(original.getY() + SLOT_GRID_STEP, canvasH - SLOT_H - OUTER_GAP);
        clone.setX(nx);
        clone.setY(ny);

        // best effort copy of optional type/tag
        setSlotTypeReflect(clone, getSlotTypeReflect(original));
        setSlotTagReflect(clone, getSlotTagReflect(original));

        layout.getSlots().add(clone);
        startGhost(clone);
    }

    /** Ensures the ID is unique among current slots. */
    private String makeUniqueId(String candidate) {
        String id = candidate;
        int i = 2;
        while (hasSlotId(id)) {
            id = candidate + i;
            i++;
        }
        return id;
    }

    private boolean hasSlotId(String id) {
        for (MenuSlotDefinition s : layout.getSlots()) {
            if (id != null && id.equals(s.getId())) return true;
        }
        return false;
    }

    private int getTagButtonY() {
        int px = this.width - TOOLBOX_WIDTH;
        int btnX = px + 6;
        int btnW = TOOLBOX_WIDTH - 12;
        int btnH = 18;

        int yType  = 14 + 16;
        int yGroup = yType + btnH + 14 + 6;
        int yTag   = yGroup + btnH + 14 + 6;
        return yTag;
    }

    private void drawTagPopup(GuiGraphics g, boolean clickedThisFrame) {
        if (!tagPopupOpen || contextSlot == null) return;

        int px = this.width - TOOLBOX_WIDTH;
        int py = getTagButtonY() + 20;
        int pw = TOOLBOX_WIDTH;
        int ph = 110;

        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);

        // Background
        g.fill(px, py, px + pw, py + ph, PANEL_DARKER);
        g.renderOutline(px, py, pw, ph, PANEL_BORDER);

        g.drawCenteredString(this.font, "Add Tag", px + pw / 2, py + 6, TEXT_MAIN);

        // EDIT BOX (visible ON TOP of popup)
        int inputX = px + 6;
        int inputY = py + 20;
        int inputW = pw - 12;
        int inputH = 14;

        tagInputBox.setX(inputX);
        tagInputBox.setY(inputY);
        tagInputBox.setWidth(inputW);
        tagInputBox.setHeight(inputH);
        tagInputBox.setVisible(true);

        tagInputBox.render(g, lastMouseX, lastMouseY, 0); // <-- Important!!

        // ADD TAG button
        int btnY = inputY + inputH + 6;
        int btnH = 16;
        boolean hoverAdd = isHover(lastMouseX, lastMouseY, inputX, btnY, inputW, btnH);

        g.fill(inputX, btnY, inputX + inputW, btnY + btnH, hoverAdd ? PANEL_LIGHTER : PANEL_DARKER);
        g.renderOutline(inputX, btnY, inputW, btnH, hoverAdd ? ACCENT_BLUE : PANEL_BORDER);
        g.drawCenteredString(this.font, "Add Tag", inputX + inputW / 2, btnY + 4, TEXT_MAIN);

        // Suggested tags
        int listY = btnY + btnH + 6;
        int optionH = 14;

        for (String tag : TAG_OPTIONS) {
            boolean hover = isHover(lastMouseX, lastMouseY, inputX, listY, inputW, optionH);

            g.fill(inputX, listY, inputX + inputW, listY + optionH, hover ? PANEL_LIGHTER : PANEL_BG);
            String shortName = shortenTag(tag);
            g.drawString(this.font, shortName, inputX + 4, listY + 3, TEXT_MAIN);

            listY += optionH;
            if (listY + optionH > py + ph - 4) break;
        }

        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // ========================================================
        // A) SCREEN-BLOCKING POPUPS (always processed first)
        // ========================================================

        // --- Texture selector popup ---
        if (button == 0 && (backgroundTexturePopupOpen || slotTexturePopupOpen)) {

            int pw = 200, ph = 200;
            int px = (this.width - pw) / 2;
            int py = (this.height - ph) / 2;

            // Click outside → close
            if (mouseX < px || mouseX > px + pw || mouseY < py || mouseY > py + ph) {
                backgroundTexturePopupOpen = false;
                slotTexturePopupOpen = false;
                return true;
            }

            // Click inside grid
            List<ResourceLocation> list = backgroundTexturePopupOpen ? BACKGROUND_TEXTURES : SLOT_TEXTURES;
            int cols = 4, thumb = 32, pad = 8;
            int startX = px + pad, startY = py + 22;

            for (int i = 0; i < list.size(); i++) {
                int cx = startX + (i % cols) * (thumb + pad);
                int cy = startY + (i / cols) * (thumb + pad);

                if (mouseX >= cx && mouseX <= cx + thumb && mouseY >= cy && mouseY <= cy + thumb) {
                    ResourceLocation tex = list.get(i);
                    applySelectedTexture(tex, backgroundTexturePopupOpen ? "Background" : "Slot Texture");
                    showSuccess(backgroundTexturePopupOpen ? "Background set!" : "Slot texture changed!");
                    autoSave();
                    backgroundTexturePopupOpen = false;
                    slotTexturePopupOpen = false;
                    return true;
                }
            }
            return true;
        }


        // ========================================================
        // B) PANEL POPUPS (handled BEFORE super.mouseClicked!)
        // ========================================================

        // --- Tag popup anchored under Tag button ---
        if (tagPopupOpen && contextSlot != null) {
            int px = this.width - TOOLBOX_WIDTH;
            int py = getTagButtonY() + 20;
            int pw = TOOLBOX_WIDTH;
            int ph = 110;

            // Click inside popup → let textbox/buttons handle it
            if (mouseX >= px && mouseX <= px + pw &&
                    mouseY >= py && mouseY <= py + ph) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            // Click outside → close popup
            tagPopupOpen = false;
            tagInputBox.setVisible(false);
            return true;
        }


        // ========================================================
        // C) BASE VANILLA HANDLING
        // ========================================================
        boolean handled = super.mouseClicked(mouseX, mouseY, button);


        // ========================================================
        // D) TYPE / GROUP POPUPS
        // ========================================================

        if (button == 0 && showRightPanel && contextSlot != null) {

            int optionH = 18;

            // --- Type popup ---
            if (typePopupOpen) {
                int[] r = getPopupRect("type");
                int px = r[0], py = r[1], pw = r[2];

                List<String> pretty = new ArrayList<>();
                for (SlotType t : SlotType.values())
                    pretty.add(prettifyTypeName(t.getDisplayName()));

                int y = py + 2;
                for (String opt : pretty) {
                    if (isHover(mouseX, mouseY, px, y, pw, optionH)) {

                        for (SlotType t : SlotType.values()) {
                            if (prettifyTypeName(t.getDisplayName()).equalsIgnoreCase(opt)) {
                                setSlotTypeReflect(contextSlot, t.getDisplayName());
                                autoSave();
                                showSuccess("Type set to: " + prettifyTypeName(t.getDisplayName()));
                                syncRightPanelFields(contextSlot);
                                break;
                            }
                        }
                        typePopupOpen = false;
                        return true;
                    }
                    y += optionH;
                }

                // click outside
                int totalH = pretty.size() * optionH + 4;
                if (!isHover(mouseX, mouseY, px, py, pw, totalH)) {
                    typePopupOpen = false;
                    return true;
                }
            }

            // --- Group popup ---
            if (groupPopupOpen) {
                int[] r = getPopupRect("group");
                int px = r[0], py = r[1], pw = r[2];

                List<String> options = new ArrayList<>(GROUP_OPTIONS);

                List<String> custom = net.chriskatze.catocraftmod.util.ItemTypeRegistry.getAllTypeNames();
                if (!custom.isEmpty()) {
                    options.add("──────────");
                    options.addAll(custom);
                }

                int y = py + 2;
                for (String opt : options) {

                    if (opt.equals("──────────")) {
                        y += 6;
                        continue;
                    }

                    if (isHover(mouseX, mouseY, px, y, pw, optionH)) {
                        contextSlot.setGroup(opt);
                        autoSave();
                        showSuccess("Group set to: " + opt);
                        syncRightPanelFields(contextSlot);
                        groupPopupOpen = false;
                        return true;
                    }
                    y += optionH;
                }

                int totalH = options.size() * optionH + 4;
                if (!isHover(mouseX, mouseY, px, py, pw, totalH)) {
                    groupPopupOpen = false;
                    return true;
                }
            }
        }


        // ========================================================
        // E) TOOLBOX BUTTONS
        // ========================================================
        if (button == 0) {

            // Left-panel: background texture
            if (bgBtnW > 0 &&
                    mouseX >= bgBtnX && mouseX <= bgBtnX + bgBtnW &&
                    mouseY >= bgBtnY && mouseY <= bgBtnY + bgBtnH) {

                backgroundTexturePopupOpen = true;
                slotTexturePopupOpen = false;
                return true;
            }

            // Left-panel: item type manager
            if (itemTypesBtnW > 0 &&
                    mouseX >= itemTypesBtnX && mouseX <= itemTypesBtnX + itemTypesBtnW &&
                    mouseY >= itemTypesBtnY && mouseY <= itemTypesBtnY + itemTypesBtnH) {

                Minecraft.getInstance().setScreen(new ItemTypeManagerScreen(this));
                return true;
            }

            // Right-panel: slot texture override
            if (showRightPanel && contextSlot != null) {
                int px = this.width - TOOLBOX_WIDTH;
                int btnX = px + 6;
                int btnW = TOOLBOX_WIDTH - 12;
                int btnH = 18;
                int btnY = getSlotTextureButtonY();

                if (isHover(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
                    slotTexturePopupOpen = true;
                    backgroundTexturePopupOpen = false;
                    return true;
                }
            }
        }


        // ========================================================
        // F) GHOST MODE / DUPLICATE / TEXT TOOL
        // ========================================================

        // Start ghost mode
        if (button == 0 && !ghostMode && !resizing) {
            if (hoveringToolSlot) {
                MenuSlotDefinition ghost = new MenuSlotDefinition(makeUniqueId("slot"), 8, 8, 18, "", "", false);
                setSlotTypeReflect(ghost, "inventory_slot");
                startGhost(ghost);
                return true;
            }
            if (hoveringToolText) {
                showWarning("🧾 (Text boxes coming soon)");
                return true;
            }
        }

        // Ghost mode placement
        if (ghostMode) {
            if (button == 1) { cancelGhost(); return true; }
            if (button == 0) { tryPlaceGhost(); return true; }
            return false;
        }

        // Ctrl-duplicate
        if (button == 0 && Screen.hasControlDown()) {
            for (int i = layout.getSlots().size() - 1; i >= 0; i--) {
                MenuSlotDefinition s = layout.getSlots().get(i);
                if (overSlot(mouseX, mouseY, s)) {
                    duplicateSlot(s);
                    return true;
                }
            }
        }

        // Shift multiselect
        if (button == 0 && Screen.hasShiftDown()) {
            for (int i = layout.getSlots().size() - 1; i >= 0; i--) {
                MenuSlotDefinition s = layout.getSlots().get(i);
                if (overSlot(mouseX, mouseY, s)) {
                    if (selectedSlots.contains(s)) selectedSlots.remove(s);
                    else selectedSlots.add(s);
                    return true;
                }
            }
            return false;
        }


        // ========================================================
        // G) NORMAL LEFT CLICK → select slot
        // ========================================================
        if (button == 0 && !Screen.hasShiftDown() && !Screen.hasControlDown()) {

            selectedSlots.clear();
            boolean hit = false;

            if (mouseX > TOOLBOX_WIDTH && mouseX < this.width - TOOLBOX_WIDTH) {

                for (int i = layout.getSlots().size() - 1; i >= 0; i--) {
                    MenuSlotDefinition s = layout.getSlots().get(i);

                    if (overSlot(mouseX, mouseY, s)) {
                        selectedSlots.add(s);
                        pendingClickSlot = s;
                        dragOffsetX = (int) mouseX - (canvasX + s.getX());
                        dragOffsetY = (int) mouseY - (canvasY + s.getY());
                        hit = true;
                        break;
                    }
                }
            }

            if (!hit && mouseX < this.width - TOOLBOX_WIDTH) {
                contextSlot = null;
                showRightPanel = false;
                typePopupOpen = false;
                groupPopupOpen = false;
                tagPopupOpen = false;
                syncRightPanelFields(null);
            }
        }


        // ========================================================
        // H) TAG CHIP REMOVAL
        // ========================================================
        if (button == 0 && showRightPanel && contextSlot != null) {

            for (TagChip chip : tagChipBounds) {
                if (chip.contains(mouseX, mouseY)) {

                    contextSlot.removeTag(chip.tag);

                    if (contextSlot.getTags().isEmpty())
                        contextSlot.setTag("");
                    else
                        contextSlot.setTag(contextSlot.getTags().get(0));

                    autoSave();
                    showSuccess("Removed tag: " + chip.tag);
                    syncRightPanelFields(contextSlot);
                    return true;
                }
            }
        }


        // ========================================================
        // I) RIGHT PANEL BUTTONS (Type, Group, Tag, Delete)
        // ========================================================
        if (button == 0 && showRightPanel && contextSlot != null) {

            int px = this.width - TOOLBOX_WIDTH;
            int btnX = px + 6;
            int btnW = TOOLBOX_WIDTH - 12;
            int btnH = 18;

            int yType = 14 + 16;
            int yGroup = yType + btnH + 14 + 6;
            int yTag = yGroup + btnH + 14 + 6;
            int yDel = getDeleteButtonY();

            // Type
            if (isHover(mouseX, mouseY, btnX, yType, btnW, btnH)) {
                typePopupOpen = !typePopupOpen;
                groupPopupOpen = false;
                tagPopupOpen = false;
                return true;
            }

            // Group
            if (isHover(mouseX, mouseY, btnX, yGroup, btnW, btnH)) {
                groupPopupOpen = !groupPopupOpen;
                typePopupOpen = false;
                tagPopupOpen = false;
                return true;
            }

            // Tag
            if (isHover(mouseX, mouseY, btnX, yTag, btnW, btnH)) {
                tagPopupOpen = !tagPopupOpen;
                typePopupOpen = false;
                groupPopupOpen = false;

                if (tagPopupOpen) {
                    tagInputBox.setVisible(true);
                    tagInputBox.setValue("");
                    tagInputBox.setFocused(true);
                } else {
                    tagInputBox.setVisible(false);
                }
                return true;
            }

            // Delete
            if (isHover(mouseX, mouseY, btnX, yDel, btnW, btnH)) {
                layout.getSlots().remove(contextSlot);
                showSuccess("Slot deleted");
                contextSlot = null;
                showRightPanel = false;
                autoSave();
                return true;
            }
        }

        // ========================================================
        // J) RIGHT CLICK CONTEXT MENU
        // ========================================================
        if (button == 1 && mouseX > TOOLBOX_WIDTH) {

            if (typePopupOpen || groupPopupOpen || tagPopupOpen) {
                typePopupOpen = false;
                groupPopupOpen = false;
                tagPopupOpen = false;
                return true;
            }

            MenuSlotDefinition clickedSlot = null;

            for (int i = layout.getSlots().size() - 1; i >= 0; i--) {
                MenuSlotDefinition s = layout.getSlots().get(i);
                if (overSlot(mouseX, mouseY, s)) {
                    clickedSlot = s;
                    break;
                }
            }

            if (clickedSlot != null) {

                // close if same slot
                if (contextSlot == clickedSlot && showRightPanel) {
                    contextSlot = null;
                    showRightPanel = false;
                    typePopupOpen = false;
                    groupPopupOpen = false;
                    tagPopupOpen = false;
                    syncRightPanelFields(null);
                    return true;
                }

                contextSlot = clickedSlot;
                showRightPanel = true;
                syncRightPanelFields(contextSlot);
                return true;
            }

            // click empty area → close right panel
            contextSlot = null;
            showRightPanel = false;
            typePopupOpen = false;
            groupPopupOpen = false;
            tagPopupOpen = false;
            syncRightPanelFields(null);
            return true;
        }


        // ========================================================
        // K) RESIZE HANDLE
        // ========================================================
        if (button == 0 && mouseX > TOOLBOX_WIDTH) {

            mouseDown = true;
            mouseDownX = (int) mouseX;
            mouseDownY = (int) mouseY;

            if (overHandle(mouseX, mouseY)) {
                resizing = true;
                resizeGrabOffsetX = (int) mouseX - (canvasX + canvasW);
                resizeGrabOffsetY = (int) mouseY - (canvasY + canvasH);
                return true;
            }
        }


        // ========================================================
        // L) FINAL FAILSAFE → close popups
        // ========================================================
        if (typePopupOpen || groupPopupOpen || tagPopupOpen) {
            typePopupOpen = false;
            groupPopupOpen = false;
            tagPopupOpen = false;
        }

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            mouseDown = false;

            // ───────────── Resize release ─────────────
            if (resizing) {
                resizing = false;

                if (wasResized) {
                    wasResized = false; // reset for next time
                    saveLayoutToFile(); // ensure one final save (immediate)
                    showSuccess("Canvas resized to " + canvasW + "×" + canvasH);
                }

                return true;
            }

            // ───────────── Slot dragging ─────────────
            if (draggingSlot != null) {
                draggingSlot = null;
                autoSave();
                return true;
            }

            // ───────────── Pick-up ghost ─────────────
            if (pendingClickSlot != null) {
                startGhost(pendingClickSlot);
                pendingClickSlot = null;
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0) {
            if (resizing) {
                int[] min = getMinCanvasSize();
                int minW = min[0], minH = min[1];

                // 🧭 Compute proposed new width/height relative to grab offset
                int newW = snapFloor((int) mouseX - resizeGrabOffsetX - canvasX, CANVAS_GRID_STEP);
                int newH = snapFloor((int) mouseY - resizeGrabOffsetY - canvasY, CANVAS_GRID_STEP);

                // Clamp between minimum required and fixed max (262×182)
                newW = Mth.clamp(newW, minW, 262);
                newH = Mth.clamp(newH, minH, 182);

                // 🔒 Always keep perfectly centered each frame
                canvasW = newW;
                canvasH = newH;
                wasResized = true; // 🟢 mark that an actual resize occurred
                canvasX = snapFloor((this.width - canvasW) / 2, CANVAS_GRID_STEP);
                canvasY = snapFloor((this.height - canvasH) / 2, CANVAS_GRID_STEP);

                // Keep layout synced & save
                layout.setWidth(canvasW);
                layout.setHeight(canvasH);
                autoSave();

                return true;
            }

            // Promote pending click → drag when threshold exceeded
            if (pendingClickSlot != null && !ghostMode) {
                if (Math.abs(mouseX - mouseDownX) >= DRAG_THRESHOLD ||
                        Math.abs(mouseY - mouseDownY) >= DRAG_THRESHOLD) {
                    draggingSlot = pendingClickSlot;
                    pendingClickSlot = null;
                }
            }

            // Active dragging
            if (draggingSlot != null) {
                int localX = (int) mouseX - canvasX - dragOffsetX;
                int localY = (int) mouseY - canvasY - dragOffsetY;
                int targetX = snapFloor(Mth.clamp(localX, 0, canvasW - SLOT_W), SLOT_GRID_STEP);
                int targetY = snapFloor(Mth.clamp(localY, 0, canvasH - SLOT_H), SLOT_GRID_STEP);

                // edge-slide resolution
                int[] resolved = resolveBlockedDrag(targetX, targetY, draggingSlot);
                int newX = resolved[0];
                int newY = resolved[1];

                int moveDX = newX - draggingSlot.getX();
                int moveDY = newY - draggingSlot.getY();

                List<MenuSlotDefinition> toMove = selectedSlots.contains(draggingSlot)
                        ? selectedSlots : List.of(draggingSlot);

                // Before committing, ensure none in the group will overlap or leave canvas
                boolean blocked = false;
                for (MenuSlotDefinition s : toMove) {
                    int nx = s.getX() + moveDX;
                    int ny = s.getY() + moveDY;
                    if (overlapsAny(nx, ny, s) || !isInsideCanvas(nx, ny)) {
                        blocked = true;
                        break;
                    }
                }
                if (!blocked) {
                    for (MenuSlotDefinition s : toMove) {
                        s.setX(s.getX() + moveDX);
                        s.setY(s.getY() + moveDY);
                    }
                    autoSave();
                }
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private int[] getMinCanvasSize() {
        int minW = OUTER_GAP * 2 + SLOT_W;
        int minH = OUTER_GAP * 2 + SLOT_H;
        for (MenuSlotDefinition s : layout.getSlots()) {
            int right  = s.getX() + SLOT_W + OUTER_GAP;
            int bottom = s.getY() + SLOT_H + OUTER_GAP;
            minW = Math.max(minW, right);
            minH = Math.max(minH, bottom);
        }
        minW = snapFloor(minW, CANVAS_GRID_STEP);
        minH = snapFloor(minH, CANVAS_GRID_STEP);
        return new int[]{minW, minH};
    }

    // ─────────────── Collision-aware dragging with edge-slide ───────────────

    private int[] resolveBlockedDrag(int targetX, int targetY, MenuSlotDefinition moving) {
        int minX = OUTER_GAP;
        int minY = OUTER_GAP;
        int maxX = canvasW - SLOT_W - OUTER_GAP;
        int maxY = canvasH - SLOT_H - OUTER_GAP;

        int curX = moving.getX();
        int curY = moving.getY();
        int newX = Mth.clamp(targetX, minX, maxX);
        int newY = Mth.clamp(targetY, minY, maxY);

        if (newX != curX) {
            int dir = Integer.signum(newX - curX);
            int tryX = curX;
            while (tryX != newX) {
                tryX += dir * SLOT_GRID_STEP;
                if (overlapsAny(tryX, curY, moving) || tryX < minX || tryX > maxX) {
                    tryX -= dir * SLOT_GRID_STEP;
                    break;
                }
            }
            newX = tryX;
        }
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

    private boolean overlapsAny(int x, int y, MenuSlotDefinition moving) {
        final int CORNER_TOLERANCE = 1;
        for (MenuSlotDefinition s : layout.getSlots()) {
            if (s == moving) continue; // skip itself (also handles ghost)
            int ax1 = x, ay1 = y, ax2 = x + SLOT_W, ay2 = y + SLOT_H;
            int bx1 = s.getX() - INNER_GAP, by1 = s.getY() - INNER_GAP;
            int bx2 = s.getX() + SLOT_W + INNER_GAP, by2 = s.getY() + SLOT_H + INNER_GAP;

            boolean overlapX = ax1 < bx2 && ax2 > bx1;
            boolean overlapY = ay1 < by2 && ay2 > by1;
            if (overlapX && overlapY) {
                int dx = Math.min(Math.abs(ax2 - bx1), Math.abs(ax1 - bx2));
                int dy = Math.min(Math.abs(ay2 - by1), Math.abs(ay1 - by2));
                if (dx < CORNER_TOLERANCE && dy < CORNER_TOLERANCE) continue;
                return true;
            }
        }
        return false;
    }

    // ─────────────── Ghost Mode ───────────────

    private void startGhost(MenuSlotDefinition s) {
        if (s == null) return;
        ghostMode = true;
        ghostSlot = s;
        ghostOrigX = s.getX();
        ghostOrigY = s.getY();
    }

    private void cancelGhost() {
        if (!ghostMode || ghostSlot == null) return;
        ghostMode = false;
        ghostSlot = null;
        showError("Placement canceled");
    }

    private void tryPlaceGhost() {
        if (!ghostMode || ghostSlot == null) return;

        int[] pos = ghostSnapped(lastMouseX, lastMouseY);
        int gx = pos[0], gy = pos[1];

        // 🧠 If this slot already exists in layout, we are *moving*, not adding
        boolean alreadyInLayout = layout.getSlots().contains(ghostSlot);

        if (!overlapsAny(gx, gy, ghostSlot)) {
            ghostSlot.setX(gx);
            ghostSlot.setY(gy);

            // only add if it's truly a new one
            if (!alreadyInLayout) layout.getSlots().add(ghostSlot);

            ghostMode = false;
            showSuccess(alreadyInLayout ? "Moved slot" : "Added new slot");
            ghostSlot = null;
            autoSave();
            return;
        }
        // Snap to nearest free if overlapping
        int[] nearest = findNearestFreePosition(gx, gy, ghostSlot);
        if (nearest != null) {
            ghostSlot.setX(nearest[0]);
            ghostSlot.setY(nearest[1]);

            if (!alreadyInLayout) layout.getSlots().add(ghostSlot);

            ghostMode = false;
            showSuccess(alreadyInLayout ? "Moved slot" : "Added new slot");
            ghostSlot = null;
            autoSave();
        }
    }

    private int[] ghostSnapped(int mouseX, int mouseY) {
        int localX = mouseX - canvasX - SLOT_W / 2;
        int localY = mouseY - canvasY - SLOT_H / 2;

        int minX = OUTER_GAP;
        int minY = OUTER_GAP;
        int maxX = canvasW - SLOT_W - OUTER_GAP;
        int maxY = canvasH - SLOT_H - OUTER_GAP;

        int gx = snapFloor(Mth.clamp(localX, minX, maxX), SLOT_GRID_STEP);
        int gy = snapFloor(Mth.clamp(localY, minY, maxY), SLOT_GRID_STEP);
        return new int[]{gx, gy};
    }

    /** Spiral search on grid to find closest valid cell near (tx,ty). */
    private int[] findNearestFreePosition(int tx, int ty, MenuSlotDefinition moving) {
        final int MAX_RADIUS = 80; // px search radius
        int bestX = -1, bestY = -1;
        int bestDist2 = Integer.MAX_VALUE;

        for (int r = 0; r <= MAX_RADIUS; r += SLOT_GRID_STEP) {
            for (int dx = -r; dx <= r; dx += SLOT_GRID_STEP) {
                int x1 = tx + dx;
                int y1 = ty - r;
                if (isInsideCanvas(x1, y1) && !overlapsAny(x1, y1, moving)) {
                    int d2 = (x1 - tx) * (x1 - tx) + (y1 - ty) * (y1 - ty);
                    if (d2 < bestDist2) { bestDist2 = d2; bestX = x1; bestY = y1; }
                }
                y1 = ty + r;
                if (isInsideCanvas(x1, y1) && !overlapsAny(x1, y1, moving)) {
                    int d2 = (x1 - tx) * (x1 - tx) + (y1 - ty) * (y1 - ty);
                    if (d2 < bestDist2) { bestDist2 = d2; bestX = x1; bestY = y1; }
                }
            }
            for (int dy = -r + SLOT_GRID_STEP; dy <= r - SLOT_GRID_STEP; dy += SLOT_GRID_STEP) {
                int y1 = ty + dy;
                int x1 = tx - r;
                if (isInsideCanvas(x1, y1) && !overlapsAny(x1, y1, moving)) {
                    int d2 = (x1 - tx) * (x1 - tx) + (y1 - ty) * (y1 - ty);
                    if (d2 < bestDist2) { bestDist2 = d2; bestX = x1; bestY = y1; }
                }
                x1 = tx + r;
                if (isInsideCanvas(x1, y1) && !overlapsAny(x1, y1, moving)) {
                    int d2 = (x1 - tx) * (x1 - tx) + (y1 - ty) * (y1 - ty);
                    if (d2 < bestDist2) { bestDist2 = d2; bestX = x1; bestY = y1; }
                }
            }
            if (bestX != -1) break;
        }
        return bestX == -1 ? null : new int[]{bestX, bestY};
    }

    private boolean isInsideCanvas(int localX, int localY) {
        int minX = OUTER_GAP;
        int minY = OUTER_GAP;
        int maxX = canvasW - SLOT_W - OUTER_GAP;
        int maxY = canvasH - SLOT_H - OUTER_GAP;
        return localX >= minX && localX <= maxX && localY >= minY && localY <= maxY;
    }

    // ─────────────── Helpers ───────────────

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

    private boolean overSlot(double mx, double my, MenuSlotDefinition s) {
        int ax = canvasX + s.getX();
        int ay = canvasY + s.getY();
        return mx >= ax && mx <= ax + SLOT_W && my >= ay && my <= ay + SLOT_H;
    }

    @Override
    public void onClose() {
        if (ghostMode) cancelGhost();
        saveLayoutToFile();
        super.onClose();
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.grabMouse();
        mc.setScreen(null);
    }

    // ─────────────────── Dropdown Rendering ───────────────────
    private void drawDropdowns(GuiGraphics g, int mouseX, int mouseY) {
        if (typeDropdownOpen) {
            // 🧹 Prepare parallel lists: pretty display and raw values
            List<String> rawTypes = TYPE_OPTIONS;
            List<String> prettyTypes = new ArrayList<>();
            for (String raw : rawTypes) prettyTypes.add(prettifyTypeName(raw));

            drawDropdown(g, prettyTypes, slotTypeDropdownBtn.getX(), slotTypeDropdownBtn.getY() + slotTypeDropdownBtn.getHeight() + 2,
                    selectedPretty -> {
                        if (contextSlot != null && selectedPretty != null) {
                            // Map pretty name back to raw internal name
                            String matchedRaw = "";
                            for (String raw : rawTypes) {
                                if (prettifyTypeName(raw).equalsIgnoreCase(selectedPretty)) {
                                    matchedRaw = raw;
                                    break;
                                }
                            }
                            if (!matchedRaw.isEmpty()) {
                                setSlotTypeReflect(contextSlot, matchedRaw);
                                autoSave();
                                showSuccess("Type set to: " + selectedPretty);
                            }
                        }
                        typeDropdownOpen = false;
                    });
        }
    }

    /** Generic dropdown helper with click handling */
    private void drawDropdown(GuiGraphics g, List<String> options, int x, int y, java.util.function.Consumer<String> onSelect) {
        final int optionHeight = 14;
        final int width = 120;
        int visibleCount = Math.min(options.size(), 8);
        int height = visibleCount * optionHeight;

        // background + border
        g.fill(x, y, x + width, y + height, 0xEE202020);
        g.hLine(x, x + width, y, 0xFF55FF55);
        g.hLine(x, x + width, y + height, 0xFF55FF55);
        g.vLine(x, y, y + height, 0xFF55FF55);
        g.vLine(x + width, y, y + height, 0xFF55FF55);

        int mx = lastMouseX, my = lastMouseY;
        for (int i = 0; i < visibleCount; i++) {
            int oy = y + i * optionHeight;
            boolean hovered = mx >= x && mx <= x + width && my >= oy && my <= oy + optionHeight;
            int color = hovered ? 0xFF55FF55 : 0xFFFFFFFF;
            g.drawString(this.font, options.get(i), x + 4, oy + 3, color, false);
        }

        // Handle click detection
        boolean leftDown = Minecraft.getInstance().mouseHandler.isLeftPressed();
        if (leftDown && !wasLeftDown) {
            int mxClick = lastMouseX;
            int myClick = lastMouseY;

            for (int i = 0; i < visibleCount; i++) {
                int oy = y + i * optionHeight;
                if (mxClick >= x && mxClick <= x + width && myClick >= oy && myClick <= oy + optionHeight) {
                    onSelect.accept(options.get(i)); // choose option
                    wasLeftDown = leftDown;          // update state before return
                    return;
                }
            }

            // Click outside closes dropdown
            if (mxClick < x || mxClick > x + width || myClick < y || myClick > y + height) {
                onSelect.accept(null); // cancel/close
            }
        }
        wasLeftDown = leftDown;
    }


    // ───────────── Reflection adapters for optional fields (type/tag) ─────────────

    private static String getSlotTypeReflect(Object o) {
        String m = invokeGetter(o, "getType", "getSlotType", "getCategory");
        if (m != null) return m;
        return readField(o, "type", "slotType", "category");
    }

    private static void setSlotTypeReflect(Object o, String v) {
        if (invokeSetter(o, v, "setType", "setSlotType", "setCategory")) return;
        writeField(o, v, "type", "slotType", "category");
    }

    private static String getSlotTagReflect(Object o) {
        String m = invokeGetter(o, "getTag", "getModTag", "getFilterTag");
        if (m != null) return m;
        return readField(o, "tag", "modTag", "filterTag");
    }

    private static void setSlotTagReflect(Object o, String v) {
        if (invokeSetter(o, v, "setTag", "setModTag", "setFilterTag")) return;
        writeField(o, v, "tag", "modTag", "filterTag");
    }

    private static String invokeGetter(Object o, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = o.getClass().getMethod(name);
                Object r = m.invoke(o);
                return r == null ? "" : String.valueOf(r);
            } catch (Exception ignored) {}
        }
        return "";
    }

    private static boolean invokeSetter(Object o, String v, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = o.getClass().getMethod(name, String.class);
                m.invoke(o, v == null ? "" : v);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static String readField(Object o, String... fieldNames) {
        for (String name : fieldNames) {
            try {
                Field f = o.getClass().getField(name);
                Object r = f.get(o);
                return r == null ? "" : String.valueOf(r);
            } catch (Exception ignored) {}
        }
        return "";
    }

    private static void writeField(Object o, String v, String... fieldNames) {
        for (String name : fieldNames) {
            try {
                Field f = o.getClass().getField(name);
                f.setAccessible(true);
                f.set(o, v == null ? "" : v);
                return;
            } catch (Exception ignored) {}
        }
    }

    /** Exact Y of the Delete button, perfectly aligned with dynamic panels. */
    private int getDeleteButtonY() {
        int y = getSlotTextureButtonY();

        // Slot Texture button height (fixed)
        int btnH = 18;

        // Space after Slot Texture button
        y += btnH + 14;

        return y;
    }

    /** Helper: exact Y of the "🎨 Slot Texture" button based on cached right-panel layout */
    private int getSlotTextureButtonY() {
        // cachedTagSectionBottom already includes:
        // - Type button block
        // - Group button block
        // - Tag button block
        // - All tag chips
        // - +8 margin below chips (set in drawRightPanel)

        int y = cachedTagSectionBottom;

        // "Slot Texture" header height
        y += 8;   // header spacing
        y += 12;  // "Slot Texture" actual button Y

        return y;
    }

    /**
     * Draws the popup list visually (no click logic).
     * Now includes:
     *  - double-fill (solid black + tinted overlay) to fully block text behind
     *  - subtle outline and hover accent
     *  - top-layer visual priority
     */
    private void drawPopupListVisual(GuiGraphics g, List<String> options,
                                     int startX, int startY, String title) {
        if (options == null || options.isEmpty()) return;

        g.flush();
        g.pose().pushPose();
        g.pose().translate(0, 0, 500); // 🧱 draw above all prior UI

        int pw = TOOLBOX_WIDTH;
        int optionH = 18;
        int totalH = options.size() * optionH + 4;

        // Fully opaque mask
        g.fill(startX, startY, startX + pw, startY + totalH, 0xFF000000);
        g.fill(startX + 1, startY + 1, startX + pw - 1, startY + totalH - 1, 0xFF1C1E25);
        g.renderOutline(startX, startY, pw, totalH, PANEL_BORDER);

        int y = startY + 2;
        for (String opt : options) {
            boolean hovered = isHover(lastMouseX, lastMouseY, startX, y, pw, optionH);
            int bg = hovered ? PANEL_LIGHTER : PANEL_DARKER;
            int border = hovered ? ACCENT_BLUE : PANEL_BORDER;

            g.fill(startX + 2, y, startX + pw - 2, y + optionH, bg);
            g.renderOutline(startX + 2, y, pw - 4, optionH, border);
            g.drawCenteredString(this.font, opt, startX + pw / 2, y + 5, TEXT_MAIN);
            y += optionH;
        }

        g.pose().popPose();
    }

    /** Converts raw slot type names like "inventory_slot" or "Magic_Upgrade_Slot" into "Inventory" or "Magic Upgrade". */
    private static String prettifyTypeName(String raw) {
        if (raw == null || raw.isBlank()) return "";

        // Normalize case and remove any "_slot" or " slot" suffix
        String cleaned = raw
                .trim()
                .replaceAll("(?i)_?slot$", "") // removes trailing "_slot" or "slot" (case-insensitive)
                .replaceAll("(?i)\\bslot\\b", "") // removes standalone word "slot"
                .replace("_", " ")
                .trim();

        if (cleaned.isEmpty()) return "";

        // Capitalize each word (e.g. "magic upgrade" → "Magic Upgrade")
        String[] parts = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
            sb.append(" ");
        }

        return sb.toString().trim();
    }

    // Small safe-get helpers (kept for parity)
    @FunctionalInterface private interface SupplierEx<T> { T get() throws Exception; }
    @FunctionalInterface private interface RunnableEx { void run() throws Exception; }

    private static final class GLFW {
        static final int GLFW_KEY_ESCAPE = 256;
        static final int GLFW_KEY_A = 65;
        static final int GLFW_KEY_DELETE = 261;
        static final int GLFW_KEY_S = 83;
        static final int GLFW_KEY_Z = 90;
        static final int GLFW_KEY_Y = 89;
    }
}