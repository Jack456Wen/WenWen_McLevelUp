package com.net.levelup;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;

/** Applied once on fresh event spawns; vanilla handles ticking, expiry and persistence. */
final class MobEffects {
    static List<AttackEffects.Entry> parse(JsonObject day) {
        return parse(day, ForgeRegistries.MOB_EFFECTS::getValue);
    }
    static List<AttackEffects.Entry> parse(JsonObject day, Function<ResourceLocation, MobEffect> resolver) {
        return AttackEffects.parse(day, "mobEffects", "mob effect", resolver);
    }
    static void apply(Mob mob, List<AttackEffects.Entry> entries) {
        for (var entry : entries) {
            var instance = new MobEffectInstance(entry.effect(), entry.durationTicks(), entry.amplifier());
            if (entry.effect().isInstantenous()) {
                if (mob.canBeAffected(instance))
                    entry.effect().applyInstantenousEffect(null, null, mob, entry.amplifier(), 1);
            } else mob.addEffect(instance);
        }
    }
    private MobEffects() { }
}
