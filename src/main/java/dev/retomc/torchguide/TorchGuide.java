package dev.retomc.torchguide;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

@Mod(value = TorchGuide.MODID, dist = Dist.CLIENT)
public class TorchGuide {
    public static final String MODID = "torchguide";

    public TorchGuide(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (container, parent) -> new ConfigurationScreen(container, parent, TransparencyConfigScreen::new));
    }

    private static final class TransparencyConfigScreen extends ConfigurationScreen.ConfigurationSectionScreen {
        private TransparencyConfigScreen(Screen parent, ModConfig.Type type, ModConfig config, Component title) {
            super(parent, type, config, title);
        }

        @Override
        protected Element createDoubleValue(@Nonnull String key, @Nonnull ValueSpec spec,
                                            @Nonnull Supplier<Double> source, @Nonnull Consumer<Double> target) {
            if (!key.equals("transparency")) return super.createDoubleValue(key, spec, source, target);
            return new Element(getTranslationComponent(key), getTooltipComponent(key, null),
                new OptionInstance<>(getTranslationKey(key), getTooltip(key, spec.getRange()),
                    (caption, value) -> Component.literal(Double.toString(value / 10.0)),
                    new OptionInstance.IntRange(1, 10), null, (int) Math.round(source.get() * 10), value -> {
                        if (value != (int) Math.round(source.get() * 10)) {
                            undoManager.add(previous -> {
                                target.accept(previous / 10.0);
                                onChanged(key);
                            }, value, previous -> {
                                target.accept(previous / 10.0);
                                onChanged(key);
                            }, (int) Math.round(source.get() * 10));
                        }
                    }));
        }
    }
}
