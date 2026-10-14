package com.net.levelup.mixin;

import com.net.levelup.ClientBgm;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block other MUSIC before playback/fade hooks can start another track. */
@Mixin(value = SoundEngine.class, priority = 2000)
abstract class EventMusicEngineMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void levelup$blockOtherMusic(SoundInstance sound, CallbackInfo ci) {
        if (ClientBgm.blocks(sound)) ci.cancel();
    }
}
