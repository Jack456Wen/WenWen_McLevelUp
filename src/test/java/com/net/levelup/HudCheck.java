package com.net.levelup;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

final class HudCheck {
    static void run() {
        check(HudTiming.tps(20, 1_000_000_000L) == 20, "normal ticks");
        check(HudTiming.tps(10, 1_000_000_000L) == 10, "slow server");
        check(HudTiming.tps(20, 2_000_000_000L) == 10, "stall included in elapsed time");
        check(HudTiming.tps(40, 1_000_000_000L) == 20, "catch-up capped");
        check(HudTiming.tps(0, 1) == 0 && HudTiming.tps(1, 0) == 0, "empty sample");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var original = new HudStatsMessage(12.5f, 30.0f);
            original.write(buffer);
            check(original.equals(HudStatsMessage.read(buffer)), "TPS and MSPT independent round trip");
            for (boolean enabled : new boolean[] {true, false}) {
                var subscription = new HudSubscriptionMessage(enabled);
                subscription.write(buffer);
                check(subscription.equals(HudSubscriptionMessage.read(buffer)), "subscription round trip");
            }
        } finally { buffer.release(); }
        System.out.println("Passed HUD tick-rate sampling and packet checks.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
