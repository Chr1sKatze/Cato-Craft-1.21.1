package net.chriskatze.catocraftmod.menu.runtime;

import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class MenuCreatorTestMenu extends AbstractContainerMenu {

    private final SimpleCraftGrid craftInv;
    private final ResultContainer resultInv = new ResultContainer();
    private final Level level;
    private final Inventory playerInv;

    // ✅ Jewellery slot storage (class field!)
    private final NonNullList<ItemStack> jewelleryInv = NonNullList.withSize(1, ItemStack.EMPTY);

    // ✅ Constructor with MenuType (NeoForge requirement)
    public MenuCreatorTestMenu(MenuType<?> type, int id, Inventory playerInv) {
        super(type, id);
        this.level = playerInv.player.level();
        this.playerInv = playerInv;

        // 2×2 crafting grid backing
        this.craftInv = new SimpleCraftGrid(this, 2, 2);

        // --- Crafting grid (0–3) ---
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                this.addSlot(new Slot(craftInv, r * 2 + c, 44 + c * 18, 17 + r * 18));
            }
        }

        // --- Result slot (4) ---
        this.addSlot(new Slot(resultInv, 0, 106, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                // Consume ingredients like vanilla
                for (int i = 0; i < craftInv.getContainerSize(); ++i) {
                    ItemStack in = craftInv.getItem(i);
                    if (!in.isEmpty()) {
                        in.shrink(1);
                        craftInv.setItem(i, in);
                    }
                }
                updateCraftingResult();
                super.onTake(player, stack);
            }
        });

        // --- Jewellery slot (5) --- (backed by jewelleryInv above)
        this.addSlot(new Slot(new Container() {
            @Override public int getContainerSize() { return 1; }
            @Override public boolean isEmpty() { return jewelleryInv.get(0).isEmpty(); }
            @Override public ItemStack getItem(int i) { return jewelleryInv.get(0); }
            @Override public ItemStack removeItem(int i, int count) {
                ItemStack stack = net.minecraft.world.ContainerHelper.removeItem(jewelleryInv, 0, count);
                if (!stack.isEmpty()) setChanged();
                return stack;
            }
            @Override public ItemStack removeItemNoUpdate(int i) {
                ItemStack stack = jewelleryInv.get(0);
                jewelleryInv.set(0, ItemStack.EMPTY);
                return stack;
            }
            @Override public void setItem(int i, ItemStack stack) {
                jewelleryInv.set(0, stack);
                setChanged();
            }
            @Override public void setChanged() { slotsChanged(this); }
            @Override public boolean stillValid(Player player) { return true; }
            @Override public void clearContent() { jewelleryInv.set(0, ItemStack.EMPTY); }
        }, 0, 8, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Only allow jewellery-tagged items
                return stack.is(ModTags.EARRINGS)
                        || stack.is(ModTags.RINGS)
                        || stack.is(ModTags.NECKLACES)
                        || stack.is(ModTags.SOUL_STONES);
            }
        });

        // --- Player inventory (6–32) ---
        int startY = 84;
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 9; ++c)
                this.addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, startY + r * 18));
        for (int c = 0; c < 9; ++c)
            this.addSlot(new Slot(playerInv, c, 8 + c * 18, startY + 58));

        updateCraftingResult();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == craftInv) updateCraftingResult();
    }

    private void updateCraftingResult() {
        if (level.isClientSide) return;

        CraftingInput input = CraftingInput.of(craftInv.getWidth(), craftInv.getHeight(), craftInv.getItems());
        Optional<RecipeHolder<CraftingRecipe>> opt =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);

        if (opt.isPresent()) {
            CraftingRecipe recipe = opt.get().value();
            ItemStack result = recipe.assemble(input, level.registryAccess());
            resultInv.setItem(0, result);
        } else {
            resultInv.setItem(0, ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    // ─────────────────────────────────────────────
    // Shift-click logic
    // ─────────────────────────────────────────────
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int resultIndex = 4;
        int jewelleryIndex = 5;
        int playerStart = 6;
        int playerEnd = this.slots.size();

        if (index == resultIndex) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);

        } else if (index >= playerStart) {
            // From player inv → prefer jewellery slot if valid
            if (stack.is(ModTags.EARRINGS) || stack.is(ModTags.RINGS)
                    || stack.is(ModTags.NECKLACES) || stack.is(ModTags.SOUL_STONES)) {
                if (!this.moveItemStackTo(stack, jewelleryIndex, jewelleryIndex + 1, false))
                    return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, jewelleryIndex, false)) {
                return ItemStack.EMPTY;
            }

        } else {
            // From crafting/jewellery → player inv
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    // ───────────── Helper concrete CraftingContainer ─────────────
    private static class SimpleCraftGrid implements net.minecraft.world.inventory.CraftingContainer {
        private final MenuCreatorTestMenu parent;
        private final int width, height;
        private final NonNullList<ItemStack> items = NonNullList.create();

        public SimpleCraftGrid(MenuCreatorTestMenu parent, int width, int height) {
            this.parent = parent;
            this.width = width;
            this.height = height;
            for (int i = 0; i < width * height; i++) this.items.add(ItemStack.EMPTY);
        }

        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }
        @Override public java.util.List<ItemStack> getItems() { return items; }
        @Override public int getContainerSize() { return items.size(); }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) if (!stack.isEmpty()) return false;
            return true;
        }

        @Override public ItemStack getItem(int index) { return items.get(index); }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack itemstack = net.minecraft.world.ContainerHelper.removeItem(items, index, count);
            if (!itemstack.isEmpty()) setChanged();
            return itemstack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return net.minecraft.world.ContainerHelper.takeItem(items, index);
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            items.set(index, stack);
            if (!stack.isEmpty() && stack.getCount() > getMaxStackSize())
                stack.setCount(getMaxStackSize());
            setChanged();
        }

        @Override public void setChanged() { parent.slotsChanged(this); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { items.clear(); }

        @Override
        public void fillStackedContents(StackedContents helper) {
            for (ItemStack stack : items) helper.accountStack(stack);
        }
    }
}