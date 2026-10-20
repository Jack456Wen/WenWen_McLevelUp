package com.net.levelup.mixin;

import com.net.levelup.FreeCamera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class FreeCameraMinecraftMixin {
    @Inject(method = {"continueAttack", "startUseItem", "pickBlock"}, at = @At("HEAD"), cancellable = true)
    private void levelup$blockActions(CallbackInfo ci) {
        if (FreeCamera.active()) ci.cancel();
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void levelup$blockAttack(CallbackInfoReturnable<Boolean> ci) {
        if (FreeCamera.active()) ci.setReturnValue(false);
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void levelup$leaveWorld(CallbackInfo ci) {
        FreeCamera.disable();
    }
}
