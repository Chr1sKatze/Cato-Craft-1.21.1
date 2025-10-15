package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EarringCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEarringCapability;
import net.chriskatze.catocraftmod.network.EarringSyncHelper;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EarringMenu extends AbstractContainerMenu {

    public static final int EARRING_SLOT_INDEX = 0;

    private final Player player;
    private final PlayerEarringCapability handler;

    // 🧩 Cache to avoid redundant syncs/saves
    private ItemStack lastSyncedStack = ItemStack.EMPTY;
    private long lastSaveTime = 0L;
    private static final long SAVE_COOLDOWN_MS = 250; // debounce for rapid changes

    public EarringMenu(int id, Inventory inv, Player player) {
        super(ModMenus.EARRING_MENU.get(), id);
        this.player = player;

        boolean isClient = player.level().isClientSide;

        if (isClient) {
            this.handler = new PlayerEarringCapability(); // dummy
            CatocraftMod.LOGGER.debug("[EarringMenu] (CLIENT) Opened for {}", player.getName().getString());
        } else {
            var cap = player.getCapability(EarringCapabilityHandler.EARRING_CAP);
            if (cap == null) {
                CatocraftMod.LOGGER.warn("[EarringMenu] (SERVER) No earring capability found for {}", player.getName().getString());
                this.handler = new PlayerEarringCapability();
            } else {
                this.handler = cap;
            }
            CatocraftMod.LOGGER.debug("[EarringMenu] (SERVER) Opened for {} — handler = {}", player.getName().getString(), handler);

            // 🧠 Initialize slot with the saved capability stack
            ItemStack saved = handler.getStackInSlot(EARRING_SLOT_INDEX);
            if (!saved.isEmpty()) {
                this.handler.setStackInSlot(EARRING_SLOT_INDEX, saved.copy());
                CatocraftMod.LOGGER.debug("[EarringMenu] (SERVER) Restored saved earring stack: {}", saved);
            }
        }

        // Add the earring slot (uses the handler we just initialized)
        this.addSlot(new SlotItemHandler(handler, EARRING_SLOT_INDEX, 78, 16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.EARRINGS);
            }
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    // 🔁 Fires when slot content changes
    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if (!(player instanceof ServerPlayer sp)) return;

        ItemStack stackInSlot = this.slots.get(EARRING_SLOT_INDEX).getItem().copy();

        // ✅ Check for real change to prevent sync spam
        if (ItemStack.isSameItemSameComponents(stackInSlot, lastSyncedStack))
            return;

        long now = System.currentTimeMillis();
        if (now - lastSaveTime < SAVE_COOLDOWN_MS) return; // debounce rapid flickers
        lastSaveTime = now;

        lastSyncedStack = stackInSlot.copy();

        var liveCap = sp.getCapability(EarringCapabilityHandler.EARRING_CAP);
        if (liveCap != null) {
            liveCap.setStackInSlot(EARRING_SLOT_INDEX, stackInSlot);
            liveCap.setChanged();
            CatocraftMod.LOGGER.debug("[EarringMenu] [SERVER] Slot changed — live capability updated with {}", stackInSlot);
        } else {
            CatocraftMod.LOGGER.warn("[EarringMenu] [SERVER] No capability found when slot changed!");
        }

        // ✅ Sync + save only on real changes
        EarringSyncHelper.syncToClient(sp);
        net.chriskatze.catocraftmod.capability.EarringDataHandler.requestImmediateSave(sp);

        // 💬 Debug feedback to player (visible only to the one who changed it)
        sp.displayClientMessage(
                Component.literal("💾 Earring saved: ")
                        .append(stackInSlot.isEmpty()
                                ? Component.literal("empty").withStyle(ChatFormatting.DARK_GRAY)
                                : stackInSlot.getHoverName().copy().withStyle(ChatFormatting.GRAY)),
                true // replaces previous line
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    // 🔁 Shift-click transfer logic
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            int earringSlot = 0;
            int inventoryStart = 1;
            int inventoryEnd = inventoryStart + 36;

            if (index == earringSlot) {
                if (!moveItemStackTo(stackInSlot, inventoryStart, inventoryEnd, true))
                    return ItemStack.EMPTY;
            } else if (handler.isItemValid(earringSlot, stackInSlot)) {
                if (!moveItemStackTo(stackInSlot, earringSlot, earringSlot + 1, false))
                    return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return result;
    }

    // 🔁 When closing GUI
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer sp)) return;

        ItemStack slotStack = this.slots.get(EARRING_SLOT_INDEX).getItem().copy();

        PlayerEarringCapability handler = sp.getCapability(EarringCapabilityHandler.EARRING_CAP);
        if (handler != null) {
            handler.setStackInSlot(EARRING_SLOT_INDEX, slotStack);
            handler.setChanged();

            // ✅ NEW: dump the capability's serialized NBT for verification
            var tag = handler.serializeNBT(sp.registryAccess());
            CatocraftMod.LOGGER.debug("[EarringMenu] [VERIFY] Persisted handler state -> {}", tag);

            CatocraftMod.LOGGER.debug("[EarringMenu] [SERVER] Persisted into live capability: {}", slotStack);
        } else {
            CatocraftMod.LOGGER.warn("[EarringMenu] [SERVER] No earring capability found for {}", sp.getName().getString());
        }

        // ✅ Sync and save
        net.chriskatze.catocraftmod.network.EarringSyncHelper.syncToClient(sp);
        net.chriskatze.catocraftmod.capability.EarringDataHandler.requestImmediateSave(sp);

        sp.displayClientMessage(
                Component.literal("💾 Earring menu closed & saved").withStyle(ChatFormatting.DARK_GRAY),
                true
        );
    }
}