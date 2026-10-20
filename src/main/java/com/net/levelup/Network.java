package com.net.levelup;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本模组的网络通道：服务端把「播放/停止事件音乐」的指令发给同样装了本模组的客户端，
 * 由客户端自己接管原版背景音乐（见 {@code ClientBgm}）。没装模组的客户端不在这条通道上，
 * 仍然使用原版声音封包方案（见 {@link EventBgm}）。
 */
final class Network {
    private static final String VERSION = "5";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(LevelUp.ID, "music"))
        .networkProtocolVersion(() -> VERSION)
        // 允许对面没装模组（纯原版客户端）连接，通道缺失时自动回退到原版封包。
        .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(VERSION))
        .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(VERSION))
        .simpleChannel();

    private Network() { }

    /** 在模组构造阶段调用（两侧都要），否则 Forge 会拒绝在注册期之后创建通道。 */
    static void register() {
        CHANNEL.messageBuilder(BgmMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(BgmMessage::write)
            .decoder(BgmMessage::read)
            .consumerMainThread(BgmMessage::handle)
            .add();
        // 客户端专用逻辑：DistExecutor 只在物理客户端加载 ClientBgm，专用服务端不会碰到这个类。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBgm::register);
        CHANNEL.messageBuilder(MobVisualMessage.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MobVisualMessage::write).decoder(MobVisualMessage::read)
            .consumerMainThread(MobVisualMessage::handle).add();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientMobVisuals::register);
        CHANNEL.messageBuilder(HudStatsMessage.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(HudStatsMessage::write).decoder(HudStatsMessage::read)
            .consumerMainThread(HudStatsMessage::handle).add();
        CHANNEL.messageBuilder(HudSubscriptionMessage.class, 3, NetworkDirection.PLAY_TO_SERVER)
            .encoder(HudSubscriptionMessage::write).decoder(HudSubscriptionMessage::read)
            .consumerMainThread(HudSubscriptionMessage::handle).add();
        CHANNEL.messageBuilder(FreeCameraRequest.class, 4, NetworkDirection.PLAY_TO_SERVER)
            .encoder(FreeCameraRequest::write).decoder(FreeCameraRequest::read)
            .consumerMainThread(FreeCameraRequest::handle).add();
        CHANNEL.messageBuilder(FreeCameraPermission.class, 5, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FreeCameraPermission::write).decoder(FreeCameraPermission::read)
            .consumerMainThread(FreeCameraPermission::handle).add();
    }
    /** 该玩家的客户端装了本模组、并协商出了这条通道时，音乐可以交给客户端精确控制。 */
    static boolean controlsMusic(ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.connection.connection);
    }
    static void stats(ServerPlayer player, HudStatsMessage message) {
        if (controlsMusic(player)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    static void subscribe(net.minecraft.network.Connection connection, boolean enabled) {
        if (CHANNEL.isRemotePresent(connection)) CHANNEL.sendToServer(new HudSubscriptionMessage(enabled));
    }
    static boolean requestFreeCamera(net.minecraft.network.Connection connection, int requestId) {
        if (!CHANNEL.isRemotePresent(connection)) return false;
        CHANNEL.sendToServer(new FreeCameraRequest(requestId));
        return true;
    }
    static void freeCameraPermission(ServerPlayer player, int requestId) {
        if (controlsMusic(player)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FreeCameraPermission(requestId, FreeCameraAccess.allowed(player)));
    }
    static void play(ServerPlayer player, ResourceLocation sound, float volume) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), BgmMessage.play(sound, volume));
    }
    static void stop(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), BgmMessage.STOP);
    }
    static void visual(ServerPlayer player, net.minecraft.world.entity.Mob mob, net.minecraft.nbt.CompoundTag data) {
        if (controlsMusic(player)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new MobVisualMessage(mob.getId(), mob.getUUID(), data.getString("eventName"),
                data.getInt("eventColor"), data.getBoolean("mobParticles")));
    }
}
