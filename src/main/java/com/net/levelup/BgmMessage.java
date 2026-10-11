package com.net.levelup;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端的事件音乐指令。{@code play} 为 false 时表示停止事件音乐
 * 并让客户端恢复原版背景音乐。
 */
record BgmMessage(boolean play, ResourceLocation sound, float volume) {
    static final BgmMessage STOP = new BgmMessage(false, null, 0);

    static BgmMessage play(ResourceLocation sound, float volume) {
        return new BgmMessage(true, sound, volume);
    }
    void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(play);
        if (play) {
            buffer.writeResourceLocation(sound);
            buffer.writeFloat(volume);
        }
    }
    static BgmMessage read(FriendlyByteBuf buffer) {
        return buffer.readBoolean()
            ? play(buffer.readResourceLocation(), buffer.readFloat()) : STOP;
    }
    void handle(Supplier<NetworkEvent.Context> context) {
        // 这个包只发给装了模组的客户端；DistExecutor 保证专用服务端不会加载客户端类。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBgm.handle(this));
    }
}
