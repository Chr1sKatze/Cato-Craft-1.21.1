package net.chriskatze.catocraftmod.menu.event;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.capability.PlayerEquipmentCapability;
import net.chriskatze.catocraftmod.capability.EquipmentCapabilityHandler;
import net.chriskatze.catocraftmod.menu.layout.EquipmentGroup;
import net.chriskatze.catocraftmod.network.EquipmentSyncHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Ensures player equipment capabilities and layout registry
 * are reloaded and re-synced when datapacks or worlds reload.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class LayoutReloadListener {

    /**
     * Called when datapacks or resources are reloaded (e.g., /reload command).
     */
    @SubscribeEvent
    public static void onDatapackReload(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        // 🟢 Step 3: Reset base and dynamic equipment groups before reload
        EquipmentGroup.resetBaseGroups();
        CatocraftMod.LOGGER.info("[Catocraft] Reset base EquipmentGroup registry before reloading layouts.");

        var server = serverLevel.getServer();
        server.getPlayerList().getPlayers().forEach(LayoutReloadListener::reconcilePlayer);

        CatocraftMod.LOGGER.info("[Catocraft] Reconciled equipment layouts for all players after reload.");
        CatocraftMod.LOGGER.debug("[Catocraft] Registered groups after reload: {}",
                EquipmentGroup.all().stream().map(EquipmentGroup::getKey).toList());
    }

    /**
     * Called when the dedicated or integrated server fully starts up.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var server = event.getServer();

        // 🟢 Ensure registry starts with base groups
        EquipmentGroup.resetBaseGroups();
        CatocraftMod.LOGGER.info("[Catocraft] Initialized EquipmentGroup registry at server start.");

        server.getPlayerList().getPlayers().forEach(LayoutReloadListener::reconcilePlayer);
        CatocraftMod.LOGGER.info("[Catocraft] Reconciled equipment layouts for all players after server start.");
    }

    /**
     * Called when the server is stopped to clear stale data.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // 🟢 Optional cleanup to prevent lingering data across world reloads
        CatocraftMod.LOGGER.info("[Catocraft] Server stopped — clearing EquipmentGroup registry.");
        EquipmentGroup.resetBaseGroups();
    }

    // ────────────────────────────────────────────────
    // Internal helper
    // ────────────────────────────────────────────────

    private static void reconcilePlayer(ServerPlayer player) {
        PlayerEquipmentCapability cap = EquipmentCapabilityHandler.get(player);
        if (cap != null) {
            cap.initializeGroupsIfMissing();
            cap.onLayoutsReloaded();
            cap.applyAllAttributes();
            cap.scheduleHealthNormalization(20);

            // 🔹 Consistent behavior: instant client sync
            EquipmentSyncHelper.syncToClient(player);

            CatocraftMod.LOGGER.debug("[Catocraft] Reconciled and synced layouts for {}", player.getGameProfile().getName());
        }
    }
}