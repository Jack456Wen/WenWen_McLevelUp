package com.net.levelup.mixin;

import com.net.levelup.FreeCamera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class FreeCameraGameRendererMixin {
    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void levelup$stableCameraFov(CallbackInfoReturnable<Double> ci) {
        // Tweakeroo uses the configured FOV directly in free camera mode.
        if (FreeCamera.active()) ci.setReturnValue(Minecraft.getInstance().options.fov().get().doubleValue());
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void levelup$stableCameraView(CallbackInfo ci) {
        if (FreeCamera.active()) ci.cancel();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void levelup$hideHand(CallbackInfo ci) {
        if (FreeCamera.active()) ci.cancel();
    }
}
