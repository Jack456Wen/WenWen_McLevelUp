package com.net.levelup;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

final class OnceEventCheck {
    private static int checks;

    static void run() {
        check(parse("{}"), "missing repeatable preserves repeat behavior");
        check(parse("{\"repeatable\":true}") && !parse("{\"repeatable\":false}"), "boolean options");
        rejected("{\"repeatable\":\"false\"}");
        rejected("{\"repeatable\":0}");
        rejected("{\"repeatable\":null}");
        rejected("{\"repeatable\":[]}");

        var once = event(false, "once");
        var another = event(false, "another");
        var repeat = event(true, "repeat");
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
        check(new DailyEvents().get(rules(event(false, "once")), 32, 123, true, afterBoth) == null,
            "reloaded equivalent event remains consumed");
        int[] draws = {0};
        check(DailyEvents.draw(List.of(once), 32, () -> { draws[0]++; return 0; },
            rule -> afterBoth.eligible(rule, 32)) == null && draws[0] == 0,
            "consumed event is never drawn again");
        var repeatingHistory = new EventHistory();
        check(cache.get(rules(repeat), 30, 123, true, repeatingHistory) == repeat
            && cache.get(rules(repeat), 31, 123, true, repeatingHistory) == repeat,
            "repeatable event can execute on consecutive days");
        check(!repeatingHistory.eligible(event(false, "repeat"), 32),
            "changing a previously played repeatable event to one-time preserves its history");
        check(EventHistory.load(new CompoundTag()).eligible(once, 31), "existing worlds start with empty history");
        System.out.println("Passed " + checks + " repeatable event and persistent history checks.");
    }

    private static Rules.Day event(boolean repeatable, String title) {
        return new Rules.Day(30, 1, repeatable, title, title, 1, 1, 1, 0, "wenwen", 1,
            List.of(), List.of(), new EventBgm(null, 1, 180), net.minecraft.world.BossEvent.BossBarColor.RED, true);
    }
    private static Rules.Snapshot rules(Rules.Day... events) {
        return new Rules.Snapshot(List.of(events), Map.of());
    }
    private static boolean parse(String text) {
        return Rules.repeatable(JsonParser.parseString(text).getAsJsonObject());
    }
    private static void rejected(String text) {
        try {
            parse(text);
            throw new AssertionError("Invalid repeatable option accepted: " + text);
        } catch (IllegalArgumentException expected) { checks++; }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
