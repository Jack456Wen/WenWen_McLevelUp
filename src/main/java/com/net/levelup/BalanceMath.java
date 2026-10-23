package com.net.levelup;

/** Pure arithmetic, kept separate so time and probability boundaries can be tested without a server. */
public final class BalanceMath {
    /** Fixed weakening below 1, uniform spawn-time strengthening above 1; random is in [0, 1). */
    public static double randomMultiplier(double maximum, double random) {
        return maximum <= 1 ? maximum : 1 + random * (maximum - 1);
    }
    public static long day(long time) { return Math.floorDiv(time, 24000L) + 1; }
    public static float remaining(long time) { return (float)(1 - Math.floorMod(time, 24000L) / 24000.0); }
    /** Reward progress within the configured strengthening range; no strengthening means no bonus. */
    public static double lootProgress(double actualBonus, double maximumBonus) {
        return maximumBonus <= 0 ? 0 : Math.max(0, Math.min(1, actualBonus / maximumBonus));
    }
    public static double lootBonus(double health, double attack, double speed, double armor) {
        return Math.max(0, health - 1) + Math.max(0, attack - 1)
            + Math.max(0, speed - 1) + Math.max(0, armor) / 20;
    }
    public static double weight(double base, double extraWeight, double progress) {
        return base + extraWeight * Math.max(0, Math.min(1, progress));
    }
    public static int experience(int original, double strength, double multiplier, int cap) {
        return experience(original, strength, multiplier, 1, cap);
    }
    public static int experience(int original, double strength, double globalMultiplier, double eventMultiplier, int cap) {
        return (int)Math.max(0, Math.min(cap, Math.round(original * strength * globalMultiplier * eventMultiplier)));
    }
    private BalanceMath() {}
}
