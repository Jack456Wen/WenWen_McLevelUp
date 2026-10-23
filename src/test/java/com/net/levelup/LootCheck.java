package com.net.levelup;

import java.util.List;
import com.google.gson.JsonObject;

final class LootCheck {
    static void run() {
        double progress = BalanceMath.lootProgress(BalanceMath.lootBonus(45, 1, 1, 0),
            BalanceMath.lootBonus(50, 1, 1, 0));
        check(Math.abs(BalanceMath.weight(10, 30, progress) - 36.93877551020408) < 1e-10,
            "45 of configured 50 must not reach cap");
        check(BalanceMath.weight(10, 30, BalanceMath.lootProgress(49, 49)) == 40, "configured maximum reaches cap");
        check(BalanceMath.weight(5, 10, 0) == 5 && BalanceMath.weight(5, 10, .5) == 10,
            "base and linear interpolation");
        check(BalanceMath.weight(5, 10, 1) == 15 && BalanceMath.weight(5, 10, 100) == 15, "cap");
        check(BalanceMath.weight(5, 0, 1) == 5, "zero extra stays fixed");
        check(BalanceMath.lootProgress(0, 0) == 0 && BalanceMath.lootProgress(0, 49) == 0, "no strengthening");
        check(BalanceMath.lootBonus(.5, .5, 1, 0) == 0, "weakening adds no bonus");
        check(BalanceMath.lootBonus(4, 2, 1.5, 10) == 5, "combined attributes");
        check(BalanceMath.lootProgress(1, 4) == .25, "dimension normalization");
        var entries = List.of(entry(1, 100, 0), entry(2, 10, 20));
        check(Rules.pick(entries, 0, .8).count() == 1 && Rules.pick(entries, 1, .8).count() == 2,
            "higher progress increases item odds against fixed blank weight");
        var even = List.of(entry(1, 1, 0), entry(2, 1, 0));
        check(Rules.pick(even, 0, .4999).count() == 1 && Rules.pick(even, 0, .5).count() == 2, "equal weights");
        check(Rules.pick(List.of(entry(1, 0, 0)), 0, .5) == null, "zero total");
        var json = new JsonObject();
        check(Rules.extraWeight(json, 5) == 10, "item default extra");
        json.addProperty("blank", true);
        check(Rules.extraWeight(json, 5) == 0, "blank default fixed");
        json.remove("blank"); json.addProperty("item", "minecraft:air");
        check(Rules.extraWeight(json, 5) == 0, "air default fixed");
        json.addProperty("extraWeight", 4);
        check(Rules.extraWeight(json, 5) == 4, "explicit extra");
        json.addProperty("extraWeight", 0);
        check(Rules.extraWeight(json, 5) == 0, "explicit zero");
        json.addProperty("extraWeight", -1);
        reject(json);
        json.addProperty("extraWeight", Double.NaN); reject(json);
        json.remove("extraWeight"); json.addProperty("rarity", 0); reject(json);
        json.remove("rarity"); json.addProperty("minStrength", 1); reject(json);
        var blank = List.of(entry(0, 300, 0), entry(1, 5, 10));
        check(Rules.pick(blank, 100, 0).count() == 0 && Rules.pick(blank, 100, .999).count() == 1, "blank participates");
        System.out.println("Passed loot growth, cap, selection and configuration checks.");
    }
    private static Rules.Entry entry(int count, double weight, double extraWeight) {
        return new Rules.Entry(null, count, weight, extraWeight);
    }
    private static void reject(JsonObject json) {
        try { Rules.extraWeight(json, 5); throw new AssertionError("invalid config accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
