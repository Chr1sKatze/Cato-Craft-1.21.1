package net.chriskatze.catocraftmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.catocraftmod.CatocraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = CatocraftMod.MOD_ID, value = Dist.CLIENT)
public class OreGlowRenderer {

    private static final Map<BlockPos, Long> GLOWING_BLOCKS = new HashMap<>();
    private static final int GLOW_DURATION_TICKS = 60;

    /** Add glowing ore positions to render list */
    public static void addGlowingOres(List<BlockPos> positions) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long expire = mc.level.getGameTime() + GLOW_DURATION_TICKS;
        for (BlockPos pos : positions) {
            GLOWING_BLOCKS.put(pos.immutable(), expire);
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
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        RandomSource random = mc.level.getRandom();

        for (BlockPos pos : GLOWING_BLOCKS.keySet()) {
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();
            ResourceLocation blockId = mc.level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                    .getKey(block);

            if (blockId == null) continue;

            poseStack.pushPose();
            poseStack.translate(
                    pos.getX() - event.getCamera().getPosition().x,
                    pos.getY() - event.getCamera().getPosition().y,
                    pos.getZ() - event.getCamera().getPosition().z
            );

            // 1️⃣ Render the normal base block
            dispatcher.renderSingleBlock(state, poseStack, buffer, 0xF000F0, 0);

            // 2️⃣ Load the emissive model
            ResourceLocation base = ResourceLocation.tryParse(CatocraftMod.MOD_ID + ":block/" + blockId.getPath() + "_emissive");
            ModelResourceLocation emissiveModelLoc = new ModelResourceLocation(base, "normal");
            BakedModel emissiveModel = mc.getModelManager().getModel(emissiveModelLoc);

            // 3️⃣ Prepare the texture for RenderType.eyes()
            ResourceLocation emissiveTexture = base;

            if (emissiveModel != null) {
                VertexConsumer vc = buffer.getBuffer(RenderType.eyes(emissiveTexture));
                var pose = poseStack.last();

                // Slight offset to reduce z-fighting
                poseStack.translate(0.0003, 0.0003, 0.0003);

                // Render all quads per face
                for (Direction dir : Direction.values()) {
                    var quads = emissiveModel.getQuads(state, dir, random);
                    for (var quad : quads) {
                        vc.putBulkData(pose, quad, 1f, 1f, 1f, 1f, 0xF000F0, 0);
                    }
                }

                // Render quads with no specific face
                for (var quad : emissiveModel.getQuads(state, null, random)) {
                    vc.putBulkData(pose, quad, 1f, 1f, 1f, 1f, 0xF000F0, 0);
                }
            }

            poseStack.popPose();
        }

        buffer.endBatch();
    }
}