package com.net.levelup;

public final class BalanceCheck {
    public static void main(String[] args) {
        check(BalanceMath.day(0) == 1 && BalanceMath.day(23999) == 1 && BalanceMath.day(24000) == 2, "day boundaries");
        check(BalanceMath.remaining(0) == 1 && BalanceMath.remaining(12000) == .5f, "day progress");
        check(BalanceMath.remaining(23000) < BalanceMath.remaining(13000), "sleep skips remaining night");
        check(BalanceMath.remaining(24000) == 1, "next day resets progress");
        double weakOdds = BalanceMath.weight(3, 3, 1) / BalanceMath.weight(100, 0, 1);
        double strongOdds = BalanceMath.weight(3, 3, 5) / BalanceMath.weight(100, 0, 5);
        check(strongOdds > weakOdds, "stronger monsters improve rare reward odds");
        check(BalanceMath.experience(10, 3, 2, 10000) == 60, "experience scaling");
        check(BalanceMath.experience(Integer.MAX_VALUE, 100, 100, 10000) == 10000, "experience overflow cap");
        check(BalanceMath.experience(10, 3, 0, 10000) == 0, "experience disabled");
        check(BalanceMath.experience(10, 3, 2, 1.5, 10000) == 90, "event and global multipliers stack");
        check(BalanceMath.experience(10, 3, 2, 0, 10000) == 0, "zero event multiplier disables all experience");
        check(BalanceMath.experience(10, 3, 2, 1, 10000) == BalanceMath.experience(10, 3, 2, 10000), "default event multiplier preserves legacy rewards");
        System.out.println("Passed 11 LevelUp balance and time checks.");
        DailyEventCheck.run();
        OnceEventCheck.run();
        AttackEffectCheck.run();
        PlayerEffectCheck.run();
        LootCheck.run();
        EventBgmCheck.run();
        MobVisualCheck.run();
        MobEffectCheck.run();
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
