package com.net.levelup;

final class HudTiming {
    static float tps(int ticks, long elapsedNanos) {
        if (ticks <= 0 || elapsedNanos <= 0) return 0;
        return (float)Math.min(20.0, ticks * 1_000_000_000.0 / elapsedNanos);
    }
    private HudTiming() { }
}
