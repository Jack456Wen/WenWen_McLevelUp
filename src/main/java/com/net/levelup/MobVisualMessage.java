package com.net.levelup;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Immutable spawn-time event identity; sent once when a player starts tracking a mob. */
record MobVisualMessage(int entityId, UUID uuid, String name, int rgb, boolean particles) {
    void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId).writeUUID(uuid).writeUtf(name).writeInt(rgb).writeBoolean(particles);
    }
    static MobVisualMessage read(FriendlyByteBuf buffer) {
        return new MobVisualMessage(buffer.readVarInt(), buffer.readUUID(), buffer.readUtf(),
            buffer.readInt(), buffer.readBoolean());
    }
    void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMobVisuals.handle(this));
    }
    static int color(BossEvent.BossBarColor color) {
        return switch (color) {
            case PINK -> 0xFF55FF;
            case BLUE -> 0x5555FF;
            case RED -> 0xFF5555;
            case GREEN -> 0x55FF55;
            case YELLOW -> 0xFFFF55;
            case PURPLE -> 0xAA00AA;
            case WHITE -> 0xFFFFFF;
        };
    }
}
