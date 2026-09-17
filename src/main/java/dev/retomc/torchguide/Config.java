package dev.retomc.torchguide;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SPACING = BUILDER
        .comment("Number of empty blocks between torch guides")
        .defineInRange("spacing", 4, 1, 32);

    public static final ModConfigSpec.DoubleValue TRANSPARENCY = BUILDER
        .comment("Torch guide transparency: 0.1 is almost opaque, 1.0 is invisible")
        .defineInRange("transparency", 0.6, 0.1, 1.0);

    public static final ModConfigSpec.IntValue RANGE = BUILDER
        .comment("Horizontal guide render radius in blocks")
        .defineInRange("range", 32, 4, 64);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
