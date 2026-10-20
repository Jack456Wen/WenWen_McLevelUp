package com.net.levelup;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

record FreeCameraPermission(int requestId, boolean allowed) {
    void write(FriendlyByteBuf buffer) { buffer.writeVarInt(requestId); buffer.writeBoolean(allowed); }
    static FreeCameraPermission read(FriendlyByteBuf buffer) {
        return new FreeCameraPermission(buffer.readVarInt(), buffer.readBoolean());
    }
    void handle(Supplier<NetworkEvent.Context> context) {
        var connection = context.get().getNetworkManager();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FreeCamera.permission(connection, requestId, allowed));
    }
}
