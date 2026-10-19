package com.net.levelup;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

record HudSubscriptionMessage(boolean enabled) {
    void write(FriendlyByteBuf buffer) { buffer.writeBoolean(enabled); }
    static HudSubscriptionMessage read(FriendlyByteBuf buffer) {
        return new HudSubscriptionMessage(buffer.readBoolean());
    }
    void handle(Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null) HudStats.subscribe(player, enabled);
    }
}
