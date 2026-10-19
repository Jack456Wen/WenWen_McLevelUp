package com.net.levelup;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

record HudStatsMessage(float tps, float mspt) {
    void write(FriendlyByteBuf buffer) { buffer.writeFloat(tps); buffer.writeFloat(mspt); }
    static HudStatsMessage read(FriendlyByteBuf buffer) { return new HudStatsMessage(buffer.readFloat(), buffer.readFloat()); }
    void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHud.stats(tps, mspt));
    }
}
