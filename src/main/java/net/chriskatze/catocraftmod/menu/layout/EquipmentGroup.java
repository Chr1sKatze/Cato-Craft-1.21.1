package net.chriskatze.catocraftmod.menu.layout;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Data-driven registry for all custom equipment slot groups.
 *
 * Each {@link EquipmentGroup} represents a logical collection of equip slots
 * (e.g. earrings, rings, necklaces, soulstones) defined via JSON in
 * {@link SlotLayoutDefinition}.
 *
 * This replaces the old enum-based system with a **dynamic registry** that:
 *  • Loads groups at runtime from datapacks or static bootstrap code.
 *  • Supports reloads without restarting the game (/reload).
 *  • Preserves registration order for consistent UI placement.
 *
 * Responsibilities:
 *  - Acts as a global key for linking slot layouts, capabilities, and items.
 *  - Provides lookup utilities for items and groups.
 *  - Supports optional item tag association for automatic group detection.
 *
 * Example:
 *  ```json
 *  {
 *    "group": "earrings",
 *    "slots": 2,
 *    "tags": ["jewellery", "cosmetic"],
 *    "valid_items": ["#catocraftmod:earrings"]
 *  }
 *  ```
 *
 * ⚙️ Reloading:
 * Calling {@link #resetBaseGroups()} restores default base groups and clears
 * dynamically loaded ones — this is invoked automatically during data reloads.
 */
public final class EquipmentGroup {

    // Internal registry of all currently active groups
    private static final Map<String, EquipmentGroup> REGISTRY = new LinkedHashMap<>();

    private final String key;
    private final TagKey<Item> tag;
    private final ResourceLocation groupId;

    private EquipmentGroup(String key, TagKey<Item> tag) {
        this.key = key;
        this.tag = tag;
        this.groupId = CatocraftMod.id("equipment/" + key);
    }

    // ────────────────────────────────────────────────
    // Registration
    // ────────────────────────────────────────────────

    /** Registers a new equipment group dynamically or returns existing. */
    public static EquipmentGroup register(String key, TagKey<Item> tag) {
        return REGISTRY.computeIfAbsent(key.toLowerCase(Locale.ROOT),
                k -> new EquipmentGroup(k, tag));
    }

    /** Registers a group without a tag (e.g. from JSON layout). */
    public static EquipmentGroup register(String key) {
        return register(key, null);
    }

    /** Returns all registered groups in definition order. */
    public static Collection<EquipmentGroup> all() {
        return REGISTRY.values();
    }

    /** Clears all groups and re-registers the built-in base groups. */
    public static void resetBaseGroups() {
        REGISTRY.clear();
        register("earrings", ModTags.Items.EARRINGS);
        register("rings", ModTags.Items.RINGS);
        register("necklaces", ModTags.Items.NECKLACES);
        register("soulstones", ModTags.Items.SOUL_STONES);
    }

    // ────────────────────────────────────────────────
    // Accessors
    // ────────────────────────────────────────────────

    public String getKey() {
        return key;
    }

    public TagKey<Item> getTag() {
        return tag;
    }

    public ResourceLocation getGroupId() {
        return groupId;
    }

    // ────────────────────────────────────────────────
    // Lookup Helpers
    // ────────────────────────────────────────────────

    /** Finds a group by its string key (e.g. "earrings"). */
    public static EquipmentGroup fromKey(String key) {
        if (key == null) return null;
        return REGISTRY.get(key.toLowerCase(Locale.ROOT));
    }

    /** Finds a group by its ResourceLocation (e.g. catocraftmod:equipment/earrings). */
    public static EquipmentGroup fromId(ResourceLocation id) {
        if (id == null) return null;
        for (EquipmentGroup g : REGISTRY.values()) {
            if (g.groupId.equals(id) || id.getPath().endsWith("/" + g.key)) {
                return g;
            }
        }
        return null;
    }

    /** Attempts to find a group based on the tag of a given item stack. */
    public static EquipmentGroup fromStack(ItemStack stack) {
        for (EquipmentGroup group : REGISTRY.values()) {
            if (group.tag != null && stack.is(group.tag)) return group;
        }
        return null;
    }

    @Override
    public String toString() {
        return key;
    }
    // ────────────────────────────────────────────────
    // Debug Helpers
    // ────────────────────────────────────────────────

    /**
     * Logs all currently registered equipment groups to the console.
     * Useful after reloads or datapack changes to verify the registry state.
     */
    public static void logRegistry() {
        if (REGISTRY.isEmpty()) {
            CatocraftMod.LOGGER.info("[EquipmentGroup] No groups registered yet.");
            return;
        }

        CatocraftMod.LOGGER.info("──────────────────────────────────────────────");
        CatocraftMod.LOGGER.info("[EquipmentGroup] Active groups: {} total", REGISTRY.size());

        for (EquipmentGroup group : REGISTRY.values()) {
            CatocraftMod.LOGGER.info(" • {}  (id: {}, tag: {})",
                    group.getKey(),
                    group.getGroupId(),
                    group.getTag() != null ? group.getTag().location() : "<none>");
        }

        CatocraftMod.LOGGER.info("──────────────────────────────────────────────");
    }
}