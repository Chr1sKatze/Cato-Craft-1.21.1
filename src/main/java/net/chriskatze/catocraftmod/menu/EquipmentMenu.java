package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.chriskatze.catocraftmod.menu.layout.SlotLayout;
import net.chriskatze.catocraftmod.menu.layout.SlotLayoutLoader;
import net.chriskatze.catocraftmod.menu.slot.ModifiableSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 💠 EquipmentMenu — data-driven equipment container
 *
 * Builds slots dynamically based on registered {@link EquipmentGroup}s
 * and their associated {@link SlotLayout} JSONs.
 *
 * Supports dynamic hot-reloading of group definitions without restarting.
 */
public class EquipmentMenu extends AbstractContainerMenu {

    public static final Component TITLE = Component.translatable("menu.catocraftmod.equipment");

    /** Start indices of each equipment group inside this container (keyed by group key). */
    private final Map<String, Integer> groupStartIndices = new LinkedHashMap<>();

    /** Keeps track of all SimpleContainers for slot ownership (keyed by group key). */
    private final Map<String, SimpleContainer> groupContainers = new LinkedHashMap<>();

    // ────────────────────────────────────────────────
    // Constructor
    // ────────────────────────────────────────────────
    public EquipmentMenu(int id, Inventory playerInventory) {
        super(ModMenus.EQUIPMENT_MENU.get(), id);

        initializeEquipmentSlots();

        // Add player inventory + hotbar after equipment section
        addPlayerInventory(playerInventory, 8, 84);
        addPlayerHotbar(playerInventory, 8, 142);
    }

    // ────────────────────────────────────────────────
    // Slot creation from JSON layout
    // ────────────────────────────────────────────────
    private void initializeEquipmentSlots() {
        final int defaultX = 80;
        final int defaultY = 20;
        int fallbackOffsetY = 0; // used for groups without layout definitions

        for (EquipmentGroup group : EquipmentGroup.all()) {
            String key = group.getKey();
            var groupId = group.getGroupId();
            SlotLayout layout = SlotLayoutLoader.get(groupId);

            // 🧩 Fallback: no layout found
            if (layout == null) {
                CatocraftMod.LOGGER.warn(
                        "[EquipmentMenu] No layout found for group '{}' — using fallback slot.",
                        key
                );
                SimpleContainer fallback = new SimpleContainer(1);
                int slotIndex = this.slots.size();
                int yPos = defaultY + (fallbackOffsetY * 20);

                this.addSlot(new ModifiableSlot(fallback, group, 0, defaultX, yPos));
                groupStartIndices.put(key, slotIndex);
                groupContainers.put(key, fallback);

                fallbackOffsetY++;
                continue;
            }

            // JSON layout available — create slots according to it
            var cells = layout.cells();
            if (cells == null || cells.isEmpty()) {
                CatocraftMod.LOGGER.warn("[EquipmentMenu] Layout {} has no cells defined.", groupId);
                continue;
            }

            SimpleContainer container = new SimpleContainer(cells.size());
            groupContainers.put(key, container);
            int startIndex = this.slots.size();
            groupStartIndices.put(key, startIndex);

            for (int i = 0; i < cells.size(); i++) {
                var cell = cells.get(i);
                this.addSlot(new ModifiableSlot(container, group, i, cell.x(), cell.y()));
            }

            CatocraftMod.LOGGER.debug(
                    "[EquipmentMenu] Added {} slot(s) for group '{}' at indices starting {}",
                    cells.size(), key, startIndex
            );
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // ────────────────────────────────────────────────
    // Shift-click placeholder
    // ────────────────────────────────────────────────
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
    }

    private void addPlayerHotbar(Inventory inv, int x, int y) {
        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(inv, col, x + col * 18, y));
    }

    // ────────────────────────────────────────────────
    // Capability → Menu Sync Bridge (diff-aware update)
    // ────────────────────────────────────────────────
    public void updateFromCapability(PlayerEquipmentCapability cap) {
        if (cap == null) return;

        cap.getAllGroups().forEach((group, handler) -> {
            String key = group.getKey();
            int startIndex = groupStartIndices.getOrDefault(key, -1);
            if (startIndex == -1) return;

            for (int i = 0; i < handler.getSlots(); i++) {
                int slotIndex = getSlotIndexForGroup(key, i);
                if (slotIndex < 0 || slotIndex >= this.slots.size()) continue;

                ItemStack newStack = handler.getStackInSlot(i);
                Slot slot = this.slots.get(slotIndex);
                ItemStack oldStack = slot.getItem();

                // ⚡ Only update if something actually changed
                if (!ItemStack.isSameItemSameComponents(oldStack, newStack)) {
                    if (slot.container != null) {
                        slot.container.setItem(slot.getSlotIndex(), newStack.copy());
                    }
                    slot.setChanged();
                }
            }
        });

        this.broadcastChanges();
    }

    /** Returns the UI slot index for a group's internal slot by group key. */
    private int getSlotIndexForGroup(String groupKey, int slotInGroup) {
        Integer base = groupStartIndices.get(groupKey);
        return base == null ? -1 : base + slotInGroup;
    }

    // ────────────────────────────────────────────────
    // Provider
    // ────────────────────────────────────────────────
    public static class Provider implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return TITLE;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new EquipmentMenu(id, inv);
        }
    }
}