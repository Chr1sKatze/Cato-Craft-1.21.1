package net.chriskatze.catocraftmod.menu.runtime;

import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ResultContainer;

import java.util.List;

/**
 * DynamicMenu builds its slot layout from a MenuLayout JSON.
 * Supports runtime SlotType logic via RuntimeSlotBuilder.
 *
 * 🧠 In this version:
 *  - Only JSON-defined slots are added (no vanilla player inventory).
 *  - layout.getWidth()/Height() are treated as pixel dimensions.
 *  - Each slot comes directly from the editor’s coordinates.
 */
public class DynamicMenu extends AbstractContainerMenu {

    private final MenuLayout layout;
    private final ItemStackHandler handler;
    private final Player player;

    // Optional extra containers (for crafting/result/misc)
    private final Container miscInv = new SimpleContainer(20);
    private final Container craftInv = new SimpleContainer(9);
    private final ResultContainer resultInv = new ResultContainer();

    public DynamicMenu(MenuType<?> type, int id, Inventory playerInv, MenuLayout layout) {
        super(type, id);
        this.layout = layout != null ? layout : new MenuLayout("fallback", 176, 166);
        this.player = playerInv.player;

        // Persistent handler (for custom slots)
        this.handler = DynamicMenuStorage.getOrCreate(player, this.layout.getName(), this.layout.getSlots().size());
        DynamicMenuStorage.setLastOpened(player, this.layout.getName());

        // ────────────────────────────────────────────────
        // Build all slots from JSON
        // ────────────────────────────────────────────────
        if (layout.getSlots() != null && !layout.getSlots().isEmpty()) {
            List<Slot> builtSlots = RuntimeSlotBuilder.buildSlots(
                    layout.getSlots(),
                    playerInv,
                    craftInv,
                    resultInv,
                    miscInv,
                    player
            );
            for (Slot slot : builtSlots) {
                this.addSlot(slot);
            }
        }

        // 🔸 Do NOT add vanilla player inventory automatically
        // That caused duplicated slots and misaligned layouts.
        // You can add a toggle or command later if you want the player inventory visible.
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        DynamicMenuStorage.save(player, layout.getName(), handler);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // TODO: handle shift-click routing later
        return ItemStack.EMPTY;
    }

    public MenuLayout getLayout() {
        return layout;
    }
}