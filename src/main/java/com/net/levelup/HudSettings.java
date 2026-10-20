package com.net.levelup;

import net.minecraftforge.common.ForgeConfigSpec;

final class HudSettings {
    enum Mode { FULL, SIMPLE, OFF }
    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.EnumValue<Mode> MODE;
    static final ForgeConfigSpec.IntValue X, Y;
    static final ForgeConfigSpec.BooleanValue FREE_CAMERA_RENDER_EDGE_CHUNKS;
    static {
        var builder = new ForgeConfigSpec.Builder();
        MODE = builder.comment("HUD mode: FULL, SIMPLE, OFF").defineEnum("mode", Mode.FULL);
        X = builder.comment("Horizontal offset in GUI pixels").defineInRange("x", 4, 0, 10000);
        Y = builder.comment("Vertical offset in GUI pixels").defineInRange("y", 4, 0, 10000);
        FREE_CAMERA_RENDER_EDGE_CHUNKS = builder.comment("在自由摄像机模式下，渲染时跳过所有邻近的边缘块（默认禁用）")
                .define("freeCameraRenderEdgeChunks", false);
        SPEC = builder.build();
    }
    private HudSettings() { }
}
