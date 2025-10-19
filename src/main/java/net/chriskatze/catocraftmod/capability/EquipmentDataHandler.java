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
 * Handles persistent save/load for the unified PlayerEquipmentCapability.
 * Performs async I/O with SHA-256 verification and rolling backups.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class EquipmentDataHandler {

    private static final String FILE_SUFFIX = "equipment";
    private static final int BACKUP_COUNT = 5;
    private static final int BACKUP_MAX_AGE_DAYS = 30;
    private static final NbtAccounter UNLIMITED = NbtAccounter.unlimitedHeap();
    private static ExecutorService IO_EXECUTOR = createExecutor();

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "EquipmentData-IO-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    private static synchronized ExecutorService getExecutor() {
        if (IO_EXECUTOR == null || IO_EXECUTOR.isShutdown() || IO_EXECUTOR.isTerminated()) {
            CatocraftMod.LOGGER.warn("[EquipmentData] Executor terminated — recreating thread pool.");
            IO_EXECUTOR = createExecutor();
        }
        return IO_EXECUTOR;
    }

    // --------------------------------------------------
    // SAVE
    // --------------------------------------------------

    @SubscribeEvent
    public static void onPlayerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        PlayerEquipmentCapability cap = getCap(player);
        if (cap == null) return;

        CompoundTag tag = cap.serializeNBT(player.registryAccess());
        if (isEmpty(tag)) {
            CatocraftMod.LOGGER.debug("[EquipmentData] Skipping save (empty) for {}", player.getName().getString());
            return;
        }

        File mainFile = event.getPlayerFile(FILE_SUFFIX);
        File tempFile = new File(mainFile.getAbsolutePath() + ".tmp");

        asyncSave(tag, mainFile, tempFile, player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CatocraftMod.LOGGER.debug("[EquipmentData] Logout save for {}", player.getName().getString());
        requestImmediateSave(player);
    }

    private static void asyncSave(CompoundTag tag, File mainFile, File tempFile, Player player) {
        getExecutor().submit(() -> {
            if (isEmpty(tag)) {
                CatocraftMod.LOGGER.debug("[EquipmentData] Empty tag, skipping save for {}", player.getName().getString());
                if (tempFile.exists()) tempFile.delete();
                return;
            }

            byte[] newHash = serializeToFileAndHash(tag, tempFile);
            if (newHash == null) return;

            if (mainFile.exists() && Arrays.equals(newHash, hashFile(mainFile))) {
                CatocraftMod.LOGGER.debug("[EquipmentData] Data unchanged for {}, skipping save.", player.getName().getString());
                tempFile.delete();
                return;
            }

            rotateAndBackup(mainFile);

            try {
                Files.move(tempFile.toPath(), mainFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                CatocraftMod.LOGGER.info("[EquipmentData] Saved unified equipment for {}", player.getName().getString());
            } catch (IOException e) {
                CatocraftMod.LOGGER.error("[EquipmentData] Failed to finalize save for {}", player.getName().getString(), e);
            }
        });
    }

    // --------------------------------------------------
    // LOAD
    // --------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        File mainFile = event.getPlayerFile(FILE_SUFFIX);
        File tempFile = new File(mainFile.getAbsolutePath() + ".tmp");
        recoverTempFile(mainFile, tempFile, player);

        PlayerEquipmentCapability cap = getCap(player);
        if (cap != null) asyncLoad(mainFile, cap, player);
    }

    private static void asyncLoad(File mainFile, PlayerEquipmentCapability cap, ServerPlayer player) {
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
                        cap.deserializeNBT(player.registryAccess(), tag);
                        loaded = true;

                        if (!file.equals(mainFile)) {
                            Files.write(mainFile.toPath(), data);
                            CatocraftMod.LOGGER.info("[EquipmentData] Restored main file from backup {}", file.getName());
                        }

                        cap.reapplyAttributesOnLogin();
                        CatocraftMod.LOGGER.debug("[EquipmentData] Loaded unified equipment for {}: {}", player.getName().getString(), tag);
                        break;
                    } catch (IOException e) {
                        CatocraftMod.LOGGER.warn("[EquipmentData] Failed to deserialize {}. Trying next backup...", file.getName(), e);
                    }
                }

                if (!loaded)
                    CatocraftMod.LOGGER.warn("[EquipmentData] No valid equipment data found for {}", player.getName().getString());
            });
        });
    }

    // --------------------------------------------------
    // SYNC + SAVE
    // --------------------------------------------------

    public static void requestImmediateSave(ServerPlayer player) {
        PlayerEquipmentCapability cap = getCap(player);
        if (cap == null) return;

        player.getServer().execute(() -> {
            File mainFile = new File(
                    player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(),
                    player.getStringUUID() + "." + FILE_SUFFIX
            );
            File tempFile = new File(mainFile.getAbsolutePath() + ".tmp");

            CompoundTag tag = cap.serializeNBT(player.registryAccess());
            if (isEmpty(tag)) return;

            getExecutor().submit(() -> {
                byte[] newHash = serializeToFileAndHash(tag, tempFile);
                if (newHash == null) return;

                if (mainFile.exists() && Arrays.equals(newHash, hashFile(mainFile))) {
                    tempFile.delete();
                    return;
                }

                rotateAndBackup(mainFile);
                try {
                    Files.move(tempFile.toPath(), mainFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    CatocraftMod.LOGGER.debug("[EquipmentData] Forced save completed for {}", player.getName().getString());
                } catch (IOException e) {
                    CatocraftMod.LOGGER.error("[EquipmentData] Forced save failed for {}", player.getName().getString(), e);
                }
            });
        });
    }

    // --------------------------------------------------
    // INTERNAL HELPERS
    // --------------------------------------------------

    private static PlayerEquipmentCapability getCap(Player player) {
        // ✅ NeoForge-style: direct typed return instead of instanceof
        if (!(player instanceof ServerPlayer sp)) return null;
        PlayerEquipmentCapability cap = sp.getCapability(EquipmentCapabilityHandler.EQUIPMENT_CAP);
        if (cap != null) {
            cap.setOwner(sp);
            cap.initializeGroupsIfMissing();
        }
        return cap;
    }

    private static boolean isEmpty(CompoundTag tag) {
        return tag == null || tag.isEmpty() ||
                !tag.contains("Groups") || tag.getList("Groups", 10).isEmpty();
    }

    private static byte[] serializeToFileAndHash(CompoundTag tag, File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileOutputStream fos = new FileOutputStream(file);
                 DigestOutputStream dos = new DigestOutputStream(fos, digest);
                 GZIPOutputStream gos = new GZIPOutputStream(dos);
                 DataOutputStream dataOut = new DataOutputStream(gos)) {
                tag.write(dataOut);
                dataOut.flush();
                gos.finish();
            }
            return digest.digest();
        } catch (IOException | NoSuchAlgorithmException e) {
            CatocraftMod.LOGGER.error("[EquipmentData] Failed to serialize data", e);
            return null;
        }
    }

    private static byte[] hashFile(File file) {
        byte[] bytes = loadFileBytes(file);
        if (bytes == null) return null;
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            CatocraftMod.LOGGER.error("SHA-256 not available!", e);
            return null;
        }
    }

    private static byte[] loadFileBytes(File file) {
        try {
            return Files.exists(file.toPath()) ? Files.readAllBytes(file.toPath()) : null;
        } catch (IOException e) {
            CatocraftMod.LOGGER.warn("[EquipmentData] Failed to read {}", file.getName(), e);
            return null;
        }
    }

    private static void recoverTempFile(File mainFile, File tempFile, ServerPlayer player) {
        if (!tempFile.exists()) return;
        try {
            File safeTemp = new File(mainFile.getAbsolutePath() + ".recovered.tmp");
            Files.move(tempFile.toPath(), safeTemp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.move(safeTemp.toPath(), mainFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            CatocraftMod.LOGGER.warn("[EquipmentData] Recovered leftover save for {}", player.getName().getString());
        } catch (IOException e) {
            CatocraftMod.LOGGER.error("[EquipmentData] Failed to recover save for {}", player.getName().getString(), e);
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
            CatocraftMod.LOGGER.error("[EquipmentData] Failed to create backup temp file: {}", backup1Temp.getName(), e);
            return;
        }

        try {
            Files.move(backup1Temp.toPath(), backup1.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            CatocraftMod.LOGGER.info("[EquipmentData] Created new backup: {} ({} bytes)", backup1.getName(), backup1.length());
        } catch (IOException e) {
            CatocraftMod.LOGGER.error("[EquipmentData] Failed to finalize backup: {}", backup1.getName(), e);
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
                if (backup.delete()) CatocraftMod.LOGGER.info("[EquipmentData] Pruned old backup: {}", backup.getName());
                existingBackups--;
            }
        }
    }

    // --------------------------------------------------
    // SHUTDOWN
    // --------------------------------------------------

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        CatocraftMod.LOGGER.info("[EquipmentData] Server stopping, shutting down executor...");
        shutdown();
    }

    public static void shutdown() {
        IO_EXECUTOR.shutdown();
        try {
            if (!IO_EXECUTOR.awaitTermination(10, TimeUnit.SECONDS)) {
                CatocraftMod.LOGGER.warn("[EquipmentData] Executor did not terminate in 10s, forcing shutdown.");
                IO_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            CatocraftMod.LOGGER.error("[EquipmentData] Interrupted during shutdown.", e);
            IO_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}