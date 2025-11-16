package net.chriskatze.catocraftmod.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

public class MenuCreatorMenu extends AbstractContainerMenu {

    private final Player player;
    private final CraftingContainer craftMatrix;
    private final ResultContainer craftResult;

    public MenuCreatorMenu(int id, Inventory playerInv) {
        super(ModMenus.MENU_CREATOR.get(), id); // adjust registry reference
        this.player = playerInv.player;
        this.craftMatrix = new TransientCraftingContainer(this, 3, 3);
        this.craftResult = new ResultContainer();

        // Result slot (custom subclass)
        this.addSlot(new ResultSlotLike(player, craftMatrix, craftResult, 0, 124, 35));

        // 3x3 crafting grid
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(craftMatrix, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Simple fallback: disable shift-click transfer for now
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == craftMatrix) updateCraftingResult();
    }

    private void updateCraftingResult() {
        if (player.level().isClientSide) return;

        var input = CraftingInput.of(
                craftMatrix.getWidth(),
                craftMatrix.getHeight(),
                craftMatrix.getItems()
        );

        player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .ifPresentOrElse(holder -> {
                    var recipe = holder.value();
                    ItemStack result = recipe.assemble(input, player.level().registryAccess());
                    craftResult.setItem(0, result);
                }, () -> craftResult.setItem(0, ItemStack.EMPTY));
    }

    /** Custom result slot (vanilla behavior clone) */
    private static class ResultSlotLike extends Slot {
        private final Player player;
        private final CraftingContainer craftMatrix;

        public ResultSlotLike(Player player, CraftingContainer matrix, Container result, int index, int x, int y) {
            super(result, index, x, y);
            this.player = player;
            this.craftMatrix = matrix;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            stack.onCraftedBy(player.level(), player, stack.getCount());

            player.level().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(
                            craftMatrix.getWidth(),
                            craftMatrix.getHeight(),
                            craftMatrix.getItems()
                    ), player.level())
                    .ifPresent(recipe -> player.triggerRecipeCrafted(recipe, craftMatrix.getItems()));

            // Consume ingredients
            for (int i = 0; i < craftMatrix.getContainerSize(); ++i) {
                ItemStack slotStack = craftMatrix.getItem(i);
                if (!slotStack.isEmpty()) {
                    craftMatrix.removeItem(i, 1);
                    if (slotStack.getItem().hasCraftingRemainingItem()) {
                        ItemStack remainder = slotStack.getItem().getCraftingRemainingItem(slotStack);
                        if (!remainder.isEmpty()) craftMatrix.setItem(i, remainder);
                    }
                }
            }

            super.onTake(player, stack);
        }
    }
}