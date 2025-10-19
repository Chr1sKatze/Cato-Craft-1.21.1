package net.chriskatze.catocraftmod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles rendering of temporary glowing overlays on ore blocks detected by the Revelation enchantment.
 *
 * This renderer operates entirely client-side, adding translucent overlays to qualifying ores
 * for a limited duration. It supports fade-in/out animations, distance-based brightness falloff,
 * and neighbor/frustum culling for performance.
 */
public class OreGlowRenderer {

    // ---------------------------------------------------------------------
    // Internal Data Structures
    // ---------------------------------------------------------------------

    /** Represents glow lifetime info for a single block position. */
    public static final class GlowInfo {
        public final long startTick; // Game time when the glow started
        public final long endTick;   // Game time when the glow expires
        public GlowInfo(long startTick, long endTick) {
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }

    /** Tracks all currently glowing blocks on the client. */
    public static final Map<BlockPos, GlowInfo> GLOWING_BLOCKS = new LinkedHashMap<>();

    // ---------------------------------------------------------------------
    // Rendering Constants
    // ---------------------------------------------------------------------

    private static final int FADE_TICKS = 40;               // Duration of fade-in/out animation in ticks
    private static final float MAX_DISTANCE = 32f;          // Max render distance for glows
    private static final float MIN_DISTANCE = 4f;           // Distance where glow brightness stops increasing
    private static final float MAX_BRIGHTNESS = 0.5f;       // Maximum alpha brightness
    private static long nextExpiryTick = Long.MAX_VALUE;    // Optimization for scheduled cleanup

    // ---------------------------------------------------------------------
    // Glow Management
    // ---------------------------------------------------------------------

    /**
     * Adds glowing ore blocks to the renderer for a set duration.
     * Called by the client packet handler when the server sends a glow update.
     */
    public static void addGlowingOres(List<BlockPos> positions, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long now = mc.level.getGameTime();
        long expire = now + durationTicks;

        for (BlockPos pos : positions) {
            GLOWING_BLOCKS.put(pos.immutable(), new GlowInfo(now, expire));
            if (expire < nextExpiryTick) nextExpiryTick = expire; // Track earliest expiry for cleanup
        }
    }

    /** Removes specific ores from the glow list immediately. */
    public static void removeGlowingOres(List<BlockPos> positions) {
        for (BlockPos pos : positions) GLOWING_BLOCKS.remove(pos);
    }

    // ---------------------------------------------------------------------
    // Rendering Logic
    // ---------------------------------------------------------------------

    /**
     * Main render hook that draws all active glowing ore overlays each frame.
     * This runs during the "AFTER_TRANSLUCENT_BLOCKS" render stage for proper blending.
     */
    public static void onRenderWorld(RenderLevelStageEvent event, Frustum frustum, Vec3 cameraPosition,
                                     Minecraft mc, MultiBufferSource.BufferSource buffer, VertexConsumer vc) {
        // Only draw during the correct stage to ensure transparency order
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        if (mc.level == null || GLOWING_BLOCKS.isEmpty()) return;
        long now = mc.level.getGameTime();

        // -----------------------------------------------------------------
        // Cleanup expired entries only when needed
        // -----------------------------------------------------------------
        if (now >= nextExpiryTick) {
            GLOWING_BLOCKS.entrySet().removeIf(e -> e.getValue().endTick < now);
            nextExpiryTick = GLOWING_BLOCKS.values().stream()
                    .mapToLong(g -> g.endTick)
                    .min()
                    .orElse(Long.MAX_VALUE);
        }

        Vec3f color = new Vec3f(1,1,1);

        // -----------------------------------------------------------------
        // Main block rendering loop
        // -----------------------------------------------------------------
        for (var entry : GLOWING_BLOCKS.entrySet()) {
            BlockPos pos = entry.getKey();
            GlowInfo info = entry.getValue();

            BlockState state = mc.level.getBlockState(pos);
            if (!state.is(ModTags.REVELATION_ORES)) continue; // Skip non-ore blocks

            // Step 1: Frustum culling – skip invisible blocks
            if (!ModRenderer.isInFrustum(pos, frustum)) continue;

            // Step 2: Distance-based skip
            double dx = pos.getX() + 0.5 - cameraPosition.x;
            double dy = pos.getY() + 0.5 - cameraPosition.y;
            double dz = pos.getZ() + 0.5 - cameraPosition.z;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > MAX_DISTANCE) continue;

            // -----------------------------------------------------------------
            // Compute visual effects (fade and distance)
            // -----------------------------------------------------------------

            // Fade in/out alpha based on lifetime progress
            float fadeAlpha = 1f;
            if (now < info.startTick + FADE_TICKS) {
                fadeAlpha = (now - info.startTick) / (float) FADE_TICKS; // Fade-in
            } else if (now > info.endTick - FADE_TICKS) {
                fadeAlpha = (info.endTick - now) / (float) FADE_TICKS;   // Fade-out
            }

            // Distance-based attenuation curve
            float distFactor = dist <= MIN_DISTANCE ? 1f :
                    dist >= MAX_DISTANCE ? 0f :
                            1f - (float) Math.pow((dist - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE), 3);

            float alpha = fadeAlpha * distFactor * MAX_BRIGHTNESS;

            ModRenderer.renderBlock(event, pos, cameraPosition, mc, vc, color, alpha, true);
        }
    }
}