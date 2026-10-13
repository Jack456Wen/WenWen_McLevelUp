package com.net.levelup;

import com.google.gson.JsonParser;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class DailyEventCheck {
    private static int checks;

    static void run() {
        var guaranteed = event(30, 1, "first");
        var later = event(30, 1, "second");
        var never = event(1, 0, "never");
        var partial = event(30, 0.2, "partial");
        var draws = new AtomicInteger();
        check(DailyEvents.draw(List.of(guaranteed), 29, () -> {
            draws.incrementAndGet(); return 0;
        }) == null && draws.get() == 0, "before day 30 the rule is not drawn");
        check(DailyEvents.draw(List.of(guaranteed), 30, () -> 0.999999) == guaranteed,
            "day 30 is eligible and probability one always wins");
        check(DailyEvents.draw(List.of(never), 30, () -> 0) == null, "probability zero never wins");
        check(DailyEvents.draw(List.of(partial), 30, () -> 0.199999) == partial, "below probability wins");
        check(DailyEvents.draw(List.of(partial), 30, () -> 0.2) == null, "probability boundary loses");
        draws.set(0);
        check(DailyEvents.draw(List.of(guaranteed, later), 30, () -> {
            draws.incrementAndGet(); return 0.5;
        }) == guaranteed && draws.get() == 2, "draw all eligible rules and use the first winner");
        check(DailyEvents.draw(List.of(never, later), 30, () -> 0.5) == later,
            "a losing earlier rule does not block later winners");
        check(DailyEvents.draw(List.of(partial, never), 30, () -> 0.9) == null,
            "all losses mean no event");
        check(DailyEvents.draw(List.of(guaranteed), (long)Integer.MAX_VALUE + 7, () -> 0) == guaranteed,
            "large world ages remain eligible");

        var traversals = new AtomicInteger();
        List<Rules.Day> counted = new AbstractList<>() {
            @Override public Rules.Day get(int index) { traversals.incrementAndGet(); return never; }
            @Override public int size() { return 1; }
        };
        var countedRules = new Rules.Snapshot(counted, Map.of());
        var cache = new DailyEvents();
        cache.get(countedRules, 30, 123, true);
        int firstTraversal = traversals.get();
        check(firstTraversal > 0 && cache.get(countedRules, 30, 123, true) == null
            && traversals.get() == firstTraversal, "cache losing days without repeating draws");
        cache.get(countedRules, 31, 123, true);
        check(traversals.get() > firstTraversal, "new day performs a new draw");
        var winningRules = new Rules.Snapshot(List.of(guaranteed), Map.of());
        check(cache.get(winningRules, 31, 123, true) == guaranteed, "reload invalidates cached rules");
        check(cache.get(winningRules, 31, 123, false) == null
            && cache.get(winningRules, 31, 123, true) == guaranteed, "event switch applies immediately");

        var randomRules = new Rules.Snapshot(List.of(event(1, 0.5, "random")), Map.of());
        var restarted = new DailyEvents();
        boolean stable = true;
        int wins = 0;
        for (long day = 1; day <= 1000; day++) {
            var result = cache.get(randomRules, day, 456, true);
            restarted.clear();
            stable &= result == restarted.get(randomRules, day, 456, true);
            stable &= result == cache.get(new Rules.Snapshot(randomRules.days(), Map.of()), day, 456, true);
            if (result != null) wins++;
        }
        check(stable, "restart, revisiting a day and unchanged reload preserve daily results");
        check(wins > 400 && wins < 600, "different days draw independently with approximately configured odds");
        check(Rules.probability(json("{\"probability\":0.2}")) == 0.2
            && Rules.probability(json("{\"probability\":0}")) == 0
            && Rules.probability(json("{\"probability\":1}")) == 1, "probability format and endpoints");
        rejected("{}");
        rejected("{\"repeatEvery\":7}");
        rejected("{\"repeatEvery\":7,\"probability\":0.2}");
        rejected("{\"probability\":-0.1}");
        rejected("{\"probability\":1.1}");
        rejected("{\"probability\":\"NaN\"}");
        rejected("{\"probability\":\"Infinity\"}");
        System.out.println("Passed " + checks + " daily event probability and cache checks.");
    }

    private static Rules.Day event(long day, double probability, String title) {
        return new Rules.Day(day, probability, true, title, title, 1, 1, 1, 0, "wenwen", 1,
            List.of(), List.of(), new EventBgm(null, 1, 180), net.minecraft.world.BossEvent.BossBarColor.RED, true, List.of());
    }
    private static com.google.gson.JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
    private static void rejected(String text) {
        try {
            Rules.probability(json(text));
            throw new AssertionError("Invalid event probability accepted: " + text);
        } catch (IllegalArgumentException expected) { checks++; }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
