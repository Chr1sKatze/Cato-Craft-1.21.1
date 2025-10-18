package net.chriskatze.catocraftmod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class ModRenderer {

    public static final float OFFSET_Z = 0.008f; // Slight offset to prevent z-fighting

    /** Cache for overlay textures to avoid repeated atlas lookups. */
    public static final Map<ResourceLocation, TextureAtlasSprite> OVERLAY_CACHE = new HashMap<>();

    /** Cached block texture atlas reference. */
    public static TextureAtlas BLOCK_ATLAS = null;
    public static TextureAtlasSprite BLOCK_SPRITE = null;

    // ---------------------------------------------------------------------
    // Resource Handling
    // ---------------------------------------------------------------------

    /** Clears cached overlay textures and atlas references (used during resource reload). */
    public static void clearOverlayCache() {
        OVERLAY_CACHE.clear();
        BLOCK_ATLAS = null;
    }

    public static void loadOverlayCache(Minecraft mc) {
        // Ensure block texture atlas is loaded
        if (BLOCK_ATLAS == null) {
            BLOCK_ATLAS = (TextureAtlas) mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
            BLOCK_SPRITE = BLOCK_ATLAS.getSprite(MissingTextureAtlasSprite.getLocation());
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ModRenderer.loadOverlayCache(mc);

        // -----------------------------------------------------------------
        // Prepare rendering context
        // -----------------------------------------------------------------
        final Vec3 cameraPosition = event.getCamera().getPosition();
        final Frustum frustum = event.getFrustum();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
        frustum.prepare(cameraPosition.x, cameraPosition.y, cameraPosition.z);

        // -----------------------------------------------------------------
        // Rendering
        // -----------------------------------------------------------------
        OreGlowRenderer.onRenderWorld(event, frustum, cameraPosition, mc, buffer, vc);
        //LightOverlayRenderer.onRenderWorld(event, frustum, cameraPosition, mc, buffer, vc);

        // Commit all rendering to GPU
        buffer.endBatch();
    }

    // ---------------------------------------------------------------------
    // Face Rendering Helpers
    // ---------------------------------------------------------------------

    public static boolean isInValidSprite(TextureAtlasSprite sprite) {
        return sprite == null || sprite == ModRenderer.BLOCK_SPRITE;
    }

    public static class UVSprite {
        public final Vec2 uv0;
        public final Vec2 uv1;
        public static final UVSprite ZERO = new UVSprite(Vec2.ZERO, Vec2.ZERO);

        public UVSprite(TextureAtlasSprite sprite) {
            if (ModRenderer.isInValidSprite(sprite)) {
                CatocraftMod.LOGGER.info("no sprite!");
                this.uv0 = Vec2.ZERO;
                this.uv1 = Vec2.ZERO;
            }
            else {
                this.uv0 = new Vec2(sprite.getU0(), sprite.getV0());
                this.uv1 = new Vec2(sprite.getU1(), sprite.getV1());
            }
        }
        public UVSprite(Vec2 uv0, Vec2 uv1) {
            this.uv0 = uv0;
            this.uv1 = uv1;
        }
    }

    public static TextureAtlasSprite getSpriteFromResource(Level level, BlockState state) {
        ResourceLocation registryName = level.registryAccess()
                .registryOrThrow(Registries.BLOCK)
                .getKey(state.getBlock());
        if (registryName == null) return null;
        ResourceLocation overlayLoc = CatocraftMod.id("block/" + registryName.getPath() + "_overlay");
        return OVERLAY_CACHE.computeIfAbsent(overlayLoc, BLOCK_ATLAS::getSprite);
    }

    public static boolean isOccluded(Minecraft mc, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = mc.level.getBlockState(neighborPos);
        return neighborState.isSolidRender(mc.level, neighborPos);
    }

    public static void renderBlock(RenderLevelStageEvent event, BlockPos pos, Vec3 cameraPosition,
                                   Minecraft mc, VertexConsumer vc, Vec3f color, float alpha, boolean useCustomSprite) {
        if (alpha < 0.01f) return; // Skip nearly invisible blocks

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        ModRenderer.cameraLookAt(cameraPosition, pos, poseStack);
        Matrix4f mat = poseStack.last().pose();

        BlockState blockState = mc.level.getBlockState(pos);
        BakedModel model = mc.getBlockRenderer().getBlockModel(blockState);
        TextureAtlasSprite sprite = useCustomSprite ? getSpriteFromResource(mc.level, blockState) : null;

        for (Direction dir : Direction.values()) {
            // Skip face if neighbor is solid on that side (occluded)
            if (isOccluded(mc, pos, dir)) continue;

            List<BakedQuad> bakedQuadList = model.getQuads(blockState, dir, mc.level.random);

            if (useCustomSprite && sprite != null) {
                ModRenderer.renderDirectionalFace(vc, mat, dir, new UVSprite(sprite), color, alpha, ModRenderer.OFFSET_Z);
                continue;
            }

            if (bakedQuadList.isEmpty()) continue;

            BakedQuad bakedQuad = bakedQuadList.getFirst();
            sprite = bakedQuad.getSprite();
            ModRenderer.renderDirectionalFace(vc, mat, dir, new UVSprite(sprite), color, alpha, ModRenderer.OFFSET_Z);
        }

        poseStack.popPose();
    }

    public static boolean isInFrustum(BlockPos pos, Frustum frustum) {
        // Step 1: Frustum culling – skip invisible blocks
        final var aabb = new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        return frustum.isVisible(aabb);
    }

    public static void cameraLookAt(Vec3 cameraPosition, BlockPos pos, PoseStack poseStack) {
        poseStack.translate(pos.getX() - cameraPosition.x, pos.getY() - cameraPosition.y, pos.getZ() - cameraPosition.z);
    }

    /** Renders a single block face in the specified direction with alpha transparency. */
    public static void renderDirectionalFace(VertexConsumer vc, Matrix4f mat, Direction dir,
                                             UVSprite uvsprite, Vec3f color, float alpha, float o) {
        Vec2 uv0 = uvsprite.uv0;
        Vec2 uv1 = uvsprite.uv1;

        switch (dir) {
            case UP -> renderFace(vc, mat, 0f, 1f + o, 0f, 0f, 1f + o, 1f, 1f, 1f + o, 1f, 1f, 1f + o, 0f, 0f, 1f, 0f, uv0, uv1, dir, color, alpha);
            case DOWN -> renderFace(vc, mat, 0f, -o, 0f, 1f, -o, 0f, 1f, -o, 1f, 0f, -o, 1f, 0f, -1f, 0f, uv0, uv1, dir, color, alpha);
            case NORTH -> renderFace(vc, mat, 0f, 0f, -o, 0f, 1f, -o, 1f, 1f, -o, 1f, 0f, -o, 0f, 0f, -1f, uv0, uv1, dir, color, alpha);
            case SOUTH -> renderFace(vc, mat, 1f, 0f, 1f + o, 1f, 1f, 1f + o, 0f, 1f, 1f + o, 0f, 0f, 1f + o, 0f, 0f, 1f, uv0, uv1, dir, color, alpha);
            case EAST -> renderFace(vc, mat, 1f + o, 0f, 0f, 1f + o, 1f, 0f, 1f + o, 1f, 1f, 1f + o, 0f, 1f, 1f, 0f, 0f, uv0, uv1, dir, color, alpha);
            case WEST -> renderFace(vc, mat, -o, 0f, 1f, -o, 1f, 1f, -o, 1f, 0f, -o, 0f, 0f, -1f, 0f, 0f, uv0, uv1, dir, color, alpha);
        }
    }

    /** Low-level helper to render four vertices forming a quad (one face). */
    public static void renderFace(VertexConsumer vc, Matrix4f mat,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float x4, float y4, float z4,
                                  float nx, float ny, float nz,
                                  Vec2 uv0,
                                  Vec2 uv1,
                                  Direction face,
                                  Vec3f color,
                                  float alpha) {

        // Correct texture orientation for specific faces
        if (face == Direction.NORTH || face == Direction.WEST || face == Direction.SOUTH || face == Direction.EAST) {
            Vec2 tmp = new Vec2(uv0.x, uv1.y);
            uv0 = new Vec2(uv1.x, uv0.y);
            uv1 = tmp;
        }
        if (face == Direction.UP || face == Direction.DOWN) {
            Vec2 tmp = new Vec2(uv1.x, uv0.y);
            uv0 = new Vec2(uv0.x, uv1.y);
            uv1 = tmp;
        }

        // Render vertex order depends on face orientation
        if (face == Direction.DOWN) {
            vc.addVertex(mat, x1, y1, z1).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv0.x, uv0.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x2, y2, z2).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv1.x, uv0.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x3, y3, z3).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv1.x, uv1.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            vc.addVertex(mat, x4, y4, z4).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv0.x, uv1.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
            return;
        }

        vc.addVertex(mat, x1, y1, z1).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv0.x, uv1.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv0.x, uv0.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x3, y3, z3).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv1.x, uv0.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
        vc.addVertex(mat, x4, y4, z4).setColor(color.getX(), color.getY(), color.getZ(), alpha).setUv(uv1.x, uv1.y).setOverlay(0).setLight(0xF000F0).setNormal(nx, ny, nz);
    }
}
