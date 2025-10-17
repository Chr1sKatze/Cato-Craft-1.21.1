package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EarringCapabilityHandler;
import net.chriskatze.catocraftmod.capability.EarringDataHandler;
import net.chriskatze.catocraftmod.capability.PlayerEarringCapability;
import net.chriskatze.catocraftmod.network.EarringSyncHelper;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Optional;

public class EarringMenu extends AbstractContainerMenu {
    public static final int EARRING_SLOT_INDEX = 0;
    private static final int RESULT_SLOT_MENU_INDEX = 0;

    private final Player player;
    private final PlayerEarringCapability handler;

    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlotContainer = new ResultContainer();

    private int armorStart = -1, armorEnd = -1;
    private int offhandIndex = -1;
    private int earringIndex = -1;
    private int invStart = -1, invEnd = -1;
    private int hotbarStart = -1, hotbarEnd = -1;

    private ItemStack lastSyncedStack = ItemStack.EMPTY;
    private long lastSaveTime = 0L;
    private static final long SAVE_COOLDOWN_MS = 250;

    private boolean suppressBroadcast = false;

    public EarringMenu(int id, Inventory inv, Player player) {
        super(ModMenus.EARRING_MENU.get(), id);
        this.player = player;

        if (player.level().isClientSide) {
            this.handler = new PlayerEarringCapability();
            CatocraftMod.LOGGER.debug("[EarringMenu] (CLIENT) Using dummy capability for {}", player.getName().getString());
        } else {
            var cap = player.getCapability(EarringCapabilityHandler.EARRING_CAP);
            if (cap == null) {
                CatocraftMod.LOGGER.warn("[EarringMenu] (SERVER) No EARRING_CAP found for {}", player.getName().getString());
                this.handler = new PlayerEarringCapability();
            } else {
                this.handler = cap;
            }
            CatocraftMod.LOGGER.debug("[EarringMenu] (SERVER) Using persistent capability {} for {}", System.identityHashCode(handler), player.getName().getString());

            ItemStack saved = handler.getStackInSlot(EARRING_SLOT_INDEX);
            if (!saved.isEmpty()) {
                handler.setStackInSlot(EARRING_SLOT_INDEX, saved.copy());
                CatocraftMod.LOGGER.debug("[EarringMenu] (SERVER) Restored saved earring stack: {}", saved);
            }
        }

        // (1) Result + 2x2 input
        this.addSlot(new ResultSlot(inv.player, craftSlots, resultSlotContainer, 0, 154, 28));
        this.addSlot(new Slot(craftSlots, 0, 98, 18));
        this.addSlot(new Slot(craftSlots, 1, 116, 18));
        this.addSlot(new Slot(craftSlots, 2, 98, 36));
        this.addSlot(new Slot(craftSlots, 3, 116, 36));

        // (2) Armor + offhand
        addArmorAndOffhandSlots(inv);

        // (3) Earring slot
        earringIndex = this.slots.size();
        this.addSlot(new SlotItemHandler(handler, EARRING_SLOT_INDEX, 78, 16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.EARRINGS);
            }

            @Override
            public void setChanged() {
                // --- CLIENT: clear both the container and the actual result Slot immediately ---
                if (player.level().isClientSide) {
                    // Clear logical container
                    resultSlotContainer.setItem(0, ItemStack.EMPTY);
                    resultSlotContainer.setChanged();

                    // Clear visible slot
                    if (RESULT_SLOT_MENU_INDEX >= 0 && RESULT_SLOT_MENU_INDEX < EarringMenu.this.slots.size()) {
                        Slot resultSlot = EarringMenu.this.slots.get(RESULT_SLOT_MENU_INDEX);
                        resultSlot.set(ItemStack.EMPTY);
                        resultSlot.setChanged();
                    }

                    // 🔹 New: Force GUI to sync with this empty slot
                    if (Minecraft.getInstance().player != null &&
                            Minecraft.getInstance().player.containerMenu == EarringMenu.this) {
                        Minecraft.getInstance().player.containerMenu.broadcastChanges();
                    }

                    // 🔹 New: Manually trigger slot update for stubborn ghost stacks
                    EarringMenu.this.slotsChanged(resultSlotContainer);

                    // 🔹 Safety delayed clear (UI tick)
                    Minecraft.getInstance().execute(() -> {
                        resultSlotContainer.setItem(0, ItemStack.EMPTY);
                        resultSlotContainer.setChanged();
                        if (RESULT_SLOT_MENU_INDEX >= 0 && RESULT_SLOT_MENU_INDEX < EarringMenu.this.slots.size()) {
                            Slot resultSlot2 = EarringMenu.this.slots.get(RESULT_SLOT_MENU_INDEX);
                            resultSlot2.set(ItemStack.EMPTY);
                            resultSlot2.setChanged();
                        }
                        // Force another broadcast after client tick
                        if (Minecraft.getInstance().player != null &&
                                Minecraft.getInstance().player.containerMenu == EarringMenu.this) {
                            Minecraft.getInstance().player.containerMenu.broadcastChanges();
                        }
                    });
                    return;
                }

                // --- SERVER: normal syncing + broadcast suppression ---
                if (!(player instanceof ServerPlayer sp)) return;

                suppressBroadcast = true;
                this.container.setChanged();

                ItemStack stackInSlot = getItem().copy();
                if (ItemStack.isSameItemSameComponents(stackInSlot, lastSyncedStack)) return;

                long now = System.currentTimeMillis();
                if (now - lastSaveTime < SAVE_COOLDOWN_MS) return;
                lastSaveTime = now;
                lastSyncedStack = stackInSlot.copy();

                var liveCap = sp.getCapability(EarringCapabilityHandler.EARRING_CAP);
                if (liveCap != null) {
                    liveCap.setStackInSlot(EARRING_SLOT_INDEX, stackInSlot);
                    liveCap.clearDirty();
                    CatocraftMod.LOGGER.debug("[EarringMenu] Updated live capability with {}", stackInSlot);
                }

                EarringDataHandler.requestImmediateSave(sp);
            }
        });

        // (4) Player inv + hotbar
        invStart = this.slots.size();
        addPlayerInventory(inv);
        invEnd = this.slots.size();
        hotbarStart = this.slots.size();
        addPlayerHotbar(inv);
        hotbarEnd = this.slots.size();

        if (!player.level().isClientSide) {
            clearAndSyncResult();
            updateCraftingResult();
        }
    }

    private void clearAndSyncResult() {
        resultSlotContainer.setRecipeUsed(null);
        resultSlotContainer.setItem(0, ItemStack.EMPTY);
        resultSlotContainer.setChanged();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == craftSlots) {
            updateCraftingResult();
        }
    }

    private void updateCraftingResult() {
        if (!(player instanceof ServerPlayer sp)) return;
        Level level = sp.level();

        CraftingInput input = CraftingInput.of(craftSlots.getWidth(), craftSlots.getHeight(), craftSlots.getItems());
        Optional<RecipeHolder<CraftingRecipe>> recipeOpt =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);

        if (recipeOpt.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = recipeOpt.get();
            CraftingRecipe recipe = holder.value();

            if (recipe.matches(input, level)) {
                ItemStack result = recipe.assemble(input, level.registryAccess());
                resultSlotContainer.setRecipeUsed(holder);
                resultSlotContainer.setItem(0, result);
                resultSlotContainer.setChanged();
            } else {
                clearAndSyncResult();
            }
        } else {
            clearAndSyncResult();
        }

        broadcastChanges();
    }

    @Override
    public void broadcastChanges() {
        if (suppressBroadcast) {
            suppressBroadcast = false;
            return;
        }

        super.broadcastChanges();

        if (player.level().isClientSide) return;

        ItemStack resultStack = resultSlotContainer.getItem(0);
        if (!resultStack.isEmpty()) {
            ItemStack earringStack = slots.get(earringIndex).getItem();
            boolean invalidResult = resultStack.is(earringStack.getItem())
                    || !isCraftingGridValid();

            if (invalidResult) {
                clearAndSyncResult();
                CatocraftMod.LOGGER.debug("[EarringMenu] Cleared ghost crafting result {}", resultStack);
            }
        }
    }

    private boolean isCraftingGridValid() {
        if (!(player instanceof ServerPlayer sp)) return false;
        Level level = sp.level();
        var input = CraftingInput.of(craftSlots.getWidth(), craftSlots.getHeight(), craftSlots.getItems());
        return level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level)
                .isPresent();
    }

    private void addArmorAndOffhandSlots(Inventory inv) {
        int x = 8, y = 8;
        armorStart = this.slots.size();
        for (int i = 0; i < 4; i++) {
            final int armorInvIndex = 39 - i;
            final EquipmentSlot slotType = switch (i) {
                case 0 -> EquipmentSlot.HEAD;
                case 1 -> EquipmentSlot.CHEST;
                case 2 -> EquipmentSlot.LEGS;
                default -> EquipmentSlot.FEET;
            };
            this.addSlot(new Slot(inv, armorInvIndex, x, y + i * 18) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack stack) { return stack.canEquip(slotType, inv.player); }
            });
        }
        armorEnd = this.slots.size();
        offhandIndex = this.slots.size();
        this.addSlot(new Slot(inv, 40, 77, 62));
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

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, craftSlots);
        clearContainer(player, resultSlotContainer);

        if (player instanceof ServerPlayer sp) {
            var cap = sp.getCapability(EarringCapabilityHandler.EARRING_CAP);
            if (cap != null) {
                ItemStack slotStack = this.slots.get(earringIndex).getItem().copy();
                cap.setStackInSlot(EARRING_SLOT_INDEX, slotStack);
                cap.setChanged();

                EarringSyncHelper.syncToClient(sp);
                EarringDataHandler.requestImmediateSave(sp);

                CatocraftMod.LOGGER.debug("[EarringMenu] [SERVER] Persisted capability state on menu close -> {}", slotStack);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();
            if (!moveItemStackTo(stackInSlot, invStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}