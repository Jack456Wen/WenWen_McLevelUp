package com.net.levelup.mixin;

import com.net.levelup.FreeCameraRendering;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class FreeCameraRenderChunkMixin {
    @Inject(method = "doesChunkExistAt", at = @At("HEAD"), cancellable = true, require = 0)
    private void levelup$allowLoadedEdges(CallbackInfoReturnable<Boolean> ci) {
        if (FreeCameraRendering.renderEdgeChunks()) ci.setReturnValue(true);
    }
}
