package com.net.levelup.mixin;

import com.net.levelup.FreeCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class FreeCameraMouseMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void levelup$turnCamera(LocalPlayer player, double yaw, double pitch) {
        if (FreeCamera.active()) FreeCamera.turn(yaw, pitch);
        else player.turn(yaw, pitch);
    }
}
