package com.net.levelup;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-save execution history, stored in the overworld's data directory. */
final class EventHistory extends SavedData {
    private final Map<String, Long> lastPlayed = new HashMap<>();
    private final Map<String, Long> playCounts = new HashMap<>();

    static EventHistory get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EventHistory::load, EventHistory::new, "levelup_events");
    }

    boolean eligible(Rules.Day event, long day) {
        Long played = lastPlayed.get(event.historyKey());
        int limit = event.maxOccurrences();
        if (limit == 0) return false;
        if (limit < 0) return true;
        // Resume the latest execution on the same date without charging it again.
        if (played != null && played == day) return true;
        if (played != null && day < played) return false;
        return playCounts.getOrDefault(event.historyKey(), 0L) < limit;
    }

    void record(Rules.Day event, long day) {
        Long previous = lastPlayed.put(event.historyKey(), day);
        if (previous == null || previous != day) {
            playCounts.compute(event.historyKey(), (key, count) -> count == null ? 1L
                : count == Long.MAX_VALUE ? count : count + 1);
            setDirty();
        }
    }

    static EventHistory load(CompoundTag tag) {
        EventHistory history = new EventHistory();
        CompoundTag played = tag.getCompound("lastPlayed");
        CompoundTag counts = tag.getCompound("playCounts");
        for (String key : played.getAllKeys()) {
            if (played.contains(key, Tag.TAG_LONG) && counts.contains(key, Tag.TAG_LONG)) {
                history.lastPlayed.put(key, played.getLong(key));
                history.playCounts.put(key, counts.getLong(key));
            }
        }
        return history;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag played = new CompoundTag();
        lastPlayed.forEach(played::putLong);
        tag.put("lastPlayed", played);
        CompoundTag counts = new CompoundTag();
        playCounts.forEach(counts::putLong);
        tag.put("playCounts", counts);
        return tag;
    }
}
