package net.chriskatze.catocraftmod.menu.slot;

import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.chriskatze.catocraftmod.menu.layout.SlotEquipValidator;
import net.chriskatze.catocraftmod.network.EquipmentSlotUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 🔹 ModifiableSlot — reusable slot that auto-syncs to the server when modified.
 *
 * Used by data-driven equipment menus. Each slot knows:
 *  - Its {@link EquipmentGroup}
 *  - Its index within that group
 *  - Validation rules via {@link SlotEquipValidator}
 */
public class ModifiableSlot extends Slot {

    private final EquipmentGroup group;
    private final int groupIndex;
    private boolean suppressNextSync = false;

    public ModifiableSlot(Container container, EquipmentGroup group, int groupIndex, int x, int y) {
        super(container, groupIndex, x, y);
        this.group = group;
        this.groupIndex = groupIndex;
    }

    @Override
    public void set(ItemStack stack) {
        suppressNextSync = true;
        super.set(stack);
        suppressNextSync = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.level() == null || !player.level().isClientSide) return;
        if (suppressNextSync) return;

        ItemStack stack = this.container.getItem(this.index);
        EquipmentSlotUpdatePacket.sendToServer(group, groupIndex, stack);
    }

    // ────────────────────────────────────────────────
    // Item Validation
    // ────────────────────────────────────────────────
    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) return true;
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        return SlotEquipValidator.canEquipItem(player, group.getGroupId(), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
    }

    public EquipmentGroup getGroup() {
        return group;
    }

    public int getGroupIndex() {
        return groupIndex;
    }
}