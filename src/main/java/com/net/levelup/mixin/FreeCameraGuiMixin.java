package com.net.levelup.mixin;

import com.net.levelup.FreeCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public abstract class FreeCameraGuiMixin {
    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void levelup$showBodyInventory(CallbackInfoReturnable<Player> ci) {
        if (FreeCamera.active()) ci.setReturnValue(Minecraft.getInstance().player);
    }
}
