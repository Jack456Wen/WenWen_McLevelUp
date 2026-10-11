package com.net.levelup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * 装了本模组的客户端：事件音乐由客户端自己播放，并且接管原版背景音乐，
 * 因此不会出现「事件音乐 + 原版背景音乐」叠加。
 * 这个类是纯客户端类，只由 {@link Network#register} 通过 DistExecutor 加载。
 */
final class ClientBgm {
    /** 当前正在播放的事件音乐，用来把自己从「屏蔽原版音乐」里排除出去。 */
    private static SoundInstance playing;
    /** 事件音乐播放期间为 true：MUSIC 音轨上除 {@link #playing} 之外的声音全部拦掉。 */
    private static boolean suppressing;

    private ClientBgm() { }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(ClientBgm::onPlaySound);
        MinecraftForge.EVENT_BUS.addListener(ClientBgm::onLoggingOut);
    }
    static void handle(BgmMessage message) {
        if (message.play()) play(message.sound(), message.volume());
        else stop();
    }
    private static void play(ResourceLocation sound, float volume) {
        Minecraft minecraft = Minecraft.getInstance();
        // 先打开屏蔽，之后任何新开始的 MUSIC 声音都会被 onPlaySound 拦掉。
        suppressing = true;
        // 清掉当前正在播放的音乐：原版背景音乐、其它模组的音乐，以及上一遍的事件音乐。
        minecraft.getSoundManager().stop(null, SoundSource.MUSIC);
        // 让 MusicManager 知道它那首已经没了：它之后会等 10～20 分钟再挑一首，而那次挑选会被拦掉。
        minecraft.getMusicManager().stopPlaying();
        // 用带音量的音乐实例，而不是 MusicManager#startPlaying（那条路音量固定为 1，会丢掉 bgmVolume）。
        playing = new SimpleSoundInstance(sound, SoundSource.MUSIC, volume, 1.0F,
            SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
            0.0D, 0.0D, 0.0D, true);
        minecraft.getSoundManager().play(playing);
    }
    private static void stop() {
        // 没接管过就不要碰 MusicManager：否则 startPlaying 会和它自己那首叠在一起。
        if (!suppressing) return;
        suppressing = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (playing != null) {
            minecraft.getSoundManager().stop(playing);
            playing = null;
        }
        if (minecraft.player != null && minecraft.level != null) {
            // 主动让 MusicManager 立刻开始当前情境的原版音乐：不然它要等自己 10～20 分钟的随机间隔，
            // 事件结束后会出现一段莫名其妙的安静。
            minecraft.getMusicManager().stopPlaying();
            minecraft.getMusicManager().startPlaying(minecraft.getSituationalMusic());
        }
    }
    /** 事件期间只允许本模组的事件音乐占用 MUSIC 音轨，其它音乐（含原版背景音乐）直接拦掉。 */
    private static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (suppressing && sound != null && sound != playing && sound.getSource() == SoundSource.MUSIC)
            event.setSound(null);
    }
    /** 退出世界/服务器时复位，避免下次进服还在屏蔽原版背景音乐。 */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        suppressing = false;
        playing = null;
    }
}
