package com.net.levelup;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

final class OnceEventCheck {
    private static int checks;

    static void run() {
        check(parse("{}") == -1, "missing maxOccurrences preserves repeat behavior");
        rejected("{\"maxOccurrences\":true}");
        rejected("{\"maxOccurrences\":false}");
        rejected("{\"maxOccurrences\":\"false\"}");
        check(parse("{\"maxOccurrences\":0}") == 0 && parse("{\"maxOccurrences\":3}") == 3
            && parse("{\"maxOccurrences\":-3}") == -1, "integer limits");
        rejected("{\"maxOccurrences\":1.5}");
        rejected("{\"maxOccurrences\":2147483648}");
        rejected("{\"maxOccurrences\":null}");
        rejected("{\"maxOccurrences\":[]}");

        var once = event(1, "once");
        var another = event(1, "another");
        var repeat = event(-1, "repeat");
        var history = new EventHistory();
        var cache = new DailyEvents();
        var rules = rules(once, another);
        check(cache.get(rules, 29, 123, true, history) == null && !history.isDirty(),
            "ineligible rules do not consume a one-time event");
        check(cache.get(rules, 30, 123, false, history) == null && !history.isDirty(),
            "disabled rules do not consume a one-time event");
        check(cache.get(rules, 30, 123, true, history) == once && history.isDirty(),
            "selected event is recorded for saving");
        check(history.eligible(another, 31), "unselected winning event is not consumed");
        check(cache.get(rules, 30, 123, true, history) == once, "one-time event stays active all day");
        check(new DailyEvents().get(rules, 30, 123, true, history) == once,
            "cache reset does not end today's one-time event");

        history.setDirty(false);
        var restored = EventHistory.load(history.save(new CompoundTag()));
        check(new DailyEvents().get(rules, 30, 123, true, restored) == once && !restored.isDirty(),
            "same-day restart restores the event without changing history");
        check(!restored.eligible(once, 31) && !restored.eligible(once, 29),
            "played one-time event is excluded on later and earlier dates");
        check(new DailyEvents().get(rules, 31, 123, true, restored) == another,
            "next day can execute the previously unselected one-time event");
        var afterBoth = EventHistory.load(restored.save(new CompoundTag()));
        check(new DailyEvents().get(rules, 32, 123, true, afterBoth) == null,
            "played events stay excluded after save/load");
        check(new DailyEvents().get(rules(another, once), 32, 123, true, afterBoth) == null,
            "reordering configuration does not reset history");
        check(new DailyEvents().get(rules(event(1, "once")), 32, 123, true, afterBoth) == null,
            "reloaded equivalent event remains consumed");
        int[] draws = {0};
        check(DailyEvents.draw(List.of(once), 32, () -> { draws[0]++; return 0; },
            rule -> afterBoth.eligible(rule, 32)) == null && draws[0] == 0,
            "consumed event is never drawn again");
        var repeatingHistory = new EventHistory();
        check(cache.get(rules(repeat), 30, 123, true, repeatingHistory) == repeat
            && cache.get(rules(repeat), 31, 123, true, repeatingHistory) == repeat,
            "maxOccurrences event can execute on consecutive days");
        check(!repeatingHistory.eligible(event(1, "repeat"), 32),
            "changing a previously played maxOccurrences event to one-time preserves its history");
        check(EventHistory.load(new CompoundTag()).eligible(once, 31), "existing worlds start with empty history");
        var three = event(3, "three");
        var limited = new EventHistory();
        for (long day = 30; day < 33; day++) {
            check(new DailyEvents().get(rules(three), day, 123, true, limited) == three, "limited occurrence executes");
            limited = EventHistory.load(limited.save(new CompoundTag()));
            limited.setDirty(false);
            check(new DailyEvents().get(rules(three), day, 123, true, limited) == three && !limited.isDirty(),
                "same-day restart does not charge another occurrence");
        }
        check(!limited.eligible(three, 33), "three occurrences exhaust the limit");
        check(limited.eligible(event(4, "three"), 33), "raising limit preserves count");
        check(!limited.eligible(event(0, "three"), 32), "zero disables even today's event");
        check(!limited.eligible(three, 31), "time rollback does not consume another occurrence");
        System.out.println("Passed " + checks + " maxOccurrences event and persistent history checks.");
    }

    private static Rules.Day event(int maxOccurrences, String title) {
        return new Rules.Day(30, 1, maxOccurrences, title, title, 1, 1, 1, 0, "wenwen", 1,
            List.of(), List.of(), new EventBgm(null, 1, 180), net.minecraft.world.BossEvent.BossBarColor.RED, true, List.of());
    }
    private static Rules.Snapshot rules(Rules.Day... events) {
        return new Rules.Snapshot(List.of(events), Map.of());
    }
    private static int parse(String text) {
        return Rules.maxOccurrences(JsonParser.parseString(text).getAsJsonObject());
    }
    private static void rejected(String text) {
        try {
            parse(text);
            throw new AssertionError("Invalid maxOccurrences option accepted: " + text);
        } catch (IllegalArgumentException expected) { checks++; }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
