package com.net.levelup;

import com.google.gson.JsonParser;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

final class PlayerEffectCheck {
    private static final MobEffect EFFECT = new MobEffect(MobEffectCategory.HARMFUL, 0) {};
    private static final MobEffect INSTANT = new MobEffect(MobEffectCategory.HARMFUL, 0) {
        @Override public boolean isInstantenous() { return true; }
    };

    static void run() {
        check(PlayerEffects.parse(json("{}"), id -> EFFECT).isEmpty(), "legacy configuration has no player effects");
        var configured = PlayerEffects.parse(json("""
            {"playerEffects":[{"effect":"minecraft:slowness","durationSeconds":30,"level":2},
            {"effect":"example:custom_effect"}]}
            """), id -> EFFECT);
        check(configured.size() == 2 && configured.get(0).durationTicks() == 600
            && configured.get(0).amplifier() == 1, "player effect seconds and human-readable level");
        check(configured.get(1).durationTicks() == 200 && configured.get(1).amplifier() == 0,
            "player effect field defaults");
        check(AttackEffects.parse(json("{\"playerEffects\":[{\"effect\":\"minecraft:slowness\"}]}"), id -> EFFECT).isEmpty(),
            "player effects do not leak into the attack effect list");
        check(PlayerEffects.parse(json("{\"attackEffects\":[{\"effect\":\"minecraft:poison\"}]}"), id -> EFFECT).isEmpty(),
            "attack effects do not leak into the player effect list");
        rejected("{\"playerEffects\":[{\"effect\":\"example:missing\"}]}", true);
        rejected("{\"playerEffects\":[{\"effect\":\"minecraft:slowness\",\"durationSeconds\":0}]}", false);
        rejected("{\"playerEffects\":[{\"effect\":\"minecraft:slowness\",\"level\":1.5}]}", false);
        rejected("{\"playerEffects\":[{\"effect\":\"minecraft:slowness\",\"level\":257}]}", false);
        rejected("{\"playerEffects\":[{\"durationSeconds\":5}]}", false);
        String entry = "{\"effect\":\"minecraft:slowness\"}";
        rejected("{\"playerEffects\":[" + String.join(",", java.util.Collections.nCopies(33, entry)) + "]}", false);
        // 瞬时效果无法在事件期间维持，直接拒绝。
        try {
            PlayerEffects.parse(json("{\"playerEffects\":[{\"effect\":\"minecraft:instant_damage\"}]}"), id -> INSTANT);
            throw new AssertionError("Instantaneous player effect accepted");
        } catch (IllegalArgumentException expected) { }
        // 每秒的施加判断：缺失就补、更强的不覆盖、同等级只在快过期时续期。
        check(PlayerEffects.needsRefresh(-1, 0, 0, 200), "missing player effect is applied");
        check(PlayerEffects.needsRefresh(0, 0, 0, 200), "expired player effect is applied");
        check(PlayerEffects.needsRefresh(0, 100, 0, 200), "player effect at half duration is refreshed");
        check(!PlayerEffects.needsRefresh(0, 101, 0, 200), "player effect above half duration is left alone");
        check(PlayerEffects.needsRefresh(0, 150, 1, 200), "weaker player effect is upgraded");
        check(!PlayerEffects.needsRefresh(1, 20, 0, 200), "stronger player effect is never downgraded");
        check(!PlayerEffects.needsRefresh(0, -1, 0, 200), "infinite player effect is left alone");
        System.out.println("Passed 18 player effect configuration and refresh checks.");
    }
    private static com.google.gson.JsonObject json(String text) { return JsonParser.parseString(text).getAsJsonObject(); }
    private static void rejected(String text, boolean unknown) {
        try {
            PlayerEffects.parse(json(text), id -> unknown ? null : EFFECT);
            throw new AssertionError("Invalid player effect configuration accepted: " + text);
        } catch (IllegalArgumentException expected) { }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
