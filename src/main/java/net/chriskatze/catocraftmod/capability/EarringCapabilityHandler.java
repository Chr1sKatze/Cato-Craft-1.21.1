/**
 * ================================================================
 *  🧿 CatoCraft Earring Capability System  (NeoForge 1.21.1)
 * ================================================================
 *
 *  Overview
 *  --------
 *  This package implements a fully persistent, syncable, and modular
 *  player capability for handling *equipped jewellery items* (earrings).
 *
 *  Key Features
 *  -------------
 *  • Persistent per-player data stored under player UUID
 *  • One-slot ItemStackHandler for equipped earring
 *  • Automatic attribute application/removal on equip/unequip
 *  • Safe server→client sync via custom network packet
 *  • Survives death, respawn, dimension change, and logout
 *  • Disk persistence with backup rotation and corruption recovery
 *
 *  ----------------------------------------------------------------
 *  File Summary
 *  ----------------------------------------------------------------
 *  🟢 PlayerEarringCapability
 *     → Core logic & data storage.
 *       - Manages the single earring slot
 *       - Handles attribute add/remove
 *       - Serializes with HolderLookup.Provider
 *       - Calls EarringSyncHelper when changed
 *
 *  🟢 EarringCapabilityHandler
 *     → Registers the capability and provides UUID-cached instances.
 *       - Creates one PlayerEarringCapability per ServerPlayer
 *       - Exposes EARRING_CAP as the EntityCapability key
 *       - Handles cloning between dimensions
 *
 *  🟢 EarringCapabilityEvents
 *     → Listens for lifecycle events.
 *       - onLogin, onRespawn, onDimensionChange, onClone
 *       - Reapplies modifiers & syncs to client
 *
 *  🟢 EarringDataHandler
 *     → Handles disk I/O and version-safe persistence.
 *       - Saves player earring data to /playerdata/<uuid>.earrings
 *       - Uses async IO thread with GZIP compression
 *       - Keeps 5 rotating backups (30-day retention)
 *       - Restores automatically if main file corrupted
 *
 *  🟢 EarringItemHandler
 *     → Lightweight ItemStackHandler helper (validation, NBT I/O)
 *
 *  🟢 JewelleryItem
 *     → Base item type for all jewellery.
 *       - Provides ImmutableMultimap<Attribute, AttributeModifier>
 *       - Adds formatted tooltip lines for modifiers
 *
 *  ----------------------------------------------------------------
 *  Data & Sync Flow
 *  ----------------------------------------------------------------
 *  [Server Load / Login]
 *     ↳ EarringDataHandler loads .earrings file
 *     ↳ Capability attached via EarringCapabilityHandler
 *     ↳ Attributes applied → EarringSyncHelper → Client updated
 *
 *  [Equip/Unequip]
 *     ↳ PlayerEarringCapability.onContentsChanged()
 *     ↳ applyJewelleryAttributes() → syncToClient()
 *
 *  [Death / Respawn / Dim Change]
 *     ↳ EarringCapabilityEvents handles clone / reapply
 *
 *  [Logout / Server Stop]
 *     ↳ EarringDataHandler async-saves player capability to disk
 *
 * ================================================================
 *  © 2025  CatoCraft Mod — Capability system designed for modular
 *  jewellery equipment and attribute-driven gameplay features.
 * ================================================================
 */

package net.chriskatze.catocraftmod.capability;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles registration and persistence of the player's earring capability.
 * NeoForge 21.1.209+.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class EarringCapabilityHandler {

    /** One persistent instance per player UUID (server side). */
    private static final Map<UUID, PlayerEarringCapability> EARRING_INSTANCES = new ConcurrentHashMap<>();

    /** Capability key for player earrings. */
    public static final EntityCapability<PlayerEarringCapability, Void> EARRING_CAP =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "earring_cap"),
                    PlayerEarringCapability.class
            );

    /**
     * Register capability provider for Player entities.
     * Always returns the SAME instance for a given player UUID.
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                EARRING_CAP,
                EntityType.PLAYER,
                (player, ctx) -> EARRING_INSTANCES.computeIfAbsent(player.getUUID(), id -> {
                    PlayerEarringCapability cap = new PlayerEarringCapability();
                    if (player instanceof ServerPlayer sp) {
                        cap.setOwner(sp); // 🔗 link owner for auto-sync
                    }
                    return cap;
                })
        );

        CatocraftMod.LOGGER.info("[EarringCap] Registered entity capability for Player (per-UUID cached instance with owner link).");
    }

    /**
     * Copy data when player respawns or changes dimension.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        PlayerEarringCapability oldCap = oldPlayer.getCapability(EARRING_CAP);
        PlayerEarringCapability newCap = newPlayer.getCapability(EARRING_CAP);

        if (oldCap != null && newCap != null) {
            newCap.deserializeNBT(newPlayer.registryAccess(),
                    oldCap.serializeNBT(oldPlayer.registryAccess()));
            CatocraftMod.LOGGER.debug("[EarringCap] Cloned earring data for {}", newPlayer.getName().getString());
        }
    }

    /**
     * Optional cleanup when a player truly leaves the world forever.
     * (Comment out if you want instances to survive across sessions in the same JVM.)
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        EARRING_INSTANCES.remove(player.getUUID());
        CatocraftMod.LOGGER.debug("[EarringCap] Cleared cached instance for {}", player.getName().getString());
    }

    /**
     * Debug log on login.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        PlayerEarringCapability cap = player.getCapability(EARRING_CAP);

        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EarringCap] No capability found for {} on login!", player.getName().getString());
            return;
        }

        // ✅ NEW: Log the raw NBT content that was just loaded
        var tag = cap.serializeNBT(player.registryAccess());
        CatocraftMod.LOGGER.debug("[EarringCap] [VERIFY] Loaded handler state on login -> {}", tag);

        ItemStack stack = cap.getStackInSlot(0);
        if (stack.isEmpty()) {
            CatocraftMod.LOGGER.info("[EarringCap] Player {} logged in with empty earring slot.", player.getName().getString());
        } else {
            CatocraftMod.LOGGER.info("[EarringCap] Player {} logged in wearing {}", player.getName().getString(), stack);
        }
    }
}