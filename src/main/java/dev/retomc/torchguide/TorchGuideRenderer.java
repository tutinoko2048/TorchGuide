package dev.retomc.torchguide;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

final class TorchGuideRenderer {
    private TorchGuideRenderer() {}

    static void render(RenderLevelStageEvent event, Minecraft minecraft, List<BlockPos> guides) {
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS);
        float[] previousColor = RenderSystem.getShaderColor().clone();
        RenderSystem.setShaderColor(1, 1, 1, (float) (1.0 - Config.TRANSPARENCY.get()));
        try {
            for (BlockPos pos : guides) {
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                minecraft.getBlockRenderer().renderSingleBlock(
                    Blocks.TORCH.defaultBlockState(), poseStack, buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, renderType
                );
                poseStack.popPose();
            }
            buffers.endBatch(renderType);
        } finally {
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
        }
    }
}
