package net.chriskatze.catocraftmod.capability;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.network.EarringSyncHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles lifecycle events (login, respawn, dimension change, clone)
 * for syncing and maintaining earring capability data.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class EarringCapabilityEvents {

    // ---------------------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EarringCapabilityHandler.EARRING_CAP);
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EarringCap] No capability found for {}", player.getName().getString());
            return;
        }

        cap.setOwner(player);
        cap.reapplyAttributesOnLogin();
        EarringSyncHelper.syncToClient(player);

        // Schedule health normalization for a few ticks after login
        cap.scheduleHealthNormalization(5);

        CatocraftMod.LOGGER.debug("[EarringCap] Reapplied attributes on login for {}", player.getName().getString());
    }

    // ---------------------------------------------------------------------
    // RESPAWN
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EarringCapabilityHandler.EARRING_CAP);
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EarringCap] No capability found for {}", player.getName().getString());
            return;
        }

        cap.setOwner(player);
        cap.reapplyAttributesOnLogin();
        EarringSyncHelper.syncToClient(player);

        // Normalize health after respawn too
        cap.scheduleHealthNormalization(5);

        CatocraftMod.LOGGER.debug("[EarringCap] Reapplied attributes on respawn for {}", player.getName().getString());
    }

    // ---------------------------------------------------------------------
    // DIMENSION CHANGE
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EarringCapabilityHandler.EARRING_CAP);
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[EarringCap] No capability found for {}", player.getName().getString());
            return;
        }

        cap.setOwner(player);
        cap.reapplyAttributesOnLogin();
        EarringSyncHelper.syncToClient(player);

        // Usually not needed, but safe to keep
        cap.scheduleHealthNormalization(3);

        CatocraftMod.LOGGER.debug(
                "[EarringCap] Reapplied attributes on dimension change for {} ({} -> {})",
                player.getName().getString(),
                event.getFrom().location(),
                event.getTo().location()
        );
    }

    // ---------------------------------------------------------------------
    // CLONE (Death)
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) ||
                !(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;

        boolean keepInventory = newPlayer.level().getGameRules()
                .getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);

        var oldCap = oldPlayer.getCapability(EarringCapabilityHandler.EARRING_CAP);
        var newCap = newPlayer.getCapability(EarringCapabilityHandler.EARRING_CAP);

        if (oldCap == null || newCap == null) return;

        if (keepInventory) {
            newCap.deserializeNBT(newPlayer.registryAccess(),
                    oldCap.serializeNBT(oldPlayer.registryAccess()));
            newCap.setOwner(newPlayer);
            newCap.reapplyAttributesOnLogin();
            EarringSyncHelper.syncToClient(newPlayer);

            newCap.scheduleHealthNormalization(5);

            CatocraftMod.LOGGER.debug("[EarringCap] Copied earring data on death for {}", newPlayer.getName().getString());
        } else {
            ItemStack stack = oldCap.getStackInSlot(0);
            if (!stack.isEmpty()) {
                oldPlayer.spawnAtLocation(stack.copy());
                oldCap.setStackInSlot(0, ItemStack.EMPTY);
                CatocraftMod.LOGGER.debug("[EarringCap] Dropped earring on death for {}", oldPlayer.getName().getString());
            }
        }
    }

    // ---------------------------------------------------------------------
    // PER-TICK HEALTH NORMALIZATION
    // ---------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var cap = player.getCapability(EarringCapabilityHandler.EARRING_CAP);
        if (cap == null) return;

        cap.setOwner(player);
        cap.tickHealthNormalizationIfNeeded();
    }
}