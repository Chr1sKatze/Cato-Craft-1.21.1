package net.chriskatze.catocraftmod.menu.client.screen;


import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.EquipmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 💠 EquipmentScreen
 *
 * Dynamic player equipment GUI driven entirely by JSON layouts.
 * Automatically refreshes when {@link PlayerEquipmentCapability} syncs from the server.
 *
 * Also handles runtime rebuilds when layouts or slot counts change
 * (e.g., after /reload or when new EquipmentGroups are registered).
 */
public class EquipmentScreen extends DynamicScreen<EquipmentMenu> {

    private int lastSlotCount = -1; // tracks layout changes for auto-rebuilds

    public EquipmentScreen(EquipmentMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    /**
     * Called whenever the equipment capability syncs to the client.
     * Updates visible items and optionally rebuilds the GUI if slot groups changed.
     */
    public void refreshFromCapability(PlayerEquipmentCapability cap) {
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EquipmentScreen] Tried to refresh from null capability.");
            return;
        }

        // 🔁 Update item stacks
        this.menu.updateFromCapability(cap);

        // 🧱 Rebuild layout if slot count changed (e.g., after /reload)
        int currentSlotCount = this.menu.slots.size();
        if (currentSlotCount != lastSlotCount) {
            lastSlotCount = currentSlotCount;
            this.rebuildLayout();
            CatocraftMod.LOGGER.info("[EquipmentScreen] Layout rebuilt ({} total slots).", currentSlotCount);
        }

        CatocraftMod.LOGGER.debug("[EquipmentScreen] Refreshed from capability ({} groups).",
                cap.getAllGroups().size());
    }

    /**
     * Safely rebuilds the screen layout after slot structure or JSON changes.
     * Prevents unnecessary full re-init() calls every tick.
     */
    private void rebuildLayout() {
        try {
            this.clearWidgets(); // clean UI elements (buttons, etc.)
            this.init();          // rebuild layout dynamically
        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[EquipmentScreen] Failed to rebuild layout: {}", e.toString());
        }
    }

    @Override
    protected ResourceLocation layoutId() {
        // Default UI JSON for this screen:
        // data/catocraftmod/ui/equipment/main.json
        return CatocraftMod.id("ui/equipment/main");
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);

        // 🎨 Optional overlays for future visual feedback:
        for (Slot slot : this.menu.slots) {
            if (slot.hasItem()) {
                // Example debug overlay: subtle glow for filled slots
                // graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x22FFFFFF);
            }
        }
    }
}