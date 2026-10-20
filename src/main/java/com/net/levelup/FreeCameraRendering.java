package com.net.levelup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.chunk.ChunkStatus;

/** Rebuilds loaded edge columns when the viewpoint moves independently of chunk delivery. */
public final class FreeCameraRendering {
    private FreeCameraRendering() { }

    public static boolean renderEdgeChunks() {
        return FreeCamera.active() && HudSettings.FREE_CAMERA_RENDER_EDGE_CHUNKS.get();
    }

    public static void moved(int oldX, int oldZ, double x, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (!FreeCamera.active() || mc.level == null) return;
        int radius = mc.options.getEffectiveRenderDistance();
        FreeCameraChunks.moved(oldX, oldZ, FreeCameraChunks.section(x), FreeCameraChunks.section(z), radius,
                (cx, cz) -> mark(mc.level, mc.levelRenderer, cx, cz));
    }

    static void returned(ClientLevel level, double oldX, double oldZ) {
        Minecraft mc = Minecraft.getInstance();
        var entity = mc.getCameraEntity();
        // Do not dirty a new dimension's renderer using the previous world's coordinates.
        if (level != mc.level || entity == null || entity.level() != level) return;
        int radius = mc.options.getEffectiveRenderDistance();
        FreeCameraChunks.returned(FreeCameraChunks.section(oldX), FreeCameraChunks.section(oldZ), radius,
                FreeCameraChunks.section(entity.getX()), FreeCameraChunks.section(entity.getZ()), radius,
                (cx, cz) -> mark(level, mc.levelRenderer, cx, cz));
    }

    private static void mark(ClientLevel level, LevelRenderer renderer, int x, int z) {
        if (level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) == null) return;
        // The reference's fixed 0..15 range misses 1.20.1's negative and upper sections.
        for (int y = level.getMinSection(); y < level.getMaxSection(); y++) renderer.setSectionDirty(x, y, z);
    }
}
