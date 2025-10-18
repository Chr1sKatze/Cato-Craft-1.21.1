/**
 * ================================================================
 *  💎 CatoCraft Soul Stone Capability System (NeoForge 1.21.1)
 * ================================================================
 *
 *  Overview
 *  --------
 *  Implements a persistent, syncable player capability for handling
 *  the equipped Soul Stone item.
 *
 *  Key Features
 *  -------------
 *  • One-slot ItemStackHandler for the Soul Stone
 *  • Persistent per-player instance (UUID-cached)
 *  • Automatic server/client synchronization
 *  • Safe cloning on respawn and dimension change
 *  • Linked to PlayerSoulStoneCapability for archetype data
 *
 *  ---------------------------------------------------------------
 *  File Summary
 *  ---------------------------------------------------------------
 *  🟢 PlayerSoulStoneCapability
 *     → Core logic for the single soul stone slot and attributes
 *
 *  🟢 SoulStoneCapabilityHandler
 *     → Registers the capability, manages per-player instances
 *
 *  🟢 SoulStoneDataHandler
 *     → Disk persistence & version-safe saves
 *
 *  🟢 SoulStoneSyncHelper
 *     → Handles client synchronization packet
 *
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
 * Handles registration and persistence of the player's Soul Stone capability.
 * NeoForge 21.1.209+.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class SoulStoneCapabilityHandler {

    /** One persistent instance per player UUID (server side). */
    private static final Map<UUID, PlayerSoulStoneCapability> SOULSTONE_INSTANCES = new ConcurrentHashMap<>();

    /** Capability key for player Soul Stone slot. */
    public static final EntityCapability<PlayerSoulStoneCapability, Void> SOULSTONE_CAP =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(CatocraftMod.MOD_ID, "soulstone_cap"),
                    PlayerSoulStoneCapability.class
            );

    /**
     * Register capability provider for Player entities.
     * Always returns the SAME instance for a given player UUID.
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                SOULSTONE_CAP,
                EntityType.PLAYER,
                (player, ctx) -> SOULSTONE_INSTANCES.computeIfAbsent(player.getUUID(), id -> {
                    PlayerSoulStoneCapability cap = new PlayerSoulStoneCapability();
                    if (player instanceof ServerPlayer sp) {
                        cap.setOwner(sp); // 🔗 link owner for sync
                    }
                    return cap;
                })
        );

        CatocraftMod.LOGGER.info("[SoulStoneCap] Registered entity capability for Player (per-UUID cached instance).");
    }

    /**
     * Copy data when player respawns or changes dimension.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();

        PlayerSoulStoneCapability oldCap = oldPlayer.getCapability(SOULSTONE_CAP);
        PlayerSoulStoneCapability newCap = newPlayer.getCapability(SOULSTONE_CAP);

        if (oldCap != null && newCap != null) {
            newCap.deserializeNBT(newPlayer.registryAccess(),
                    oldCap.serializeNBT(oldPlayer.registryAccess()));
            CatocraftMod.LOGGER.debug("[SoulStoneCap] Cloned Soul Stone data for {}", newPlayer.getName().getString());
        }
    }

    /**
     * Remove cached instance when player logs out.
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        SOULSTONE_INSTANCES.remove(player.getUUID());
        CatocraftMod.LOGGER.debug("[SoulStoneCap] Cleared cached instance for {}", player.getName().getString());
    }

    /**
     * Debug information on login.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        PlayerSoulStoneCapability cap = player.getCapability(SOULSTONE_CAP);

        if (cap == null) {
            CatocraftMod.LOGGER.warn("[SoulStoneCap] No capability found for {} on login!", player.getName().getString());
            return;
        }

        var tag = cap.serializeNBT(player.registryAccess());
        CatocraftMod.LOGGER.debug("[SoulStoneCap] [VERIFY] Loaded Soul Stone handler -> {}", tag);

        ItemStack stack = cap.getStackInSlot(0);
        if (stack.isEmpty()) {
            CatocraftMod.LOGGER.info("[SoulStoneCap] Player {} logged in with empty Soul Stone slot.", player.getName().getString());
        } else {
            CatocraftMod.LOGGER.info("[SoulStoneCap] Player {} logged in carrying Soul Stone {}", player.getName().getString(), stack);
        }
    }
}