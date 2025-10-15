package net.chriskatze.catocraftmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.chriskatze.catocraftmod.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Handles rendering of temporary glowing overlays on ore blocks detected by the Revelation enchantment.
 *
 * This renderer operates entirely client-side, adding translucent overlays to qualifying ores
 * for a limited duration. It supports fade-in/out animations, distance-based brightness falloff,
 * and neighbor/frustum culling for performance.
 */
@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class OreGlowRenderer {

    // ---------------------------------------------------------------------
    // Internal Data Structures
    // ---------------------------------------------------------------------

    /** Represents glow lifetime info for a single block position. */
    private static final class GlowInfo {
        final long startTick; // Game time when the glow started
        final long endTick;   // Game time when the glow expires
        GlowInfo(long startTick, long endTick) {
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }

    /** Tracks all currently glowing blocks on the client. */
    private static final Map<BlockPos, GlowInfo> GLOWING_BLOCKS = new LinkedHashMap<>();

    /** Cache for overlay textures to avoid repeated atlas lookups. */
    private static final Map<ResourceLocation, TextureAtlasSprite> OVERLAY_CACHE = new HashMap<>();

    /** Cached block texture atlas reference. */
    private static TextureAtlas BLOCK_ATLAS;

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
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // Only draw during the correct stage to ensure transparency order
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
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

        // -----------------------------------------------------------------
        // Prepare camera and rendering context
        // -----------------------------------------------------------------
        var camPos = event.getCamera().getPosition();
        double camX = camPos.x, camY = camPos.y, camZ = camPos.z;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        // Prepare frustum once per frame for culling optimization
        var frustum = event.getFrustum();
        if (frustum != null) {
            frustum.prepare(camX, camY, camZ);
        }

        // Ensure block texture atlas is loaded
        if (BLOCK_ATLAS == null) {
            BLOCK_ATLAS = (TextureAtlas) mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
            if (BLOCK_ATLAS == null) return;
        }

        TextureAtlasSprite missingSprite = BLOCK_ATLAS.getSprite(MissingTextureAtlasSprite.getLocation());

        // -----------------------------------------------------------------
        // Main block rendering loop
        // -----------------------------------------------------------------
        for (var entry : GLOWING_BLOCKS.entrySet()) {
            BlockPos pos = entry.getKey();
            GlowInfo info = entry.getValue();

            BlockState state = mc.level.getBlockState(pos);
            if (!state.is(ModTags.REVELATION_ORES)) continue; // Skip non-ore blocks

            // Step 1: Frustum culling – skip invisible blocks
            if (frustum != null) {
                var aabb = new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
                if (!frustum.isVisible(aabb)) continue;
            }

            // Step 2: Distance-based skip
            double dx = pos.getX() + 0.5 - camX;
            double dy = pos.getY() + 0.5 - camY;
            double dz = pos.getZ() + 0.5 - camZ;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > MAX_DISTANCE) continue;

            // Step 3: Retrieve overlay texture
            Block block = state.getBlock();
            ResourceLocation registryName = mc.level.registryAccess()
                    .registryOrThrow(Registries.BLOCK)
                    .getKey(block);
            if (registryName == null) continue;

            ResourceLocation overlayLoc = CatocraftMod.id("block/" + registryName.getPath() + "_overlay");
            TextureAtlasSprite sprite = OVERLAY_CACHE.computeIfAbsent(overlayLoc, BLOCK_ATLAS::getSprite);
            if (sprite == null || sprite == missingSprite) continue;

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
            if (alpha < 0.01f) continue; // Skip nearly invisible blocks

            // -----------------------------------------------------------------
            // Render visible faces only (neighbor culling)
            // -----------------------------------------------------------------
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);

            Matrix4f mat = poseStack.last().pose();
            float offset = 0.008f; // Slight offset to prevent z-fighting

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = mc.level.getBlockState(neighborPos);

                // Skip face if neighbor is solid on that side (occluded)
                if (neighborState.isSolidRender(mc.level, neighborPos)) continue;

                renderDirectionalFace(vc, mat, dir, sprite, alpha, offset);
            }

            poseStack.popPose();
        }

        // Commit all rendering to GPU
        buffer.endBatch();
    }

    // ---------------------------------------------------------------------
    // Face Rendering Helpers
    // ---------------------------------------------------------------------

    /** Renders a single block face in the specified direction with alpha transparency. */
    private static void renderDirectionalFace(VertexConsumer vc, Matrix4f mat, Direction dir,
                                              TextureAtlasSprite sprite, float alpha, float o) {
        switch (dir) {
            case UP -> renderFace(vc, mat, 0f, 1f + o, 0f, 0f, 1f + o, 1f, 1f, 1f + o, 1f, 1f, 1f + o, 0f, 0f, 1f, 0f, sprite, dir, alpha);
            case DOWN -> renderFace(vc, mat, 0f, -o, 0f, 1f, -o, 0f, 1f, -o, 1f, 0f, -o, 1f, 0f, -1f, 0f, sprite, dir, alpha);
            case NORTH -> renderFace(vc, mat, 0f, 0f, -o, 0f, 1f, -o, 1f, 1f, -o, 1f, 0f, -o, 0f, 0f, -1f, sprite, dir, alpha);
            case SOUTH -> renderFace(vc, mat, 1f, 0f, 1f + o, 1f, 1f, 1f + o, 0f, 1f, 1f + o, 0f, 0f, 1f + o, 0f, 0f, 1f, sprite, dir, alpha);
            case EAST -> renderFace(vc, mat, 1f + o, 0f, 0f, 1f + o, 1f, 0f, 1f + o, 1f, 1f, 1f + o, 0f, 1f, 1f, 0f, 0f, sprite, dir, alpha);
            case WEST -> renderFace(vc, mat, -o, 0f, 1f, -o, 1f, 1f, -o, 1f, 0f, -o, 0f, 0f, -1f, 0f, 0f, sprite, dir, alpha);
        }
    }

    /** Low-level helper to render four vertices forming a quad (one face). */
    private static void renderFace(VertexConsumer vc, Matrix4f mat,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float x3, float y3, float z3,
                                   float x4, float y4, float z4,
                                   float nx, float ny, float nz,
                                   TextureAtlasSprite sprite,
                                   Direction face,
                                   float alpha) {

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        // Correct texture orientation for specific faces
        if (face == Direction.NORTH || face == Direction.WEST || face == Direction.SOUTH || face == Direction.EAST) {
            float tmp = u0; u0 = u1; u1 = tmp;
        }
        if (face == Direction.UP || face == Direction.DOWN) {
            float tmp = v0; v0 = v1; v1 = tmp;
        }

        // Render vertex order depends on face orientation
        if (face == Direction.DOWN) {
            vc.addVertex(mat, x1, y1, z1).setColor(1f, 1f, 1f, alpha).setUv(u0, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x2, y2, z2).setColor(1f, 1f, 1f, alpha).setUv(u1, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x3, y3, z3).setColor(1f, 1f, 1f, alpha).setUv(u1, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x4, y4, z4).setColor(1f, 1f, 1f, alpha).setUv(u0, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            return;
        }

        vc.addVertex(mat, x1, y1, z1).setColor(1f, 1f, 1f, alpha).setUv(u0, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(1f, 1f, 1f, alpha).setUv(u0, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(1f, 1f, 1f, alpha).setUv(u1, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x4, y4, z4).setColor(1f, 1f, 1f, alpha).setUv(u1, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
    }

    // ---------------------------------------------------------------------
    // Resource Handling
    // ---------------------------------------------------------------------

    /** Clears cached overlay textures and atlas references (used during resource reload). */
    public static void clearOverlayCache() {
        OVERLAY_CACHE.clear();
        BLOCK_ATLAS = null;
    }

    /**
     * Registers a reload listener to automatically clear cached textures when
     * the game's resource packs are reloaded.
     */
    public static void registerReloadListener() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager() instanceof ReloadableResourceManager reloadable) {
            reloadable.registerReloadListener(new PreparableReloadListener() {
                @Override
                public CompletableFuture<Void> reload(PreparationBarrier barrier,
                                                      ResourceManager resourceManager,
                                                      ProfilerFiller prep,
                                                      ProfilerFiller reload,
                                                      Executor background,
                                                      Executor game) {
                    return CompletableFuture.runAsync(OreGlowRenderer::clearOverlayCache, game)
                            .thenCompose(barrier::wait);
                }
            });
        }
    }
}