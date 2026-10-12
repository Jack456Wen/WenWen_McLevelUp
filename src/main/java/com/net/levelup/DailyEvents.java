package com.net.levelup;

import java.util.List;
import java.util.SplittableRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/** One cached draw per world day; a stable seed preserves the result across restarts. */
final class DailyEvents {
    private Rules.Snapshot snapshot;
    private long observedDay;
    private long worldSeed;
    private boolean enabled;
    private Rules.Day selected;
    private EventHistory history;
    private final EventHistory transientHistory = new EventHistory();

    Rules.Day get(Rules.Snapshot rules, long day, long seed, boolean allowEvents) {
        return get(rules, day, seed, allowEvents, transientHistory);
    }

    Rules.Day get(Rules.Snapshot rules, long day, long seed, boolean allowEvents, EventHistory history) {
        if (snapshot == rules && observedDay == day && worldSeed == seed && enabled == allowEvents
            && this.history == history)
            return selected;
        snapshot = rules;
        observedDay = day;
        worldSeed = seed;
        enabled = allowEvents;
        this.history = history;
        // Separate from the world's gameplay RNG: spawning/players cannot change the daily draw.
        var random = new SplittableRandom(seed ^ Long.rotateLeft(day * 0x9E3779B97F4A7C15L, 21));
        selected = allowEvents ? draw(rules.days(), day, random::nextDouble, rule -> history.eligible(rule, day)) : null;
        // Only the event that actually executes is consumed; other winning rules remain eligible.
        if (selected != null) history.record(selected, day);
        return selected;
    }

    static Rules.Day draw(List<Rules.Day> rules, long day, DoubleSupplier random) {
        return draw(rules, day, random, rule -> true);
    }

    static Rules.Day draw(List<Rules.Day> rules, long day, DoubleSupplier random, Predicate<Rules.Day> eligible) {
        Rules.Day first = null;
        for (Rules.Day rule : rules) {
            if (day < rule.day() || !eligible.test(rule)) continue;
            // Every eligible rule gets its own draw, even after an earlier rule succeeds.
            boolean won = random.getAsDouble() < rule.probability();
            if (won && first == null) first = rule;
        }
        return first;
    }

    void clear() {
        snapshot = null;
        selected = null;
    }
}
