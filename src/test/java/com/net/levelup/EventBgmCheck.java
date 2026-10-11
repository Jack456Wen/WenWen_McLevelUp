package com.net.levelup;

import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

final class EventBgmCheck {
    static void run() {
        check(parse("{}").sound() == null, "legacy events have BGM disabled");
        var custom = parse("{\"bgm\":\"levelup:music.blood_moon\",\"bgmVolume\":0.7}");
        check(custom.sound().toString().equals("levelup:music.blood_moon") && custom.volume() == 0.7f,
            "resource-pack sound ID and custom volume");
        check(parse("{\"bgm\":\"minecraft:music_disc.13\"}").volume() == 1, "default volume");
        check(parse("{\"bgm\":\"\"}").sound() == null, "empty name disables BGM");
        rejected("{\"bgm\":\"INVALID ID\"}");
        rejected("{\"bgmVolume\":-1}");
        rejected("{\"bgmVolume\":1.1}");
        check(parse("{}").durationSeconds() == 180, "legacy duration defaults to 180 seconds");
        var loop = parse("{\"bgm\":\"levelup:music.blood_moon\",\"bgmDurationSeconds\":1.25}");
        long start = 5_000_000_000L;
        long deadline = loop.nextPlayback(start);
        check(deadline == 6_250_000_000L, "fractional duration uses real elapsed time");
        check(EventBgm.due(start, null) && !EventBgm.due(deadline - 1, deadline)
            && EventBgm.due(deadline, deadline) && EventBgm.due(deadline + 5_000_000_000L, deadline), "first playback and repeat deadline boundaries");
        check(!parse("{}").enabled() && !parse("{\"bgm\":\"minecraft:music_disc.13\",\"bgmVolume\":0}").enabled(), "disabled and muted sounds are not scheduled");
        rejected("{\"bgmDurationSeconds\":0}");
        rejected("{\"bgmDurationSeconds\":86401}");
        // 自定义发包的编解码（服务端发、客户端收），不需要 Minecraft 启动环境。
        BgmMessage play = BgmMessage.play(new ResourceLocation("levelup:music.blood_moon"), 0.7F);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        play.write(buffer);
        check(buffer.readableBytes() > 0, "BGM play message is not empty");
        check(BgmMessage.read(buffer).equals(play), "BGM play message round-trips");
        FriendlyByteBuf stopBuffer = new FriendlyByteBuf(Unpooled.buffer());
        BgmMessage.STOP.write(stopBuffer);
        check(BgmMessage.read(stopBuffer).equals(BgmMessage.STOP), "BGM stop message round-trips");
        System.out.println("Passed 16 event BGM configuration, loop scheduling and packet checks.");
    }
    private static EventBgm parse(String text) { return EventBgm.parse(JsonParser.parseString(text).getAsJsonObject()); }
    private static void rejected(String text) {
        try { parse(text); throw new AssertionError("Invalid BGM configuration accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
