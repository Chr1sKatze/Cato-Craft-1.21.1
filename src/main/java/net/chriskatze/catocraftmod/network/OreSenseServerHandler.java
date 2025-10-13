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

@EventBusSubscriber(modid = CatocraftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class OreSenseServerHandler {

    private static final int BASE_COOLDOWN = 400; // 20s
    private static final int COOLDOWN_REDUCTION_PER_LEVEL = 20; // -1s per level
    private static final int BASE_GLOW_DURATION = 60; // 3s
    private static final int GLOW_RADIUS = 8;
    private static final int BROADCAST_RADIUS = 128;

    // cooldown counter per player
    private static final Map<UUID, Integer> cooldownMap = new HashMap<>();

    // active glowing blocks per player
    private static final Map<UUID, List<BlockPos>> activeGlowMap = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {

            Map<ServerPlayer, List<BlockPos>> playerGlowUpdates = new HashMap<>();

            for (ServerPlayer player : level.players()) {
                UUID playerId = player.getUUID();
                int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.ORE_SENSE.getHolder(), player.getMainHandItem()
                );

                List<BlockPos> previousGlow = activeGlowMap.getOrDefault(playerId, Collections.emptyList());

                // No enchantment → remove glow immediately
                if (enchantLevel <= 0) {
                    if (!previousGlow.isEmpty()) {
                        activeGlowMap.remove(playerId);
                        cooldownMap.remove(playerId);
                        OreSenseGlowPacket.sendToClient(level, player, Collections.emptyList(), 0);
                    }
                    continue;
                }

                // Increment cooldown
                int cooldown = cooldownMap.getOrDefault(playerId, 0) + 1;
                cooldownMap.put(playerId, cooldown);

                int maxCooldown = Math.max(40, BASE_COOLDOWN - (enchantLevel - 1) * COOLDOWN_REDUCTION_PER_LEVEL);
                List<BlockPos> newGlow = previousGlow;

                // Trigger glow update on cooldown
                if (cooldown >= maxCooldown) {
                    cooldownMap.put(playerId, 0);
                    newGlow = findGlowBlocks(level, player);
                }

                // Only send update if glow changed
                if (!newGlow.equals(previousGlow)) {
                    activeGlowMap.put(playerId, newGlow);
                    if (!newGlow.isEmpty()) {
                        playerGlowUpdates.put(player, newGlow);
                    } else {
                        OreSenseGlowPacket.sendToClient(level, player, Collections.emptyList(), 0);
                    }
                }
            }

            // Deduplicate globally
            Set<BlockPos> allGlowBlocks = new HashSet<>();
            for (List<BlockPos> blocks : playerGlowUpdates.values()) {
                allGlowBlocks.addAll(blocks);
            }

            // Broadcast to nearby players
            for (ServerPlayer recipient : level.players()) {
                List<BlockPos> visibleBlocks = new ArrayList<>();
                for (BlockPos pos : allGlowBlocks) {
                    for (ServerPlayer origin : playerGlowUpdates.keySet()) {
                        if (recipient.distanceToSqr(origin) <= BROADCAST_RADIUS * BROADCAST_RADIUS) {
                            visibleBlocks.add(pos);
                            break;
                        }
                    }
                }

                if (!visibleBlocks.isEmpty()) {
                    int maxLevel = playerGlowUpdates.keySet().stream()
                            .filter(p -> recipient.distanceToSqr(p) <= BROADCAST_RADIUS * BROADCAST_RADIUS)
                            .mapToInt(p -> EnchantmentHelper.getItemEnchantmentLevel(
                                    ModEnchantments.ORE_SENSE.getHolder(), p.getMainHandItem()))
                            .max().orElse(1);
                    int glowDuration = BASE_GLOW_DURATION + (maxLevel - 1) * 20;

                    OreSenseGlowPacket.sendToClient(level, recipient, visibleBlocks, glowDuration);
                }
            }
        }
    }

    private static List<BlockPos> findGlowBlocks(ServerLevel level, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        List<BlockPos> glowBlocks = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-GLOW_RADIUS, -GLOW_RADIUS, -GLOW_RADIUS),
                center.offset(GLOW_RADIUS, GLOW_RADIUS, GLOW_RADIUS))) {
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockState(pos).is(ModTags.EMISSIVE_ORES)) {
                glowBlocks.add(pos.immutable());
            }
        }
        return glowBlocks;
    }
}