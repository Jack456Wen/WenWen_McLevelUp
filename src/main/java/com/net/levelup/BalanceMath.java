package com.net.levelup;

/** Pure arithmetic, kept separate so time and probability boundaries can be tested without a server. */
public final class BalanceMath {
    public static long day(long time) { return Math.floorDiv(time, 24000L) + 1; }
    public static float remaining(long time) { return (float)(1 - Math.floorMod(time, 24000L) / 24000.0); }
    public static double weight(double base, double rarity, double strength) {
        return base * Math.pow(Math.min(100, strength), rarity);
    }
    public static int experience(int original, double strength, double multiplier, int cap) {
        return experience(original, strength, multiplier, 1, cap);
    }
    public static int experience(int original, double strength, double globalMultiplier, double eventMultiplier, int cap) {
        return (int)Math.max(0, Math.min(cap, Math.round(original * strength * globalMultiplier * eventMultiplier)));
    }
    private BalanceMath() {}
}
