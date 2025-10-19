package net.chriskatze.catocraftmod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import static net.minecraft.world.item.Items.TORCH;

public class LightOverlayRenderer {

    public static void onRenderWorld(RenderLevelStageEvent event, Frustum frustum, Vec3 cameraPosition,
                                     Minecraft mc, MultiBufferSource.BufferSource buffer, VertexConsumer vc) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        if (!mc.player.getHandSlots().iterator().next().is(TORCH)) {
            return;
        }

        Player player = mc.player;
        assert player != null;

        BlockPos playerPos = player.blockPosition();
        int radius = 5;

        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-radius, -radius, -radius), playerPos.offset(radius, radius, radius))) {
                double distSq = player.position().distanceToSqr(Vec3.atCenterOf(pos));
                float brightness = Mth.clamp(1.0f - (float) distSq / (radius * radius), 0.0f, 1.0f);

            if (brightness >= 0.01f) {
                ModRenderer.renderBlock(event, pos, cameraPosition, mc, vc, new Vec3f(1f,0.75f,0.25f), brightness * 0.75f, false);
            }
        }
    }

}
