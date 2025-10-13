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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class OreGlowRenderer {

    private static final Map<BlockPos, Long> GLOWING_BLOCKS = new HashMap<>();
    private static final int GLOW_DURATION_TICKS = 60;

    private static final Map<ResourceLocation, TextureAtlasSprite> OVERLAY_CACHE = new HashMap<>();

    public static void addGlowingOres(List<BlockPos> positions, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long expire = mc.level.getGameTime() + durationTicks;

        for (BlockPos pos : positions) {
            Long existing = GLOWING_BLOCKS.get(pos);
            if (existing == null || existing < expire) {
                GLOWING_BLOCKS.put(pos.immutable(), expire);
            }
        }
    }

    // HELPER
    public static void removeGlowingOres(List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            GLOWING_BLOCKS.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long time = mc.level.getGameTime();
        GLOWING_BLOCKS.entrySet().removeIf(e -> e.getValue() < time);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        TextureAtlas atlas = (TextureAtlas) mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (atlas == null) return;

        // Pre-fetch the missing texture sprite once
        TextureAtlasSprite missingSprite = atlas.getSprite(MissingTextureAtlasSprite.getLocation());

        for (BlockPos pos : GLOWING_BLOCKS.keySet()) {
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();
            if (block == null) continue;

            // ✅ Skip blocks not in EMISSIVE_ORES tag
            if (!state.is(ModTags.EMISSIVE_ORES)) continue;

            ResourceLocation registryName = mc.level.registryAccess()
                    .registryOrThrow(Registries.BLOCK)
                    .getKey(block);
            if (registryName == null) continue;

            ResourceLocation overlayLoc = CatocraftMod.id("block/" + registryName.getPath() + "_overlay");
            TextureAtlasSprite sprite = OVERLAY_CACHE.computeIfAbsent(overlayLoc, atlas::getSprite);

            // Skip missing sprites
            if (sprite == null || sprite == missingSprite) continue;

            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - event.getCamera().getPosition().x,
                    pos.getY() - event.getCamera().getPosition().y,
                    pos.getZ() - event.getCamera().getPosition().z
            );

            VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
            Matrix4f mat = poseStack.last().pose();
            float o = 0.008f;

            for (Direction dir : Direction.values()) {
                switch (dir) {
                    case UP -> renderFace(vc, mat,
                            0f, 1f + o, 0f,
                            0f, 1f + o, 1f,
                            1f, 1f + o, 1f,
                            1f, 1f + o, 0f,
                            0f, 1f, 0f,
                            sprite, dir);

                    case DOWN -> renderFace(vc, mat,
                            0f, 0f - o, 0f,
                            1f, 0f - o, 0f,
                            1f, 0f - o, 1f,
                            0f, 0f - o, 1f,
                            0f, -1f, 0f,
                            sprite, dir);

                    case NORTH -> renderFace(vc, mat,
                            0f, 0f, -o,
                            0f, 1f, -o,
                            1f, 1f, -o,
                            1f, 0f, -o,
                            0f, 0f, -1f,
                            sprite, dir);

                    case SOUTH -> renderFace(vc, mat,
                            1f, 0f, 1f + o,
                            1f, 1f, 1f + o,
                            0f, 1f, 1f + o,
                            0f, 0f, 1f + o,
                            0f, 0f, 1f,
                            sprite, dir);

                    case EAST -> renderFace(vc, mat,
                            1f + o, 0f, 0f,
                            1f + o, 1f, 0f,
                            1f + o, 1f, 1f,
                            1f + o, 0f, 1f,
                            1f, 0f, 0f,
                            sprite, dir);

                    case WEST -> renderFace(vc, mat,
                            -o, 0f, 1f,
                            -o, 1f, 1f,
                            -o, 1f, 0f,
                            -o, 0f, 0f,
                            -1f, 0f, 0f,
                            sprite, dir);
                }
            }

            poseStack.popPose();
        }

        buffer.endBatch();
    }

    private static void renderFace(VertexConsumer vc, Matrix4f mat,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float x3, float y3, float z3,
                                   float x4, float y4, float z4,
                                   float nx, float ny, float nz,
                                   TextureAtlasSprite sprite,
                                   Direction face) {

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        if (face == Direction.NORTH || face == Direction.WEST) {
            float tmp = u0; u0 = u1; u1 = tmp;
        }
        if (face == Direction.SOUTH || face == Direction.EAST) {
            float tmp = u0; u0 = u1; u1 = tmp;
        }
        if (face == Direction.UP || face == Direction.DOWN) {
            float tmp = v0; v0 = v1; v1 = tmp;
        }

        if (face == Direction.DOWN) {
            vc.addVertex(mat, x1, y1, z1).setColor(1f, 1f, 1f, 1f).setUv(u0, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x2, y2, z2).setColor(1f, 1f, 1f, 1f).setUv(u1, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x3, y3, z3).setColor(1f, 1f, 1f, 1f).setUv(u1, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x4, y4, z4).setColor(1f, 1f, 1f, 1f).setUv(u0, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            return;
        }

        vc.addVertex(mat, x1, y1, z1).setColor(1f, 1f, 1f, 1f).setUv(u0, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(1f, 1f, 1f, 1f).setUv(u0, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(1f, 1f, 1f, 1f).setUv(u1, v0).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x4, y4, z4).setColor(1f, 1f, 1f, 1f).setUv(u1, v1).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
    }

    public static void clearOverlayCache() {
        OVERLAY_CACHE.clear();
    }


    public static void registerReloadListener() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager() instanceof ReloadableResourceManager reloadable) {
            reloadable.registerReloadListener(new PreparableReloadListener() {
                @Override
                public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                                      ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                                      Executor backgroundExecutor, Executor gameExecutor) {
                    return CompletableFuture.runAsync(OreGlowRenderer::clearOverlayCache, gameExecutor)
                            .thenCompose(barrier::wait);
                }
            });
        }
    }
}