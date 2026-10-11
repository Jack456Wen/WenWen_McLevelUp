package com.net.levelup;

import java.util.List;

final class LootCheck {
    static void run() {
        // minStrength 门槛：不满足的条目完全不参与抽取，都不满足时这次就没有额外掉落。
        var gated = List.of(entry(1, 1, 5), entry(2, 1, 1));
        check(Rules.pick(gated, 1, 0).count() == 2, "entries above the strength gate are skipped");
        check(Rules.pick(gated, 5, 0).count() == 1, "entries become eligible once strength reaches the gate");
        check(Rules.pick(List.of(entry(1, 1, 5)), 1, 0.5) == null, "nothing eligible means no drop");
        // 等权重：把 [0,1) 均分。
        var even = List.of(entry(1, 1, 1), entry(2, 1, 1));
        check(Rules.pick(even, 1, 0).count() == 1 && Rules.pick(even, 1, 0.4999).count() == 1
            && Rules.pick(even, 1, 0.5).count() == 2 && Rules.pick(even, 1, 0.9999).count() == 2,
            "equal weights split the random range");
        // rarity：权重按 强度^rarity 放大，强度越高稀有条目越容易抽到。
        var rarity = List.of(entry(1, 100, 0, 1), entry(2, 1, 3, 1));
        check(Rules.pick(rarity, 1, 0.5).count() == 1 && Rules.pick(rarity, 5, 0.5).count() == 2,
            "rarity raises the rare entry's odds with strength");
        // 权重为 0 的条目抽不中；整表权重为 0 时没有额外掉落。
        var zero = List.of(entry(1, 0, 0, 1), entry(2, 1, 0, 1));
        check(Rules.pick(zero, 1, 0).count() == 2 && Rules.pick(List.of(entry(1, 0, 0, 1)), 1, 0.99) == null,
            "zero weight entries are never picked");
        // 轮空条目照常参与抽取：抽中时 item 为 null，roll 会返回空物品堆（不掉落）。
        var blank = List.of(blankEntry(1, 0, 1), entry(2, 1, 0, 1));
        check(Rules.pick(blank, 1, 0).item() == null && Rules.pick(blank, 1, 0.9).count() == 2,
            "blank entries take part in the draw and are recognisable as blank");
        System.out.println("Passed 7 loot table selection checks.");
    }
    /** item 为 null 表示真实物品在这个纯逻辑测试里不重要，这里只用 count 区分条目。 */
    private static Rules.Entry entry(int count, double weight, double rarity, double minimumStrength) {
        return new Rules.Entry(null, count, weight, rarity, minimumStrength);
    }
    private static Rules.Entry entry(int count, double weight, double minimumStrength) {
        return entry(count, weight, 0, minimumStrength);
    }
    private static Rules.Entry blankEntry(double weight, double rarity, double minimumStrength) {
        return new Rules.Entry(null, 0, weight, rarity, minimumStrength);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
