package com.net.levelup;

import com.google.gson.JsonParser;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

final class AttackEffectCheck {
    private static final MobEffect EFFECT = new MobEffect(MobEffectCategory.HARMFUL, 0) {};
    static void run() {
        check(AttackEffects.parse(json("{}"), id -> EFFECT).isEmpty(), "legacy configuration has no effects");
        var configured = AttackEffects.parse(json("""
            {"attackEffects":[{"effect":"minecraft:poison","durationSeconds":10,"level":2},
            {"effect":"example:custom_effect"}]}
            """), id -> EFFECT);
        check(configured.size() == 2 && configured.get(0).durationTicks() == 200
            && configured.get(0).amplifier() == 1, "seconds and human-readable effect level");
        check(configured.get(1).id().toString().equals("example:custom_effect")
            && configured.get(1).durationTicks() == 200 && configured.get(1).amplifier() == 0,
            "mod effect namespace and field defaults");
        rejected("{\"attackEffects\":[{\"effect\":\"example:missing\"}]}", true);
        rejected("{\"attackEffects\":[{\"effect\":\"minecraft:poison\",\"durationSeconds\":0}]}", false);
        rejected("{\"attackEffects\":[{\"effect\":\"minecraft:poison\",\"level\":1.5}]}", false);
        rejected("{\"attackEffects\":[{\"effect\":\"minecraft:poison\",\"level\":257}]}", false);
        String entry = "{\"effect\":\"minecraft:poison\"}";
        rejected("{\"attackEffects\":[" + String.join(",", java.util.Collections.nCopies(33, entry)) + "]}", false);
        System.out.println("Passed 8 attack effect configuration checks.");
    }
    private static com.google.gson.JsonObject json(String text) { return JsonParser.parseString(text).getAsJsonObject(); }
    private static void rejected(String text, boolean unknown) {
        try {
            AttackEffects.parse(json(text), id -> unknown ? null : EFFECT);
            throw new AssertionError("Invalid attack effect configuration accepted: " + text);
        } catch (IllegalArgumentException expected) { }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
