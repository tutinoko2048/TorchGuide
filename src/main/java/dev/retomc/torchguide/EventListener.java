package dev.retomc.torchguide;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
            TRACKER.queuePlacement(pos, Config.UNDO_WINDOW_TICKS.get());
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        TRACKER.setLevel(minecraft.level);
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        while (TOGGLE.consumeClick()) {
            player.displayClientMessage(Component.translatable(
                TRACKER.toggle() ? "message.torchguide.enabled" : "message.torchguide.disabled"
            ), true);
        }

        TRACKER.tick(player.blockPosition(), holdsTorch(player), Config.SPACING.get(), Config.RANGE.get());
    }

    private static boolean holdsTorch(Player player) {
        return player.getMainHandItem().is(Items.TORCH)
            || player.getOffhandItem().is(Items.TORCH);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        List<BlockPos> guides = TRACKER.guides();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || guides.isEmpty()
            || !TRACKER.enabled() || player == null || !holdsTorch(player)) return;

        TorchGuideRenderer.render(event, minecraft, guides);
    }
}
