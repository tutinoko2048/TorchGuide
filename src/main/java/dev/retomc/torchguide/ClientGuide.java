package dev.retomc.torchguide;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TorchGuide.MODID, value = Dist.CLIENT)
public final class ClientGuide {
    private static final KeyMapping TOGGLE = new KeyMapping(
        "key.torchguide.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.torchguide"
    );

    private static ClientLevel currentLevel;
    private static BlockPos origin;
    private static BlockPos pendingPlacement;
    private static List<BlockPos> guides = List.of();
    private static boolean enabled;
    private static int ticks;

    private ClientGuide() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    public static void onPlacement(UseItemOnBlockEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (enabled && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK
            && event.getLevel() == minecraft.level && event.getPlayer() == minecraft.player
            && event.getItemStack().is(Items.TORCH)) {
            BlockPos pos = new BlockPlaceContext(event.getUseOnContext()).getClickedPos();
            if (!minecraft.level.getBlockState(pos).is(Blocks.TORCH)) {
                pendingPlacement = pos.immutable();
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (currentLevel != minecraft.level) {
            currentLevel = minecraft.level;
            origin = null;
            pendingPlacement = null;
            guides = List.of();
        }
        if (minecraft.player == null || minecraft.level == null) return;

        while (TOGGLE.consumeClick()) {
            enabled = !enabled;
            minecraft.player.displayClientMessage(Component.translatable(
                enabled ? "message.torchguide.enabled" : "message.torchguide.disabled"
            ), true);
        }

        boolean originChanged = false;
        if (pendingPlacement != null) {
            if (enabled && minecraft.level.getBlockState(pendingPlacement).is(Blocks.TORCH)) {
                origin = pendingPlacement;
                originChanged = true;
            }
            pendingPlacement = null;
        }

        if (!enabled || origin == null || !holdsTorch(minecraft)) {
            guides = List.of();
        } else if (originChanged || ++ticks % 5 == 0) {
            guides = findGuides(minecraft);
        }
    }

    private static boolean holdsTorch(Minecraft minecraft) {
        return minecraft.player.getMainHandItem().is(Items.TORCH)
            || minecraft.player.getOffhandItem().is(Items.TORCH);
    }

    private static List<BlockPos> findGuides(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        BlockPos playerPos = minecraft.player.blockPosition();
        int range = Config.RANGE.get();
        int spacing = Config.SPACING.get() + 1;
        int minX = playerPos.getX() - range;
        int minZ = playerPos.getZ() - range;
        int firstX = minX + Math.floorMod(origin.getX() - minX, spacing);
        int firstZ = minZ + Math.floorMod(origin.getZ() - minZ, spacing);
        int topY = Math.min(playerPos.getY() + 4, level.getMaxBuildHeight() - 1);
        int bottomY = Math.max(playerPos.getY() - 8, level.getMinBuildHeight());
        BlockState torch = Blocks.TORCH.defaultBlockState();
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();
        List<BlockPos> positions = new ArrayList<>();

        for (int x = firstX; x <= playerPos.getX() + range; x += spacing) {
            for (int z = firstZ; z <= playerPos.getZ() + range; z += spacing) {
                int dx = x - playerPos.getX();
                int dz = z - playerPos.getZ();
                if (dx * dx + dz * dz > range * range) continue;
                candidate.set(x, playerPos.getY(), z);
                if (!level.hasChunkAt(candidate)) continue;

                for (int y = topY; y >= bottomY; y--) {
                    candidate.set(x, y, z);
                    if (level.getBlockState(candidate).canBeReplaced()
                        && level.getFluidState(candidate).isEmpty()
                        && torch.canSurvive(level, candidate)) {
                        positions.add(candidate.immutable());
                        break;
                    }
                }
            }
        }
        return positions;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || guides.isEmpty()
            || !enabled || minecraft.player == null || !holdsTorch(minecraft)) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS);
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
