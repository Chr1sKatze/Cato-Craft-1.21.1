package net.chriskatze.catocraftmod.capability;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.item.EquippableItemBase;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.chriskatze.catocraftmod.menu.layout.SlotEquipValidator;
import net.chriskatze.catocraftmod.menu.layout.SlotLayoutDefinition;
import net.chriskatze.catocraftmod.menu.layout.SlotLayoutLoader;
import net.chriskatze.catocraftmod.network.MenuSyncHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

/**
 * ✅ Unified capability for managing all custom player equipment groups.
 *
 * This system is now fully **data-driven**, replacing the old enum-based layout.
 * Equipment groups (earrings, rings, soulstones, etc.) are defined via JSON
 * in {@link SlotLayoutDefinition}, registered at runtime in {@link EquipmentGroup},
 * and dynamically synchronized per player.
 *
 * Responsibilities:
 *  - Maintain a {@link ItemStackHandler} for each {@link EquipmentGroup}.
 *  - Apply/Remove attribute modifiers from equipped items.
 *  - Handle slot dependency and conflict validation via {@link SlotEquipValidator}.
 *  - Automatically sync state with clients and preserve across reloads.
 *
 * ⚙️ Supports dynamic reloads:
 * When slot layout JSONs are changed and `/reload` is executed,
 * all groups are rebuilt and valid equipped items are restored.
 *
 * 🔸 Note:
 * `EquipmentGroup` is no longer an enum — all collections use
 * `LinkedHashMap` / `LinkedHashSet` to preserve order and support
 * runtime addition/removal of groups.
 */
public class PlayerEquipmentCapability {

    // Dynamic registry-friendly structure; keeps insertion order for UI consistency.
    private final Map<EquipmentGroup, ItemStackHandler> groupInventories = new LinkedHashMap<>();

    private boolean dirty = false;
    private boolean suppressSync = false;
    private ServerPlayer owner;
    private int pendingHealthFixTicks = 0;
    private int ticksSinceLastSync = 0;
    private static final int SYNC_INTERVAL_TICKS = 20; // every 1s if dirty
    private boolean suppressAttributeReapply = false;

    public void setOwner(ServerPlayer player) { this.owner = player; }
    public ServerPlayer getOwner() { return owner; }

    // NOTE: EquipmentGroup is now a dynamic registry class, not an enum.
    // Therefore, we use LinkedHashMap/LinkedHashSet instead of EnumMap/EnumSet.
    // This allows hot-reloading and data-driven expansion of equipment groups.

    // ────────────────────────────────────────────────
    // Global log cache for attribute/modifier warnings
    // Prevents repeating the same warnings every tick
    // ────────────────────────────────────────────────
    private static final Set<String> LOGGED_WARNINGS = Collections.synchronizedSet(new HashSet<>());

    // ────────────────────────────────────────────────
    // Initialization
    // ────────────────────────────────────────────────

    /**
     * Ensures all currently registered equipment groups are initialized,
     * and removes any groups that no longer exist (from deleted or renamed layouts).
     * Now includes per-player context in logs for better debugging.
     */
    public void initializeGroupsIfMissing() {
        int created = 0;
        int removed = 0;
        int totalSlots = 0;

        Set<EquipmentGroup> validGroups = new HashSet<>(EquipmentGroup.all());

        // 🗑️ Step 1 — Remove orphaned groups (no longer in registry)
        Iterator<Map.Entry<EquipmentGroup, ItemStackHandler>> it = groupInventories.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<EquipmentGroup, ItemStackHandler> entry = it.next();
            if (!validGroups.contains(entry.getKey())) {
                it.remove();
                removed++;
            }
        }

        // 🆕 Step 2 — Add missing groups
        for (EquipmentGroup group : validGroups) {
            if (groupInventories.containsKey(group)) continue;

            SlotLayoutDefinition def = SlotLayoutLoader.getDefinition(group.getGroupId());
            int slotCount = 1;

            if (def != null) {
                try {
                    var layout = def.toSlotLayout();
                    if (layout.isGridMode() && layout.cells() != null && !layout.cells().isEmpty()) {
                        slotCount = layout.cells().size();
                    } else if (layout.wrapAfter() > 0) {
                        slotCount = layout.wrapAfter();
                    }
                } catch (Exception e) {
                    CatocraftMod.LOGGER.warn("[EquipmentCap] Invalid layout for group {}: {}", group.getKey(), e.toString());
                }
            }

            ItemStackHandler handler = new ItemStackHandler(slotCount) {
                @Override
                protected void onContentsChanged(int slot) {
                    super.onContentsChanged(slot);
                    handleContentsChanged(group);
                }
            };

            groupInventories.put(group, handler);
            created++;
            totalSlots += slotCount;
        }

        // 👤 Optional: add player context to log if we have an owner
        String playerName = (owner != null)
                ? owner.getGameProfile().getName()
                : "<no-owner>";

        CatocraftMod.LOGGER.info(
                "[EquipmentCap] Sync complete for {} → {} new groups, {} removed, {} total active ({} slots).",
                playerName,
                created,
                removed,
                groupInventories.size(),
                totalSlots
        );
    }

    // ────────────────────────────────────────────────
    // Accessors
    // ────────────────────────────────────────────────

    public ItemStack getItem(EquipmentGroup group) {
        var inv = groupInventories.get(group);
        return inv != null ? inv.getStackInSlot(0) : ItemStack.EMPTY;
    }

    public Set<EquipmentGroup> getEquippedGroups() {
        Set<EquipmentGroup> equipped = new LinkedHashSet<>();
        for (var entry : groupInventories.entrySet()) {
            var handler = entry.getValue();
            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    equipped.add(entry.getKey());
                    break;
                }
            }
        }
        return equipped;
    }

    // ────────────────────────────────────────────────
    // Equip / Unequip
    // ────────────────────────────────────────────────

    public void setItem(EquipmentGroup group, ItemStack stack) {
        var inv = groupInventories.get(group);
        if (inv == null || owner == null) return;

        Set<ResourceLocation> equippedGroupIds = new HashSet<>();
        getEquippedGroups().forEach(g -> equippedGroupIds.add(g.getGroupId()));

        ResourceLocation groupId = group.getGroupId();

        // ───────────────────────────────────────────────
        // Unequip validation
        // ───────────────────────────────────────────────
        if (stack.isEmpty()) {
            // Instead of blocking, automatically remove dependents
            autoUnequipDependents(group);
        }

        // ───────────────────────────────────────────────
        // Equip validation
        // ───────────────────────────────────────────────
        boolean canEquip = SlotEquipValidator.canEquip(owner, group, getEquippedGroups());
        if (!canEquip) {
            owner.getInventory().placeItemBackInInventory(stack);
            return;
        }

        boolean validItem = SlotEquipValidator.canEquipItem(owner, group, stack);
        if (!validItem) {
            owner.getInventory().placeItemBackInInventory(stack);
            return;
        }

        // ───────────────────────────────────────────────
        // Core apply (with suppression guard)
        // ───────────────────────────────────────────────
        suppressAttributeReapply = true;
        inv.setStackInSlot(0, stack);

        // ───────────────────────────────────────────────
        // Linked slot mirroring
        // ───────────────────────────────────────────────
        SlotLayoutDefinition def = SlotLayoutLoader.getDefinition(groupId);
        if (def != null && !def.linked_with().isEmpty()) {
            for (String linkedKey : def.linked_with()) {
                EquipmentGroup linkedGroup = EquipmentGroup.fromKey(linkedKey);
                if (linkedGroup == null || linkedGroup == group) continue;

                var linkedInv = groupInventories.get(linkedGroup);
                if (linkedInv == null || linkedInv.getSlots() == 0) continue;

                ItemStack linkedStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
                linkedInv.setStackInSlot(0, linkedStack);

                CatocraftMod.LOGGER.debug("[EquipmentCap] Synced linked group '{}' with '{}'.",
                        linkedKey, group.getKey());
            }
        }
        suppressAttributeReapply = false;

        // Reapply modifiers after any linked update
        if (owner != null && !owner.level().isClientSide && !suppressAttributeReapply) {
            applyAllAttributes();
        }
    }

    /**
     * Automatically unequips all groups that depend on the given group.
     * Called when a required base item is unequipped.
     */
    private void autoUnequipDependents(EquipmentGroup baseGroup) {
        ResourceLocation baseId = baseGroup.getGroupId();

        for (var entry : groupInventories.entrySet()) {
            EquipmentGroup otherGroup = entry.getKey();
            if (otherGroup == baseGroup) continue;

            SlotLayoutDefinition def = SlotLayoutLoader.getDefinition(otherGroup.getGroupId());
            if (def == null || def.requires().isEmpty()) continue;

            boolean dependsOnBase = def.requires().stream().anyMatch(req ->
                    baseId.getPath().endsWith("/" + req) || baseId.getPath().equals(req)
            );

            if (dependsOnBase) {
                var handler = entry.getValue();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack old = handler.getStackInSlot(i);
                    if (!old.isEmpty()) {
                        handler.setStackInSlot(i, ItemStack.EMPTY);
                        owner.getInventory().placeItemBackInInventory(old);
                        CatocraftMod.LOGGER.debug("[EquipmentCap] Auto-unequipped dependent '{}' because '{}' was removed.",
                                otherGroup.getKey(), baseGroup.getKey());
                    }
                }

                // 🔁 Recursively unequip deeper dependencies
                autoUnequipDependents(otherGroup);
            }
        }
    }

    public Map<EquipmentGroup, ItemStackHandler> getAllGroups() {
        return groupInventories;
    }

    // ────────────────────────────────────────────────
    // Serialization
    // ────────────────────────────────────────────────

    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag root = new CompoundTag();
        ListTag groupsList = new ListTag();

        for (var entry : groupInventories.entrySet()) {
            EquipmentGroup group = entry.getKey();
            ItemStackHandler inv = entry.getValue();

            CompoundTag groupTag = new CompoundTag();
            groupTag.putString("GroupKey", group.getKey());
            groupTag.put("Items", inv.serializeNBT(lookup));
            groupsList.add(groupTag);
        }

        root.put("Groups", groupsList);
        return root;
    }

    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider lookup, CompoundTag tag) {
        groupInventories.clear();

        var groupsList = tag.getList("Groups", 10);
        for (int i = 0; i < groupsList.size(); i++) {
            CompoundTag groupTag = groupsList.getCompound(i);
            String key = groupTag.getString("GroupKey");
            EquipmentGroup group = EquipmentGroup.fromKey(key);
            if (group == null) continue;

            SlotLayoutDefinition def = SlotLayoutLoader.getDefinition(group.getGroupId());
            int slotCount = def != null && def.toSlotLayout().isGridMode()
                    ? def.toSlotLayout().cells().size()
                    : 1;

            ItemStackHandler inv = new ItemStackHandler(slotCount) {
                @Override
                protected void onContentsChanged(int slot) {
                    handleContentsChanged(group);
                }
            };

            inv.deserializeNBT(lookup, groupTag.getCompound("Items"));
            groupInventories.put(group, inv);
        }

        CatocraftMod.LOGGER.debug("[EquipmentCap] Deserialized {} equipment groups.", groupInventories.size());
    }

    // ────────────────────────────────────────────────
    // Sync + Attributes
    // ────────────────────────────────────────────────

    private void handleContentsChanged(EquipmentGroup group) {
        if (suppressSync) return;
        dirty = true;
        ticksSinceLastSync = 0;

        if (owner != null && !owner.level().isClientSide) {
            applyAllAttributes();
        }

        CatocraftMod.LOGGER.debug("[EquipmentCap] Group {} changed, marked for sync.", group.getKey());
    }

    public boolean shouldSyncToClient() {
        if (suppressSync || owner == null || owner.level().isClientSide) return false;

        boolean shouldSync = dirty || ticksSinceLastSync >= SYNC_INTERVAL_TICKS;
        if (shouldSync) {
            dirty = false;
            ticksSinceLastSync = 0;
        } else {
            ticksSinceLastSync++;
        }

        return shouldSync;
    }

    /** Remove old modifiers and reapply from all equipped items safely (persistent log suppression). */
    public void applyAllAttributes() {
        if (owner == null || owner.level().isClientSide) return;

        try {
            // 🔹 1. Remove all old modifiers from this mod namespace
            for (AttributeInstance attr : owner.getAttributes().getSyncableAttributes()) {
                attr.getModifiers().stream()
                        .filter(mod -> mod.id().getNamespace().equals(CatocraftMod.MOD_ID))
                        .toList()
                        .forEach(mod -> attr.removeModifier(mod.id()));
            }

            // 🔹 2. Apply active modifiers from equipped items
            for (var entry : groupInventories.entrySet()) {
                ItemStack stack = entry.getValue().getStackInSlot(0);
                if (stack.isEmpty()) continue;

                if (stack.getItem() instanceof EquippableItemBase equipItem) {
                    equipItem.getAttributeModifiers().forEach((holder, modifier) -> {
                        try {
                            var inst = owner.getAttribute(holder);
                            if (inst != null && !inst.hasModifier(modifier.id())) {
                                inst.addTransientModifier(modifier);
                            } else if (inst == null) {
                                String key = "missing_attr:" + holder;
                                if (LOGGED_WARNINGS.add(key)) {
                                    CatocraftMod.LOGGER.warn(
                                            "[EquipmentCap] Missing attribute instance for {} when applying {} (item: {})",
                                            holder, modifier.id(), stack.getItem()
                                    );
                                }
                            }
                        } catch (Exception e) {
                            String key = "modifier_error:" + modifier.id();
                            if (LOGGED_WARNINGS.add(key)) {
                                CatocraftMod.LOGGER.error(
                                        "[EquipmentCap] Failed to apply modifier {} from item {}: {}",
                                        modifier.id(), stack.getItem(), e.toString()
                                );
                            }
                        }
                    });
                }
            }

            // 🔹 3. Health + sync
            normalizeHealth();
            syncAttributesAndHealth();

        } catch (Exception e) {
            if (LOGGED_WARNINGS.add("global_error")) {
                CatocraftMod.LOGGER.error(
                        "[EquipmentCap] Unexpected error while reapplying attributes: {}", e.toString()
                );
            }
        }
    }

    /**
     * Called when slot layout JSONs are reloaded (e.g. /reload command).
     * Rebuilds all groups to reflect the new layout definitions,
     * while attempting to preserve equipped items wherever possible.
     */
    public void onLayoutsReloaded() {
        if (owner == null || owner.level().isClientSide) return;

        CatocraftMod.LOGGER.info("[EquipmentCap] Reloading slot layouts for {}", owner.getGameProfile().getName());

        try {
            // Backup currently equipped stacks
            Map<EquipmentGroup, List<ItemStack>> oldStacks = new LinkedHashMap<>();
            groupInventories.forEach((group, handler) -> {
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < handler.getSlots(); i++) {
                    items.add(handler.getStackInSlot(i).copy());
                }
                oldStacks.put(group, items);
            });

            // Rebuild inventory structure from the new definitions
            groupInventories.clear();
            initializeGroupsIfMissing();

            // Try to restore items into the updated slot handlers
            oldStacks.forEach((group, stacks) -> {
                var handler = groupInventories.get(group);
                if (handler == null) return;

                for (int i = 0; i < Math.min(handler.getSlots(), stacks.size()); i++) {
                    ItemStack stack = stacks.get(i);
                    if (!stack.isEmpty()) {
                        try {
                            // Only restore if still valid for that slot group
                            boolean validItem = SlotEquipValidator.canEquipItem(owner, group, stack);
                            if (validItem) {
                                handler.setStackInSlot(i, stack);
                            } else {
                                // Invalid item — put back into player inventory
                                owner.getInventory().placeItemBackInInventory(stack);
                                CatocraftMod.LOGGER.warn(
                                        "[EquipmentCap] Removed invalid item '{}' from group '{}'",
                                        stack.getItem(), group.getKey()
                                );
                            }
                        } catch (Exception e) {
                            CatocraftMod.LOGGER.error(
                                    "[EquipmentCap] Error restoring item {} in group {}: {}",
                                    stack.getItem(), group.getKey(), e.toString()
                            );
                        }
                    }
                }
            });

            // Reapply attributes after reload
            applyAllAttributes();
            normalizeHealth();

            CatocraftMod.LOGGER.info("[EquipmentCap] Layout reload complete for {}", owner.getName().getString());

        } catch (Exception e) {
            CatocraftMod.LOGGER.error("[EquipmentCap] Failed to reload layouts for {}: {}",
                    owner != null ? owner.getName().getString() : "unknown", e.toString());
        }
    }

    // ────────────────────────────────────────────────
    // Health + Sync helpers
    // ────────────────────────────────────────────────

    private void normalizeHealth() {
        float oldMax = (float) Math.max(owner.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH), 20.0);
        float newMax = (float) owner.getMaxHealth();
        float current = owner.getHealth();
        if (Math.abs(current - oldMax) < 0.1f) owner.setHealth(newMax);
        else owner.setHealth(Math.min(current, newMax));
    }

    private void syncAttributesAndHealth() {
        var toSync = owner.getAttributes().getAttributesToSync();
        if (!toSync.isEmpty()) {
            owner.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(owner.getId(), toSync));
            toSync.clear();
        }
        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                owner.getHealth(),
                owner.getFoodData().getFoodLevel(),
                owner.getFoodData().getSaturationLevel()
        ));
    }

    // ────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────

    public void reapplyAttributesOnLogin() {
        if (owner == null || owner.level().isClientSide) return;
        initializeGroupsIfMissing();
        applyAllAttributes();
        normalizeHealth();
        CatocraftMod.LOGGER.debug("[EquipmentCap] Reapplied attributes for {}", owner.getName().getString());
    }

    /** Schedule gradual health normalization after login or reload. */
    public void scheduleHealthNormalization(int ticks) {
        this.pendingHealthFixTicks = Math.max(this.pendingHealthFixTicks, ticks);
    }

    /** Gradually normalize player health if pending. */
    private void tickHealthNormalizationIfNeeded() {
        if (owner == null || owner.level().isClientSide) return;
        if (pendingHealthFixTicks <= 0) return;

        float max = (float) owner.getMaxHealth();
        float cur = owner.getHealth();

        if (cur > max) owner.setHealth(max);
        else if (cur < max - 0.5f) owner.setHealth(Math.min(cur + 0.25f, max));

        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                owner.getHealth(),
                owner.getFoodData().getFoodLevel(),
                owner.getFoodData().getSaturationLevel()
        ));

        pendingHealthFixTicks--;
    }

    public void tick() {
        tickHealthNormalizationIfNeeded();
        if (shouldSyncToClient() && owner != null) {
            MenuSyncHelper.forceSyncToClient(owner);
        }
    }

    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }
}