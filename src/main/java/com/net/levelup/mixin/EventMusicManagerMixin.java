package com.net.levelup.mixin;

import com.net.levelup.ClientBgm;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs before Biome Music's priority-5 tick/fade injections. */
@Mixin(value = MusicManager.class, priority = 2000)
abstract class EventMusicManagerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void levelup$pauseBackgroundManager(CallbackInfo ci) {
        if (ClientBgm.controlsBackgroundMusic()) ci.cancel();
    }
    @Inject(method = "startPlaying", at = @At("HEAD"), cancellable = true)
    private void levelup$blockBackgroundStart(Music music, CallbackInfo ci) {
        if (ClientBgm.controlsBackgroundMusic()) ci.cancel();
    }
}
