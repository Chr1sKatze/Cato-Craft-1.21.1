package net.chriskatze.catocraftmod.menu.runtime;

import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.chriskatze.catocraftmod.creator.menu.MenuSlotDefinition;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;


/**
 * DynamicMenu builds its slot layout from a MenuLayout JSON.
 * Now supports persistent storage per-player (temporary, non-file).
 */
public class DynamicMenu extends AbstractContainerMenu {

    private final MenuLayout layout;
    private final ItemStackHandler handler;
    private final Player player;

    public DynamicMenu(MenuType<?> type, int id, Inventory playerInv, MenuLayout layout) {
        super(type, id);
        this.layout = layout != null ? layout : new MenuLayout("fallback", 9, 3);
        this.player = playerInv.player;

        // 🧠 Try to reuse or create a new handler for this layout
        this.handler = DynamicMenuStorage.getOrCreate(player, this.layout.getName(), this.layout.getSlots().size());
        DynamicMenuStorage.setLastOpened(player, this.layout.getName());

        // ────────────────────────────────────────────────
        // Layout-defined slots
        // ────────────────────────────────────────────────
        if (this.layout.getSlots() != null && !this.layout.getSlots().isEmpty()) {
            for (int i = 0; i < this.layout.getSlots().size(); i++) {
                MenuSlotDefinition def = this.layout.getSlots().get(i);

                this.addSlot(new Slot(new HandlerWrapper(handler, i), i, def.getX(), def.getY()) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return super.mayPlace(stack); // future tag validation
                    }
                });
            }
        }

        // ────────────────────────────────────────────────
        // Player inventory
        // ────────────────────────────────────────────────
        addPlayerInventory(playerInv, this.layout.getHeight() * 18 + 40);
    }

    private void addPlayerInventory(Inventory inv, int offsetY) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, offsetY + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, offsetY + 58));
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
        return ItemStack.EMPTY; // handle shift-click later
    }

    public MenuLayout getLayout() {
        return layout;
    }
}