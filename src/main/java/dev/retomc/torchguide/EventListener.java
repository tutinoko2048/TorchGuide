package dev.retomc.torchguide;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TorchGuide.MODID, value = Dist.CLIENT)
public final class EventListener {
    private static final KeyMapping TOGGLE = new KeyMapping(
        "key.torchguide.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.torchguide"
    );

    private static final TorchGuideTracker TRACKER = new TorchGuideTracker();

    private EventListener() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    public static void onPlacement(UseItemOnBlockEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (TRACKER.enabled() && event.getUsePhase() == UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK
            && event.getLevel() == minecraft.level && event.getPlayer() == minecraft.player
            && event.getItemStack().is(Items.TORCH)) {
            TRACKER.setLevel(minecraft.level);
            BlockPos pos = new BlockPlaceContext(event.getUseOnContext()).getClickedPos();
            TRACKER.queuePlacement(pos);
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        TRACKER.setLevel(minecraft.level);
        if (minecraft.player == null || minecraft.level == null) return;

        while (TOGGLE.consumeClick()) {
            minecraft.player.displayClientMessage(Component.translatable(
                TRACKER.toggle() ? "message.torchguide.enabled" : "message.torchguide.disabled"
            ), true);
        }

        TRACKER.tick(minecraft.player.blockPosition(), holdsTorch(minecraft), Config.SPACING.get(), Config.RANGE.get());
    }

    private static boolean holdsTorch(Minecraft minecraft) {
        return minecraft.player.getMainHandItem().is(Items.TORCH)
            || minecraft.player.getOffhandItem().is(Items.TORCH);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        List<BlockPos> guides = TRACKER.guides();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || guides.isEmpty()
            || !TRACKER.enabled() || minecraft.player == null || !holdsTorch(minecraft)) return;

        TorchGuideRenderer.render(event, minecraft, guides);
    }
}
