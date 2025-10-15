package net.chriskatze.catocraftmod.capability;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.item.JewelleryItem;
import net.chriskatze.catocraftmod.network.EarringSyncHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Stores a single equipped earring for a player.
 * Handles serialization, syncing, and attribute application.
 */
public class PlayerEarringCapability extends ItemStackHandler {

    private boolean dirty = false;
    private boolean suppressSync = false;
    private ServerPlayer owner;
    private int pendingHealthFixTicks = 0;

    public PlayerEarringCapability() {
        super(1);
    }

    public void setOwner(ServerPlayer player) {
        this.owner = player;
    }

    public ServerPlayer getOwner() {
        return owner;
    }

    // ------------------- Serialization -------------------

    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider registryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Size", getSlots());

        var list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag stackTag = (CompoundTag) stack.save(registryAccess, new CompoundTag());
                stackTag.putByte("Slot", (byte) i);
                list.add(stackTag);
            }
        }

        tag.put("Items", list);
        CatocraftMod.LOGGER.debug("[EarringCapability] serializeNBT -> {}", tag);
        return tag;
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registryAccess, CompoundTag nbt) {
        setSize(nbt.contains("Size", 3) ? nbt.getInt("Size") : 1);

        var list = nbt.getList("Items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag stackTag = list.getCompound(i);
            int slot = stackTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < getSlots()) {
                ItemStack stack = ItemStack.parseOptional(registryAccess, stackTag);
                setStackInSlot(slot, stack);
            }
        }

        CatocraftMod.LOGGER.debug("[EarringCapability] deserializeNBT <- {}", nbt);
    }

    // ------------------- Syncing -------------------

    /** Applies server-synced NBT safely without triggering another sync. */
    public void applySyncFromServer(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registryAccess) {
        this.suppressSync = true;
        try {
            deserializeNBT(registryAccess, tag);
            applyJewelleryAttributes();
        } finally {
            this.suppressSync = false;
        }
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (suppressSync) return;

        dirty = true;
        CatocraftMod.LOGGER.debug("[EarringCapability] Slot {} changed, marking dirty", slot);

        if (owner != null && !owner.level().isClientSide) {
            applyJewelleryAttributes();
            EarringSyncHelper.syncToClient(owner);
        }
    }

    // ------------------- Attribute Logic -------------------

    /** Applies modifiers for the currently equipped earring. */
    private void applyJewelleryAttributes() {
        if (owner == null || owner.level().isClientSide) return;

        ItemStack stack = getStackInSlot(0);

        // 1️⃣ Remove all old modifiers from our mod
        for (AttributeInstance attr : owner.getAttributes().getSyncableAttributes()) {
            attr.getModifiers().stream()
                    .filter(mod -> mod.id().getNamespace().equals(CatocraftMod.MOD_ID))
                    .toList()
                    .forEach(mod -> attr.removeModifier(mod.id()));
        }

        // 2️⃣ Apply new modifiers if this is a jewellery item
        if (stack.getItem() instanceof JewelleryItem jewellery) {
            var mods = jewellery.getJewelleryModifiers();
            mods.forEach((holder, modifier) -> {
                var instance = owner.getAttribute(holder);
                if (instance != null && !instance.hasModifier(modifier.id())) {
                    instance.addTransientModifier(modifier);

                    String attrKey = holder.unwrapKey()
                            .map(k -> k.location().toString())
                            .orElse("[unregistered]");
                    CatocraftMod.LOGGER.debug("[EarringCapability] Applied modifier {} to {}", modifier.id(), attrKey);
                }
            });
        }

        // 3️⃣ Correct health value based on old vs. new max
        float oldMax = (float) Math.max(owner.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH), 20.0);
        float newMax = (float) owner.getMaxHealth();
        float current = owner.getHealth();

        // If player was nearly full before, keep them full after the bonus
        if (Math.abs(current - oldMax) < 0.1f) {
            owner.setHealth(newMax);
        } else {
            // Otherwise just clamp to new max
            owner.setHealth(Math.min(current, newMax));
        }

        // 4️⃣ Force attribute and health sync to client
        var attributesToSync = owner.getAttributes().getAttributesToSync();
        if (!attributesToSync.isEmpty()) {
            owner.connection.send(new net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket(
                    owner.getId(), attributesToSync
            ));
            attributesToSync.clear();
        }

        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                owner.getHealth(),
                owner.getFoodData().getFoodLevel(),
                owner.getFoodData().getSaturationLevel()
        ));
    }

    /** Reapplies modifiers on login, respawn, or dimension change. */
    public void reapplyAttributesOnLogin() {
        if (owner != null && !owner.level().isClientSide) {
            applyJewelleryAttributes();
        }
    }

    // Ask to normalize health for N END ticks (recommend 5)
    public void scheduleHealthNormalization(int ticks) {
        // keep the longest request so overlapping schedules don't cut each other short
        this.pendingHealthFixTicks = Math.max(this.pendingHealthFixTicks, ticks);
    }

    // Called from PlayerTickEvent.Post (END phase)
    public void tickHealthNormalizationIfNeeded() {
        if (owner == null || owner.level().isClientSide) return;
        if (pendingHealthFixTicks <= 0) return;

        // Normalize health *every* END tick while countdown is active
        float max = (float) owner.getMaxHealth();
        float cur  = owner.getHealth();
        owner.setHealth(cur >= max - 0.5f ? max : Math.min(cur, max));

        // Push HUD immediately
        owner.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                owner.getHealth(),
                owner.getFoodData().getFoodLevel(),
                owner.getFoodData().getSaturationLevel()
        ));

        // Decrement after applying so we run for the requested number of ticks
        pendingHealthFixTicks--;
    }

    // ------------------- Dirty Tracking -------------------
    public void setChanged() { this.dirty = true; }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { this.dirty = false; }
}