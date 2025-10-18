package net.chriskatze.catocraftmod.network;

import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.enchantment.ModEnchantments;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * Server-side handler for the Revelation enchantment.
 *
 * This class runs on every server tick and checks which players have Revelation equipped.
 * It calculates which nearby ore blocks should glow and sends packets to clients to display
 * the glowing overlay. It also handles cooldowns, radius scaling per enchantment level,
 * and optimized broadcasting to nearby players.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID)
public class RevelationServerHandler {

    // ---------------------------------------------------------------------
    // Configuration Constants
    // ---------------------------------------------------------------------

    private static final int BASE_COOLDOWN = 400;                 // Base cooldown in ticks (20s)
    private static final int COOLDOWN_REDUCTION_PER_LEVEL = 20;   // Reduce cooldown by 1s per enchant level
    private static final int BASE_GLOW_RADIUS = 10;               // Base glow radius in blocks
    private static final int RADIUS_INCREASE_PER_LEVEL = 1;       // Additional radius per enchant level
    private static final int GLOW_DURATION_TICKS = 120;           // How long the glow lasts (6s)
    private static final int BROADCAST_RADIUS = 64;               // Max distance for sending glow packets

    /** Tracks per-player cooldown timers for glow activation. */
    private static final Map<UUID, Integer> cooldownMap = new HashMap<>();

    // ---------------------------------------------------------------------
    // Server Tick Handling
    // ---------------------------------------------------------------------

    /**
     * Runs every server tick and checks which players have Revelation active.
     * Calculates glowing ores and sends packets to nearby clients.
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server == null) return;

        // Loop over all loaded worlds
        for (ServerLevel level : server.getAllLevels()) {
            List<ServerPlayer> players = level.players();
            if (players.isEmpty()) continue;

            // -------------------------------------------------------------
            // Pre-filter players who currently have the Revelation enchantment
            // -------------------------------------------------------------
            Map<ServerPlayer, Integer> enchantedPlayers = new HashMap<>();
            for (ServerPlayer player : players) {
                int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.REVELATION.getHolder(), player.getMainHandItem()
                );

                if (enchantLevel > 0) {
                    enchantedPlayers.put(player, enchantLevel);
                } else {
                    cooldownMap.remove(player.getUUID()); // Cleanup cooldown for unequipped players
                }
            }

            if (enchantedPlayers.isEmpty()) continue; // Skip if no players have enchantment

            // Maps players to blocks that should glow, and collect all blocks for broadcast
            Map<ServerPlayer, List<BlockPos>> playerGlowMap = new HashMap<>();
            Set<BlockPos> allGlowBlocks = new HashSet<>();

            // -------------------------------------------------------------
            // Compute glowing blocks for each enchanted player
            // -------------------------------------------------------------
            for (Map.Entry<ServerPlayer, Integer> entry : enchantedPlayers.entrySet()) {
                ServerPlayer player = entry.getKey();
                int enchantLevel = entry.getValue();
                UUID playerId = player.getUUID();

                // Update cooldown
                int cooldown = cooldownMap.getOrDefault(playerId, 0) + 1;
                cooldownMap.put(playerId, cooldown);

                int effectiveCooldown = Math.max(GLOW_DURATION_TICKS,
                        BASE_COOLDOWN - (enchantLevel - 1) * COOLDOWN_REDUCTION_PER_LEVEL);

                if (cooldown < effectiveCooldown) continue; // Still on cooldown
                cooldownMap.put(playerId, 0); // Reset cooldown

                // Determine radius of effect based on enchantment level
                int radius = BASE_GLOW_RADIUS + (enchantLevel - 1) * RADIUS_INCREASE_PER_LEVEL;

                List<BlockPos> glowBlocks = findGlowBlocks(level, player.blockPosition(), radius);
                if (!glowBlocks.isEmpty()) {
                    playerGlowMap.put(player, glowBlocks);
                    allGlowBlocks.addAll(glowBlocks);
                }
            }

            if (allGlowBlocks.isEmpty()) continue; // Nothing to send

            // -------------------------------------------------------------
            // Broadcast glow information to nearby players efficiently
            // -------------------------------------------------------------
            int broadcastRadiusSqr = BROADCAST_RADIUS * BROADCAST_RADIUS;
            for (ServerPlayer recipient : players) {
                double rx = recipient.getX(), ry = recipient.getY(), rz = recipient.getZ();
                List<BlockPos> visibleBlocks = new ArrayList<>();

                // Only include glowing blocks from players within broadcast radius
                for (ServerPlayer origin : playerGlowMap.keySet()) {
                    double dx = origin.getX() - rx;
                    double dy = origin.getY() - ry;
                    double dz = origin.getZ() - rz;
                    if (dx * dx + dy * dy + dz * dz <= broadcastRadiusSqr) {
                        visibleBlocks.addAll(allGlowBlocks);
                        break; // No need to check other origins
                    }
                }

                // Send packet to client if there are any visible blocks
                if (!visibleBlocks.isEmpty()) {
                    RevelationGlowPacket.sendToClient(level, recipient, visibleBlocks, GLOW_DURATION_TICKS);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Block Detection Helpers
    // ---------------------------------------------------------------------

    /**
     * Finds all ore blocks within a cubic radius around a center position.
     * Only includes blocks that are loaded and tagged as REVELATION_ORES.
     */
    private static List<BlockPos> findGlowBlocks(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> glowBlocks = new ArrayList<>();

        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minY = center.getY() - radius, maxY = center.getY() + radius;
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;

        // Iterate over all blocks in the cubic area
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (!level.hasChunkAt(pos)) continue; // Skip unloaded chunks

                    if (level.getBlockState(pos).is(ModTags.REVELATION_ORES)) {
                        glowBlocks.add(pos.immutable()); // Immutable to prevent accidental modifications
                    }
                }
            }
        }
        return glowBlocks;
    }
}