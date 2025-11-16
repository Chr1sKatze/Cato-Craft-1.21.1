package net.chriskatze.catocraftmod.menu;

import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.chriskatze.catocraftmod.creator.menu.MenuSlotDefinition;
import net.chriskatze.catocraftmod.menu.layout.SlotType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds a runtime container from a JSON-driven MenuLayout.
 * Every slot is placed and grouped dynamically according to layout data.
 */
public final class DynamicMenuFactory {

    private DynamicMenuFactory() {}

    public static AbstractContainerMenu build(int id, Inventory playerInv, Player player, MenuLayout layout) {
        BlockPos pos = BlockPos.ZERO; // reserved for future BE link

        return new AbstractContainerMenu(MenuType.GENERIC_9x3, id) {

            // Core containers
            private final TransientCraftingContainer craftMatrix = new TransientCraftingContainer(this, 3, 3);
            private final ResultContainer craftResult = new ResultContainer();

            // Slot group registry (groupName → slots)
            private final Map<String, List<Slot>> slotGroups = new HashMap<>();

            // Quick access groups for vanilla-like routing
            private final List<Slot> playerSlots = new ArrayList<>();
            private final List<Slot> hotbarSlots = new ArrayList<>();

            @Override
            public boolean stillValid(Player p) {
                return true;
            }

            {
                // ────────────── Build all slots dynamically ──────────────
                for (MenuSlotDefinition def : layout.getSlots()) {
                    SlotType type = SlotType.fromString(def.getType());
                    if (type == null) continue;

                    final int x = def.getX();
                    final int y = def.getY();
                    final String group = inferGroup(def, type);

                    Slot s = switch (type) {
                        // ───── Player Inventory / Hotbar ─────
                        case INVENTORY -> addPlayerInventorySlots(playerInv, x, y, group);
                        case HOTBAR -> addHotbarSlots(playerInv, x, y, group);

                        // ───── Vanilla Gear / Offhand ─────
                        case ARMOR -> addArmorSlots(playerInv, x, y, group);
                        case OFF_HAND -> addOffhandSlot(playerInv, x, y, group);

                        // ───── Crafting System ─────
                        case CRAFTING -> addCraftingGrid(x, y, group);
                        case CRAFTING_RESULT -> addResultSlot(player, x, y, group);

                        // ───── Custom Tag / Ingredient Slots ─────
                        case INGREDIENT -> addIngredientSlot(def, x, y, group);
                        case INGREDIENT_RESULT -> addIngredientResultSlot(x, y, group);
                        case JEWELLERY -> addJewellerySlot(playerInv, x, y, group);

                        // ───── Default / unknown type ─────
                        default -> addGenericSlot(playerInv, x, y, group);
                    };

                    // Some slot-adding helpers return null (e.g., multi-slot types handled internally)
                    if (s != null) {
                        this.addSlot(s);
                        slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                    }
                }

                // Debug log
                net.chriskatze.catocraftmod.CatocraftMod.LOGGER.info(
                        "[DynamicMenuFactory] Built groups: {}", slotGroups.keySet());
            }

            private String inferGroup(MenuSlotDefinition def, SlotType type) {
                if (def.getGroup() != null && !def.getGroup().isBlank()) {
                    return def.getGroup();
                }

                return switch (type) {
                    case INVENTORY -> "inventory";
                    case HOTBAR -> "hotbar";
                    case ARMOR -> "armor";
                    case OFF_HAND -> "offhand";
                    case CRAFTING -> "crafting";
                    case CRAFTING_RESULT, INGREDIENT_RESULT -> "output";
                    case INGREDIENT -> "ingredients";
                    case JEWELLERY -> "accessory";
                    default -> "default";
                };
            }

            // ─────────────────────────────────────────────────────────────
            // Slot construction helpers
            // ─────────────────────────────────────────────────────────────

            private Slot addPlayerInventorySlots(Inventory inv, int x, int y, String group) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        Slot s = new Slot(inv, 9 + col + row * 9, x + col * 18, y + row * 18);
                        this.addSlot(s);
                        playerSlots.add(s);
                        slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                    }
                }
                return null;
            }

            private Slot addHotbarSlots(Inventory inv, int x, int y, String group) {
                for (int col = 0; col < 9; col++) {
                    Slot s = new Slot(inv, col, x + col * 18, y);
                    this.addSlot(s);
                    hotbarSlots.add(s);
                    slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                }
                return null;
            }

            private Slot addArmorSlots(Inventory inv, int x, int y, String group) {
                for (int i = 0; i < 4; i++) {
                    Slot s = new Slot(inv, 36 + i, x, y + i * 18) {
                        @Override public int getMaxStackSize() { return 1; }
                    };
                    this.addSlot(s);
                    slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                }
                return null;
            }

            private Slot addOffhandSlot(Inventory inv, int x, int y, String group) {
                Slot s = new Slot(inv, 40, x, y) {
                    @Override public int getMaxStackSize() { return 1; }
                };
                this.addSlot(s);
                slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                return null;
            }

            private Slot addCraftingGrid(int x, int y, String group) {
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        Slot s = new Slot(craftMatrix, col + row * 3, x + col * 18, y + row * 18);
                        this.addSlot(s);
                        slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                    }
                }
                return null;
            }

            private Slot addResultSlot(Player player, int x, int y, String group) {
                Slot s = new ResultSlot(player, craftMatrix, craftResult, 0, x, y);
                this.addSlot(s);
                slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                return s;
            }

            private Slot addIngredientSlot(MenuSlotDefinition def, int x, int y, String group) {
                int next = nextFreeCraftIndex();
                TagKey<Item> tag = getItemTag(def.getTag());
                Slot s = new Slot(craftMatrix, next, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return tag == null || stack.is(tag);
                    }
                };
                this.addSlot(s);
                slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                return s;
            }

            private Slot addIngredientResultSlot(int x, int y, String group) {
                Slot s = new Slot(craftResult, 0, x, y) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                };
                this.addSlot(s);
                slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                return s;
            }

            private Slot addJewellerySlot(Inventory inv, int x, int y, String group) {
                TagKey<Item> tag = getItemTag("catocraftmod:jewellery_items");
                Slot s = new Slot(inv, findFirstEmptyPlayerIndex(), x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return tag != null && stack.is(tag);
                    }
                    @Override public int getMaxStackSize() { return 1; }
                };
                this.addSlot(s);
                slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                return s;
            }

            private Slot addGenericSlot(Inventory inv, int x, int y, String group) {
                Slot s = new Slot(inv, findFirstEmptyPlayerIndex(), x, y);
                this.addSlot(s);
                slotGroups.computeIfAbsent(group, g -> new ArrayList<>()).add(s);
                return s;
            }

            // ─────────────────────────────────────────────────────────────
            // Shift-click transfer logic (group-driven, vanilla aware)
            // ─────────────────────────────────────────────────────────────
            @Override
            public ItemStack quickMoveStack(Player p, int index) {
                Slot slot = this.slots.get(index);
                if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

                ItemStack stack = slot.getItem();
                ItemStack copy = stack.copy();

                String fromGroup = getGroupOfSlot(slot);

                // Prefer vanilla-like routing
                if (fromGroup.equalsIgnoreCase("output")) {
                    moveToGroup(stack, playerSlots);
                    moveToGroup(stack, hotbarSlots);
                } else if (fromGroup.equalsIgnoreCase("inventory") || fromGroup.equalsIgnoreCase("hotbar")) {
                    moveToGroup(stack, slotGroups.getOrDefault("output", List.of()));
                } else {
                    // Generic fallback: move to any other group
                    for (var entry : slotGroups.entrySet()) {
                        String groupName = entry.getKey();
                        if (groupName.equals(fromGroup)) continue;
                        if (moveToGroup(stack, entry.getValue())) break;
                    }
                }

                if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
                else slot.setChanged();

                slot.onTake(p, stack);
                return copy;
            }

            private boolean moveToGroup(ItemStack stack, List<Slot> targets) {
                if (targets == null || targets.isEmpty()) return false;
                int start = targets.get(0).index;
                int end = targets.get(targets.size() - 1).index + 1;
                return this.moveItemStackTo(stack, start, end, false);
            }

            private String getGroupOfSlot(Slot slot) {
                for (var entry : slotGroups.entrySet()) {
                    if (entry.getValue().contains(slot)) return entry.getKey();
                }
                return "default";
            }

            // ─────────────────────────────────────────────────────────────
            // Crafting & cleanup
            // ─────────────────────────────────────────────────────────────
            @Override
            public void slotsChanged(Container container) {
                super.slotsChanged(container);
                if (container == craftMatrix) updateCraftingResult();
            }

            private void updateCraftingResult() {
                if (player.level().isClientSide) return;

                var input = net.minecraft.world.item.crafting.CraftingInput.of(
                        craftMatrix.getWidth(),
                        craftMatrix.getHeight(),
                        craftMatrix.getItems()
                );

                player.level().getRecipeManager().getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                        input,
                        player.level()
                ).ifPresentOrElse(holder -> {
                    var recipe = holder.value();
                    ItemStack result = recipe.assemble(input, player.level().registryAccess());
                    craftResult.setItem(0, result);
                }, () -> craftResult.setItem(0, ItemStack.EMPTY));
            }

            @Override
            public void removed(Player p) {
                super.removed(p);
                this.clearContainer(p, craftMatrix);
            }

            // ─────────────────────────────────────────────────────────────
            // Small helpers
            // ─────────────────────────────────────────────────────────────
            private int nextFreeCraftIndex() {
                for (int i = 0; i < craftMatrix.getContainerSize(); i++) {
                    if (craftMatrix.getItem(i).isEmpty()) return i;
                }
                return 0;
            }

            private int findFirstEmptyPlayerIndex() {
                for (int i = 0; i < playerInv.getContainerSize(); i++) {
                    if (playerInv.getItem(i).isEmpty()) return i;
                }
                return 0;
            }

            private TagKey<Item> getItemTag(String fullName) {
                if (fullName == null || fullName.isBlank()) return null;
                ResourceLocation rl = ResourceLocation.tryParse(fullName);
                if (rl == null) return null;
                return TagKey.create(Registries.ITEM, rl);
            }
        };
    }
}