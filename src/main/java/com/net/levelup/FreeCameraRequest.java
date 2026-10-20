package com.net.levelup;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

record FreeCameraRequest(int requestId) {
    void write(FriendlyByteBuf buffer) { buffer.writeVarInt(requestId); }
    static FreeCameraRequest read(FriendlyByteBuf buffer) { return new FreeCameraRequest(buffer.readVarInt()); }
    void handle(Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null && requestId > 0) Network.freeCameraPermission(player, requestId);
    }
}
