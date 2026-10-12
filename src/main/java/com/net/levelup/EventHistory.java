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

    static EventHistory get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EventHistory::load, EventHistory::new, "levelup_events");
    }

    boolean eligible(Rules.Day event, long day) {
        Long played = lastPlayed.get(event.historyKey());
        // Resume today's event after restart/reload, but never draw it on another date.
        return event.repeatable() || played == null || played == day;
    }

    void record(Rules.Day event, long day) {
        Long previous = lastPlayed.put(event.historyKey(), day);
        if (previous == null || previous != day) setDirty();
    }

    static EventHistory load(CompoundTag tag) {
        EventHistory history = new EventHistory();
        CompoundTag played = tag.getCompound("lastPlayed");
        for (String key : played.getAllKeys()) {
            if (played.contains(key, Tag.TAG_LONG)) history.lastPlayed.put(key, played.getLong(key));
        }
        return history;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        CompoundTag played = new CompoundTag();
        lastPlayed.forEach(played::putLong);
        tag.put("lastPlayed", played);
        return tag;
    }
}
