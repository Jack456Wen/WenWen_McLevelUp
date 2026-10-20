package com.net.levelup.mixin;

import com.net.levelup.FreeCamera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class FreeCameraLevelRendererMixin {
    // Equivalent to the reference's temporary spectator flag, restricted to
    // terrain setup instead of changing player.isSpectator() for unrelated code.
    // renderLevel keeps this call site under Sodium-family renderers too, so
    // the spectator flag still reaches their chunk setup.
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"), index = 3, require = 0)
    private boolean levelup$spectatorTerrain(boolean spectator) {
        return FreeCamera.active() || spectator;
    }
}
