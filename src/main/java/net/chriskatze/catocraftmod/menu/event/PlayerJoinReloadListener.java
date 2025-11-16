package net.chriskatze.catocraftmod.menu.event;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.network.MenuSyncHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Ensures equipment layouts are reloaded, synced, and visually updated
 * when a player joins or reconnects.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class PlayerJoinReloadListener {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerEquipmentCapability cap = EquipmentCapabilityHandler.get(player);
        if (cap != null) {
            // Rebuild slots + restore items
            cap.initializeGroupsIfMissing();
            cap.onLayoutsReloaded();

            // Reapply attributes immediately
            cap.applyAllAttributes();
            cap.scheduleHealthNormalization(20);

            // 🔹 Instant client sync
            MenuSyncHelper.syncToClient(player);

            CatocraftMod.LOGGER.info("[Catocraft] Refreshed and synced layouts for {} on join.",
                    player.getGameProfile().getName());
        }
    }
}