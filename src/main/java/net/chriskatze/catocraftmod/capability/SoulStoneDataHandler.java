package net.chriskatze.catocraftmod.capability;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Handles disk persistence for the Soul Stone capability.
 *  • Async GZIP save/load
 *  • SHA-256 integrity hashes
 *  • Rotating backups & automatic recovery
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class SoulStoneDataHandler {

    private static final String FILE_SUFFIX = "soulstone";
    private static final int BACKUP_COUNT = 5;
    private static final int BACKUP_MAX_AGE_DAYS = 30;
    private static final NbtAccounter UNLIMITED = NbtAccounter.unlimitedHeap();
    private static ExecutorService IO_EXECUTOR = createExecutor();

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SoulStoneData-IO-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    private static synchronized ExecutorService getExecutor() {
        if (IO_EXECUTOR == null || IO_EXECUTOR.isShutdown() || IO_EXECUTOR.isTerminated()) {
            CatocraftMod.LOGGER.warn("[SoulStoneDataHandler] Executor was terminated — recreating thread pool.");
            IO_EXECUTOR = createExecutor();
        }
        return IO_EXECUTOR;
    }

    /* ------------------------------------------------------
     * SAVE
     * ------------------------------------------------------ */

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        var handler = getHandler(player);
        if (handler == null) return;

        if (handler.getSlots() == 0 || handler.getStackInSlot(0).isEmpty()) {
            CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Skipping premature/empty save for {}", player.getName().getString());
            return;
        }

        File mainFile = event.getPlayerFile(FILE_SUFFIX);
        File tempFile = new File(mainFile.getAbsolutePath() + ".tmp");

        CompoundTag tag = handler.serializeNBT(player.registryAccess());
        if (tag == null || tag.isEmpty() || !tag.contains("Items") || tag.getList("Items", 10).isEmpty()) {
            CatocraftMod.LOGGER.warn("[SoulStoneDataHandler] Skipping save — tag empty for {}", player.getName().getString());
            return;
        }

        CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Saving Soul Stone data for {}: {}",
                player.getName().getString(), handler.getStackInSlot(0));

        asyncSave(tag, mainFile, tempFile, player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CatocraftMod.LOGGER.debug(
                "[SoulStoneDataHandler] Player {} is logging out — saving SoulStone data.",
                player.getName().getString()
        );

        // Retrieve the handler (capability)
        ItemStackHandler handler = getHandler(player);
        if (handler == null) {
            CatocraftMod.LOGGER.warn("[SoulStoneDataHandler] No handler found during logout save for {}", player.getName().getString());
            return;
        }

        // Perform an immediate, safe save on logout
        requestImmediateSave(player);
    }

    private static void asyncSave(CompoundTag tag, File mainFile, File tempFile, Player player) {
        getExecutor().submit(() -> {
            if (tag == null || tag.isEmpty() || !tag.contains("Items") || tag.getList("Items", 10).isEmpty()) {
                CatocraftMod.LOGGER.warn("[SoulStoneDataHandler] Skipping save — handler empty for {}", player.getName().getString());
                if (tempFile.exists()) tempFile.delete();
                return;
            }

            byte[] newHash = serializeToFileAndHash(tag, tempFile);
            if (newHash == null) return;

            if (mainFile.exists() && Arrays.equals(newHash, hashFile(mainFile))) {
                CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Soul Stone data unchanged for {}, skipping save.", player.getName().getString());
                tempFile.delete();
                return;
            }

            rotateAndBackup(mainFile);

            try {
                Files.move(tempFile.toPath(), mainFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                CatocraftMod.LOGGER.info("[SoulStoneDataHandler] Soul Stone data saved for {}", player.getName().getString());
            } catch (IOException e) {
                CatocraftMod.LOGGER.error("[SoulStoneDataHandler] Failed to finalize Soul Stone save for {}", player.getName().getString(), e);
            }
        });
    }

    /* ------------------------------------------------------
     * LOAD
     * ------------------------------------------------------ */

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        File mainFile = event.getPlayerFile(FILE_SUFFIX);
        File tempFile = new File(mainFile.getAbsolutePath() + ".tmp");
        recoverTempFile(mainFile, tempFile, player);

        ItemStackHandler handler = getHandler(player);
        if (handler != null) asyncLoad(mainFile, handler, player);
    }

    private static void asyncLoad(File mainFile, ItemStackHandler handler, ServerPlayer player) {
        getExecutor().submit(() -> {
            File[] candidates = new File[BACKUP_COUNT + 1];
            candidates[0] = mainFile;
            for (int i = 1; i <= BACKUP_COUNT; i++)
                candidates[i] = new File(mainFile.getAbsolutePath() + ".bak" + i);

            ServerLevel level = (ServerLevel) player.level();
            level.getServer().execute(() -> {
                boolean loaded = false;

                for (File file : candidates) {
                    if (!file.exists()) continue;
                    byte[] data = loadFileBytes(file);
                    if (data == null) continue;

                    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                         GZIPInputStream gis = new GZIPInputStream(bais);
                         DataInputStream dis = new DataInputStream(gis)) {

                        CompoundTag tag = CompoundTag.TYPE.load(dis, UNLIMITED);
                        handler.deserializeNBT(player.registryAccess(), tag);
                        CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Loaded data for {}: {}", player.getName().getString(), tag);
                        loaded = true;

                        if (!file.equals(mainFile)) {
                            try {
                                Files.write(mainFile.toPath(), data);
                                CatocraftMod.LOGGER.info("Restored main Soul Stone file from backup {}", file.getName());
                            } catch (IOException e) {
                                CatocraftMod.LOGGER.error("Failed to restore main file from backup {}", file.getName(), e);
                            }
                        }

                        net.chriskatze.catocraftmod.network.SoulStoneSyncHelper.syncToClient(player);
                        CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Synced loaded data to {}", player.getName().getString());
                        break;
                    } catch (IOException e) {
                        CatocraftMod.LOGGER.warn("Failed to deserialize {}. Trying next backup...", file.getName(), e);
                    }
                }

                if (!loaded)
                    CatocraftMod.LOGGER.warn("No valid Soul Stone data found for {}", player.getName().getString());
            });
        });
    }

    /* ------------------------------------------------------
     * AUTO-SYNC ON LOGIN / RESPAWN
     * ------------------------------------------------------ */
    @SubscribeEvent
    public static void onPlayerLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncToClient(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncToClient(player);
    }

    private static void syncToClient(ServerPlayer player) {
        var cap = player.getCapability(SoulStoneCapabilityHandler.SOULSTONE_CAP);
        if (cap instanceof ItemStackHandler handler) {
            net.chriskatze.catocraftmod.network.SoulStoneSyncHelper.syncToClient(player);
            CatocraftMod.LOGGER.debug("Synced Soul Stone data to client for {}", player.getName().getString());
        }
    }

    /* ------------------------------------------------------
     * MANUAL SAVE REQUEST
     * ------------------------------------------------------ */
    public static void requestImmediateSave(ServerPlayer player) {
        var cap = player.getCapability(SoulStoneCapabilityHandler.SOULSTONE_CAP);
        if (cap == null) {
            CatocraftMod.LOGGER.warn("[SoulStoneDataHandler] Tried to save but player {} has no capability!", player.getName().getString());
            return;
        }

        player.getServer().execute(() -> {
            File mainFile = new File(
                    player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(),
                    player.getStringUUID() + "." + FILE_SUFFIX
            );
            File tempFile = new File(mainFile.getAbsolutePath() + ".tmp");

            CompoundTag tag = cap.serializeNBT(player.registryAccess());
            if (tag == null || tag.isEmpty() || !tag.contains("Items") || tag.getList("Items", 10).isEmpty()) {
                CatocraftMod.LOGGER.warn("[SoulStoneDataHandler] Skipping immediate save — tag empty for {}", player.getName().getString());
                return;
            }

            CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Queuing immediate save for {}: {}", player.getName().getString(), tag);

            getExecutor().submit(() -> {
                byte[] newHash = serializeToFileAndHash(tag, tempFile);
                if (newHash == null) return;

                if (mainFile.exists() && Arrays.equals(newHash, hashFile(mainFile))) {
                    CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Forced save skipped (unchanged) for {}", player.getName().getString());
                    tempFile.delete();
                    return;
                }

                rotateAndBackup(mainFile);
                try {
                    Files.move(tempFile.toPath(), mainFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Forced save completed for {}", player.getName().getString());
                } catch (IOException e) {
                    CatocraftMod.LOGGER.error("[SoulStoneDataHandler] Forced save failed for {}", player.getName().getString(), e);
                }
            });
        });
    }

    /* ------------------------------------------------------
     * FILE HELPERS
     * ------------------------------------------------------ */
    private static ItemStackHandler getHandler(Player player) {
        var cap = player.getCapability(SoulStoneCapabilityHandler.SOULSTONE_CAP);
        return cap instanceof ItemStackHandler handler ? handler : null;
    }

    private static byte[] serializeToFileAndHash(CompoundTag tag, File file) {
        try {
            CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Writing tag to file {}: {}", file.getName(), tag);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileOutputStream fos = new FileOutputStream(file);
                 DigestOutputStream dos = new DigestOutputStream(fos, digest);
                 GZIPOutputStream gos = new GZIPOutputStream(dos);
                 DataOutputStream dataOut = new DataOutputStream(gos)) {

                tag.write(dataOut);
                dataOut.flush();
                gos.finish();
            }

            byte[] hash = digest.digest();
            CatocraftMod.LOGGER.debug("[SoulStoneDataHandler] Finished writing {} (SHA-256: {})", file.getName(), bytesToHex(hash));
            return hash;
        } catch (IOException | NoSuchAlgorithmException e) {
            CatocraftMod.LOGGER.error("Failed to serialize Soul Stone data", e);
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] hashFile(File file) {
        byte[] bytes = loadFileBytes(file);
        return bytes == null ? null : hashBytes(bytes);
    }

    private static byte[] hashBytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            CatocraftMod.LOGGER.error("SHA-256 not available!", e);
            return null;
        }
    }

    private static byte[] loadFileBytes(File file) {
        if (!file.exists()) return null;
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            CatocraftMod.LOGGER.warn("Failed to read file {}", file.getName(), e);
            return null;
        }
    }

    private static void recoverTempFile(File mainFile, File tempFile, ServerPlayer player) {
        if (!tempFile.exists()) return;
        try {
            CatocraftMod.LOGGER.warn("Recovering leftover Soul Stone save for {}", player.getName().getString());
            File safeTemp = new File(mainFile.getAbsolutePath() + ".recovered.tmp");
            Files.move(tempFile.toPath(), safeTemp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(safeTemp.toPath(), mainFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            CatocraftMod.LOGGER.error("Failed to recover Soul Stone save for {}", player.getName().getString(), e);
        }
    }

    private static void rotateAndBackup(File mainFile) {
        if (!mainFile.exists()) return;

        for (int i = BACKUP_COUNT; i > 0; i--) {
            File older = new File(mainFile.getAbsolutePath() + ".bak" + i);
            if (!older.exists()) continue;
            if (i == BACKUP_COUNT) older.delete();
            else older.renameTo(new File(mainFile.getAbsolutePath() + ".bak" + (i + 1)));
        }

        File backup1Temp = new File(mainFile.getAbsolutePath() + ".bak1.tmp");
        File backup1 = new File(mainFile.getAbsolutePath() + ".bak1");

        try (FileInputStream fis = new FileInputStream(mainFile);
             FileOutputStream fos = new FileOutputStream(backup1Temp)) {
            fis.transferTo(fos);
            fos.flush();
        } catch (IOException e) {
            CatocraftMod.LOGGER.error("Failed to create Soul Stone backup temp file: {}", backup1Temp.getName(), e);
            return;
        }

        try {
            Files.move(backup1Temp.toPath(), backup1.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            CatocraftMod.LOGGER.info("Created new Soul Stone backup: {} ({} bytes)", backup1.getName(), backup1.length());
        } catch (IOException e) {
            CatocraftMod.LOGGER.error("Failed to finalize Soul Stone backup: {}", backup1.getName(), e);
        }

        pruneOldBackups(mainFile);
    }

    private static void pruneOldBackups(File mainFile) {
        Instant cutoff = Instant.now().minus(BACKUP_MAX_AGE_DAYS, ChronoUnit.DAYS);
        int existingBackups = 0;

        for (int i = 1; i <= BACKUP_COUNT; i++)
            if (new File(mainFile.getAbsolutePath() + ".bak" + i).exists()) existingBackups++;

        for (int i = BACKUP_COUNT; i >= 1; i--) {
            File backup = new File(mainFile.getAbsolutePath() + ".bak" + i);
            if (!backup.exists()) continue;
            if (Instant.ofEpochMilli(backup.lastModified()).isBefore(cutoff) && existingBackups > 1) {
                if (backup.delete()) CatocraftMod.LOGGER.info("Pruned old Soul Stone backup: {}", backup.getName());
                existingBackups--;
            }
        }
    }

    /* ------------------------------------------------------
     * SHUTDOWN
     * ------------------------------------------------------ */
    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        CatocraftMod.LOGGER.info("Server stopping, shutting down SoulStoneDataHandler executor...");
        shutdown();
    }

    public static void shutdown() {
        IO_EXECUTOR.shutdown();
        try {
            if (!IO_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                CatocraftMod.LOGGER.warn("SoulStoneDataHandler executor did not terminate in 10 seconds, forcing shutdown.");
                IO_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            CatocraftMod.LOGGER.error("Interrupted while shutting down SoulStoneDataHandler executor.", e);
            IO_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}