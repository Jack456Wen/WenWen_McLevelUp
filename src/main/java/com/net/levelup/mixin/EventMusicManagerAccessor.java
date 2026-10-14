package com.net.levelup.mixin;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicManager.class)
public interface EventMusicManagerAccessor {
    @Accessor("nextSongDelay") void levelup$setNextSongDelay(int ticks);
}
