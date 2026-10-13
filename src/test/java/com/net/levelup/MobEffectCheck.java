package com.net.levelup;

import com.google.gson.JsonParser;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

final class MobEffectCheck {
    private static final MobEffect EFFECT = new MobEffect(MobEffectCategory.BENEFICIAL, 0) {};
    static void run() {
        check(MobEffects.parse(json("{}"), id -> EFFECT).isEmpty(), "legacy defaults");
        var entries = MobEffects.parse(json("""
            {"mobEffects":[{"effect":"minecraft:resistance","durationSeconds":600,"level":2},
            {"effect":"example:custom"}]}
            """), id -> EFFECT);
        check(entries.size() == 2 && entries.get(0).durationTicks() == 12000
            && entries.get(0).amplifier() == 1, "duration and level conversion");
        check(entries.get(1).durationTicks() == 200 && entries.get(1).amplifier() == 0, "entry defaults");
        check(MobEffects.parse(json("{\"playerEffects\":[{\"effect\":\"minecraft:speed\"}]}"), id -> EFFECT).isEmpty(), "separate player list");
        for (String text : new String[] {
            "{\"mobEffects\":[{\"effect\":\"minecraft:speed\",\"durationSeconds\":0}]}",
            "{\"mobEffects\":[{\"effect\":\"minecraft:speed\",\"level\":257}]}",
            "{\"mobEffects\":[{\"effect\":\"minecraft:speed\",\"level\":1.5}]}"}) {
            try { MobEffects.parse(json(text), id -> EFFECT); throw new AssertionError("invalid entry accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        try { MobEffects.parse(json("{\"mobEffects\":[{\"effect\":\"example:missing\"}]}"), id -> null);
            throw new AssertionError("unknown effect accepted"); }
        catch (IllegalArgumentException expected) { }
        String entry = "{\"effect\":\"minecraft:speed\"}";
        try { MobEffects.parse(json("{\"mobEffects\":[" + String.join(",", java.util.Collections.nCopies(33, entry)) + "]}"), id -> EFFECT);
            throw new AssertionError("too many effects accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Passed 9 mob effect configuration checks.");
    }
    private static com.google.gson.JsonObject json(String text) { return JsonParser.parseString(text).getAsJsonObject(); }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
