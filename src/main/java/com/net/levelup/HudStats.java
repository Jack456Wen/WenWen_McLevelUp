package com.net.levelup;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LevelUp.ID)
public final class HudStats {
    private static final java.util.Set<net.minecraft.server.level.ServerPlayer> SUBSCRIBERS = new java.util.HashSet<>();
    private static long sampleStart;
    private static int sampleTicks;
    private HudStats() { }
    static void subscribe(net.minecraft.server.level.ServerPlayer player, boolean enabled) {
        if (enabled) SUBSCRIBERS.add(player);
        else SUBSCRIBERS.remove(player);
    }
    @SubscribeEvent
    public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SUBSCRIBERS.remove(event.getEntity());
    }
    @SubscribeEvent
    public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event) {
        SUBSCRIBERS.clear();
        sampleStart = 0;
        sampleTicks = 0;
    }
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (SUBSCRIBERS.isEmpty()) { sampleStart = 0; sampleTicks = 0; return; }
        long now = System.nanoTime();
        if (sampleStart == 0) { sampleStart = now; sampleTicks = 0; return; }
        ++sampleTicks;
        long elapsed = now - sampleStart;
        if (elapsed < 1_000_000_000L) return;
        var message = new HudStatsMessage(HudTiming.tps(sampleTicks, elapsed), server.getAverageTickTime());
        sampleStart = now;
        sampleTicks = 0;
        for (var player : SUBSCRIBERS) Network.stats(player, message);
    }
}
