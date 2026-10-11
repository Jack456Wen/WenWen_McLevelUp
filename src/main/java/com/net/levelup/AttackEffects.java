package com.net.levelup;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Immutable spawn snapshots, persisted by registry name and resolved once per loaded attacker. */
public final class AttackEffects {
    public record Entry(ResourceLocation id, MobEffect effect, int durationTicks, int amplifier) {}
    private static final Map<Mob, List<Entry>> CACHE = new WeakHashMap<>();
    private static boolean applying;

    public static List<Entry> parse(JsonObject day) {
        return parse(day, ForgeRegistries.MOB_EFFECTS::getValue);
    }
    static List<Entry> parse(JsonObject day, java.util.function.Function<ResourceLocation, MobEffect> resolver) {
        return parse(day, "attackEffects", "attack effect", resolver);
    }
    /** 解析名为 {@code key} 的效果数组；{@code label} 只用于报错信息（攻击效果 / 玩家效果）。 */
    static List<Entry> parse(JsonObject day, String key, String label,
                             java.util.function.Function<ResourceLocation, MobEffect> resolver) {
        if (!day.has(key)) return List.of();
        var array = day.getAsJsonArray(key);
        if (array.size() > 32) throw new IllegalArgumentException("At most 32 " + key + " per event");
        List<Entry> entries = new ArrayList<>();
        for (var element : array) {
            JsonObject o = element.getAsJsonObject();
            if (!o.has("effect")) throw new IllegalArgumentException("Missing effect in " + key);
            ResourceLocation id = ResourceLocation.tryParse(o.get("effect").getAsString());
            MobEffect effect = id == null ? null : resolver.apply(id);
            if (effect == null) throw new IllegalArgumentException("Unknown " + label + ": " + o.get("effect"));
            int duration = integer(o, "durationSeconds", 10, 1, 86400, label) * 20;
            int amplifier = integer(o, "level", 1, 1, 256, label) - 1;
            entries.add(new Entry(id, effect, duration, amplifier));
        }
        return List.copyOf(entries);
    }
    private static int integer(JsonObject o, String key, int fallback, int min, int max, String label) {
        double value = o.has(key) ? o.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value != Math.floor(value) || value < min || value > max)
            throw new IllegalArgumentException("Invalid " + label + " " + key + ": " + value);
        return (int)value;
    }
    public static void save(Mob mob, CompoundTag record, List<Entry> entries) {
        if (entries.isEmpty()) return;
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("effect", entry.id.toString());
            tag.putInt("durationTicks", entry.durationTicks);
            tag.putInt("amplifier", entry.amplifier);
            list.add(tag);
        }
        record.put("attackEffects", list);
        CACHE.put(mob, entries);
    }
    private static List<Entry> load(CompoundTag record) {
        List<Entry> entries = new ArrayList<>();
        ListTag list = record.getList("attackEffects", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(32, list.size()); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("effect"));
            MobEffect effect = id == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(id);
            // A removed mod effect must not break existing world entities.
            if (effect != null && tag.getInt("durationTicks") > 0)
                entries.add(new Entry(id, effect, Math.min(1728000, tag.getInt("durationTicks")),
                    Math.max(0, Math.min(255, tag.getInt("amplifier")))));
        }
        return List.copyOf(entries);
    }
    public static void apply(Mob attacker, LivingEntity target, CompoundTag record) {
        if (applying || !record.contains("attackEffects", Tag.TAG_LIST)) return;
        List<Entry> entries = CACHE.computeIfAbsent(attacker, ignored -> load(record));
        applying = true;
        try {
            for (Entry entry : entries) {
                if (entry.effect.isInstantenous()) {
                    if (target.canBeAffected(new MobEffectInstance(entry.effect, entry.durationTicks, entry.amplifier)))
                        entry.effect.applyInstantenousEffect(attacker, attacker, target, entry.amplifier, 1.0);
                } else target.addEffect(new MobEffectInstance(entry.effect, entry.durationTicks, entry.amplifier), attacker);
            }
        } finally { applying = false; }
    }
    public static void clear() { CACHE.clear(); applying = false; }
    private AttackEffects() {}
}
