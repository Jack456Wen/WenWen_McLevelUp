package com.net.levelup;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 事件 BGM 配置。播放由客户端负责（见 {@code ClientBgm}），服务端只按天数决定
 * 什么时候播放、什么时候停止，指令通过 {@link Network} 发给装了本模组的客户端。
 */
public record EventBgm(ResourceLocation sound, float volume, double durationSeconds) {
    public static EventBgm parse(JsonObject day) {
        String name = day.has("bgm") ? day.get("bgm").getAsString() : "";
        double volume = day.has("bgmVolume") ? day.get("bgmVolume").getAsDouble() : 1;
        if (!Double.isFinite(volume) || volume < 0 || volume > 1)
            throw new IllegalArgumentException("bgmVolume must be between 0 and 1");
        double duration = day.has("bgmDurationSeconds") ? day.get("bgmDurationSeconds").getAsDouble() : 180;
        if (!Double.isFinite(duration) || duration < 1 || duration > 86400)
            throw new IllegalArgumentException("bgmDurationSeconds must be between 1 and 86400");
        ResourceLocation sound = name.isBlank() ? null : ResourceLocation.tryParse(name);
        if (!name.isBlank() && sound == null) throw new IllegalArgumentException("Invalid bgm sound ID: " + name);
        // Resource-pack-only sounds need not be registered on the server.
        return new EventBgm(sound, (float)volume, duration);
    }
    public boolean enabled() { return sound != null && volume > 0; }
    public long nextPlayback(long now) { return now + Math.round(durationSeconds * 1_000_000_000L); }
    public static boolean due(long now, Long deadline) { return deadline == null || now - deadline >= 0; }
    /**
     * 让客户端（重新）开始播放事件音乐，同时接管原版背景音乐。
     * @return 指令是否发出；false 表示客户端没装本模组或通道版本不匹配，此时不会播放。
     */
    public boolean play(ServerPlayer player) {
        if (!enabled() || !Network.controlsMusic(player)) return false;
        Network.play(player, sound, volume);
        return true;
    }
    /** 停止事件音乐；客户端随后会立刻恢复原版背景音乐。 */
    public void stop(ServerPlayer player) {
        if (sound != null && Network.controlsMusic(player)) Network.stop(player);
    }
}
