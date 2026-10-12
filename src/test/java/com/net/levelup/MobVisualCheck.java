package com.net.levelup;

import com.google.gson.JsonParser;
import net.minecraft.world.BossEvent.BossBarColor;

final class MobVisualCheck {
    static void run() {
        var defaults = JsonParser.parseString("{}").getAsJsonObject();
        check(Rules.bossBarColor(defaults) == BossBarColor.RED && Rules.mobParticles(defaults), "legacy defaults");
        for (var color : BossBarColor.values()) {
            var json = JsonParser.parseString("{\"bossBarColor\":\"" + color.name().toLowerCase(java.util.Locale.ROOT) + "\"}").getAsJsonObject();
            check(Rules.bossBarColor(json) == color, "all colors");
            check((MobVisualMessage.color(color) & 0xFF000000) == 0, "RGB range");
        }
        check(!Rules.mobParticles(JsonParser.parseString("{\"mobParticles\":false}").getAsJsonObject()), "disable particles");
        for (String text : new String[] {"{\"bossBarColor\":\"orange\"}", "{\"bossBarColor\":null}",
            "{\"bossBarColor\":1}", "{\"mobParticles\":\"false\"}"}) {
            try {
                var json = JsonParser.parseString(text).getAsJsonObject();
                Rules.bossBarColor(json);
                Rules.mobParticles(json);
                throw new AssertionError("accepted invalid visual config: " + text);
            } catch (IllegalArgumentException expected) { }
        }
        System.out.println("Passed LevelUp visual configuration checks.");
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
