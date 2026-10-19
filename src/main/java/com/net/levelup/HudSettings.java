package com.net.levelup;

import net.minecraftforge.common.ForgeConfigSpec;

final class HudSettings {
    enum Mode { FULL, SIMPLE, OFF }
    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.EnumValue<Mode> MODE;
    static final ForgeConfigSpec.IntValue X, Y;
    static {
        var builder = new ForgeConfigSpec.Builder();
        MODE = builder.comment("HUD mode: FULL, SIMPLE, OFF").defineEnum("mode", Mode.FULL);
        X = builder.comment("Horizontal offset in GUI pixels").defineInRange("x", 4, 0, 10000);
        Y = builder.comment("Vertical offset in GUI pixels").defineInRange("y", 4, 0, 10000);
        SPEC = builder.build();
    }
    private HudSettings() { }
}
