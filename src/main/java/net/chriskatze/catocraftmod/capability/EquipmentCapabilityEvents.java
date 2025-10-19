package net.chriskatze.catocraftmod.capability;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Global lifecycle event handler for the unified PlayerEquipmentCapability.
 * Handles login, respawn, dimension change, clone, and per-tick updates.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class EquipmentCapabilityEvents {

    // ────────────────────────────────────────────────
    // LOGIN
    // ────────────────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EquipmentCap] Missing capability for {}", player.getName().getString());
            return;
        }

        cap.setOwner(player);
        cap.initializeGroupsIfMissing();
        cap.reapplyAttributesOnLogin();
        cap.scheduleHealthNormalization(5);

        CatocraftMod.LOGGER.debug("[EquipmentCap] Reapplied attributes on login for {}", player.getName().getString());
    }

    // ────────────────────────────────────────────────
    // RESPAWN
    // ────────────────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        cap.setOwner(player);
        cap.reapplyAttributesOnLogin();
        cap.scheduleHealthNormalization(5);

        CatocraftMod.LOGGER.debug("[EquipmentCap] Reapplied attributes on respawn for {}", player.getName().getString());
    }

    // ────────────────────────────────────────────────
    // DIMENSION CHANGE
    // ────────────────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        cap.setOwner(player);
        cap.reapplyAttributesOnLogin();
        cap.scheduleHealthNormalization(3);

        CatocraftMod.LOGGER.debug(
                "[EquipmentCap] Reapplied attributes on dimension change for {} ({} -> {})",
                player.getName().getString(),
                event.getFrom().location(),
                event.getTo().location()
        );
    }

    // ────────────────────────────────────────────────
    // CLONE (Death)
    // ────────────────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)
                || !(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;

        boolean keepInventory = newPlayer.level().getGameRules()
                .getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);

        var oldCap = oldPlayer.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        var newCap = newPlayer.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);

        if (oldCap == null || newCap == null) return;

        if (keepInventory) {
            newCap.deserializeNBT(newPlayer.registryAccess(), oldCap.serializeNBT(oldPlayer.registryAccess()));
            newCap.setOwner(newPlayer);
            newCap.reapplyAttributesOnLogin();
            newCap.scheduleHealthNormalization(5);

            CatocraftMod.LOGGER.debug("[EquipmentCap] Copied unified equipment data on death for {}", newPlayer.getName().getString());
        } else {
            // Optional: drop equipped items (per-group logic can be added later)
            CatocraftMod.LOGGER.debug("[EquipmentCap] Skipping item drop for now (future per-group feature).");
        }
    }

    // ────────────────────────────────────────────────
    // PER-TICK UPDATES (Health + Sync)
    // ────────────────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap == null) return;

        cap.setOwner(player);
        cap.tick(); // Handles both health normalization and client sync throttling
    }
}