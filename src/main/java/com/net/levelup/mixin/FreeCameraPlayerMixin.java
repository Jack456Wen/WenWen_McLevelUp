package com.net.levelup.mixin;

import com.net.levelup.FreeCamera;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class FreeCameraPlayerMixin {
    // Keep body physics and normal position packets active while the viewpoint is detached.
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void levelup$keepBodyActive(CallbackInfoReturnable<Boolean> ci) {
        if (FreeCamera.active()) ci.setReturnValue(true);
    }
}
