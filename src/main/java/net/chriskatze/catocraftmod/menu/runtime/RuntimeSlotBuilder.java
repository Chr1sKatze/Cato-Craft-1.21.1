package net.chriskatze.catocraftmod.menu.runtime;

import net.chriskatze.catocraftmod.creator.common.ProjectFileManager;
import net.chriskatze.catocraftmod.creator.menu.MenuGroupDefinition;
import net.chriskatze.catocraftmod.creator.menu.MenuLayout;
import net.chriskatze.catocraftmod.creator.menu.MenuLayoutSerializer;
import net.chriskatze.catocraftmod.creator.menu.MenuSlotDefinition;
import net.chriskatze.catocraftmod.creatorhub.CreatorType;
import net.chriskatze.catocraftmod.menu.layout.SlotType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 🧩 RuntimeSlotBuilder
 *
 * Builds live Slot instances from MenuSlotDefinitions.
 * Now supports dynamic group validation via MenuGroupDefinition rules.
 */
public class RuntimeSlotBuilder {

    public static List<Slot> buildSlots(
            List<MenuSlotDefinition> layoutSlots,
            Inventory playerInv,
            Container craftingInv,
            ResultContainer resultInv,
            Container miscInv,
            Player player
    ) {
        List<Slot> slots = new ArrayList<>();
        if (layoutSlots == null || layoutSlots.isEmpty()) return slots;

        // ─────────────────────── Load layout reference ───────────────────────
        MenuLayout layout = null;
        if (player != null) {
            String lastName = DynamicMenuStorage.getLastOpened(player);
            if (lastName != null && !lastName.isBlank()) {
                File layoutFile = ProjectFileManager.getFile(CreatorType.MENU, lastName).toFile();
                if (layoutFile.exists()) {
                    layout = MenuLayoutSerializer.load(layoutFile);
                }
            }
        }

        // ─────────────────────── Build each slot ───────────────────────
        for (MenuSlotDefinition def : layoutSlots) {
            final SlotType type = SlotType.fromString(def.getType()) != null
                    ? SlotType.fromString(def.getType())
                    : SlotType.INVENTORY;

            int x = def.getX();
            int y = def.getY();
            int index = slots.size(); // ensure unique container index

            Container target = miscInv;
            if (type.isPlayerLinked()) target = playerInv;
            else if (type == SlotType.CRAFTING) target = craftingInv;
            else if (type == SlotType.CRAFTING_RESULT) target = resultInv;

            MenuGroupDefinition group = (layout != null)
                    ? layout.findGroup(def.getGroup())
                    : null;

            Slot slot = new TypedSlot(type, target, index, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (stack == null || stack.isEmpty()) return false;

                    // 1️⃣ Group-based restrictions
                    if (group != null) {
                        // Tag rule
                        if (group.getValidItemTag() != null && !group.getValidItemTag().isBlank()) {
                            ResourceLocation tagLoc = ResourceLocation.tryParse(group.getValidItemTag());
                            if (tagLoc != null) {
                                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
                                boolean matches = stack.is(tagKey);
                                if (!matches && group.isStrict()) return false;
                            }
                        }

                        // Type rule (simple string check)
                        if (group.getValidItemType() != null && !group.getValidItemType().isBlank()) {
                            String expected = group.getValidItemType().toLowerCase();
                            String actual = stack.getItem().getClass().getSimpleName().toLowerCase();
                            boolean matches = actual.contains(expected);
                            if (!matches && group.isStrict()) return false;
                        }
                    }

                    // 2️⃣ Slot-level tag fallback
                    if (def.getTag() != null && !def.getTag().isBlank()) {
                        ResourceLocation tagLoc = ResourceLocation.tryParse(def.getTag());
                        if (tagLoc != null) {
                            TagKey<Item> slotTag = TagKey.create(Registries.ITEM, tagLoc);
                            if (!stack.is(slotTag)) return false;
                        }
                    }

                    // 3️⃣ Type-specific exceptions
                    if (type == SlotType.CRAFTING_RESULT || type == SlotType.INGREDIENT_RESULT)
                        return false;

                    return true;
                }
            };

            slots.add(slot);
        }

        return slots;
    }
}