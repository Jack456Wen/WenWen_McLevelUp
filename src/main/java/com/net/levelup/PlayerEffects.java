package com.net.levelup;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;
import java.util.function.Function;

/**
 * 事件期间直接贴在玩家身上的效果（配置字段 {@code playerEffects}）。
 * 与 {@link AttackEffects} 不同，这里不需要怪物攻击：事件进行中每秒检查一次，
 * 效果缺失（被牛奶解除、死亡重生、自然过期）或快过期就重新施加；
 * 事件结束后不再施加，玩家身上的效果会在 {@code durationSeconds} 内自然淡出。
 */
public final class PlayerEffects {
    public static List<AttackEffects.Entry> parse(JsonObject day) {
        return parse(day, ForgeRegistries.MOB_EFFECTS::getValue);
    }
    static List<AttackEffects.Entry> parse(JsonObject day, Function<ResourceLocation, MobEffect> resolver) {
        List<AttackEffects.Entry> entries = AttackEffects.parse(day, "playerEffects", "player effect", resolver);
        for (AttackEffects.Entry entry : entries) {
            // 瞬时效果（瞬间伤害/瞬间治疗之类）会在事件期间每秒反复触发，配置出来只会出事。
            if (entry.effect().isInstantenous())
                throw new IllegalArgumentException("playerEffects cannot use an instant effect: " + entry.id());
        }
        return entries;
    }
    /**
     * 玩家身上的效果是否要（重新）施加。
     * @param amplifier 玩家当前该效果的等级，-1 表示没有
     * @param remainingTicks 玩家当前的剩余 tick，-1 表示无限时长
     */
    static boolean needsRefresh(int amplifier, int remainingTicks, int wantedAmplifier, int durationTicks) {
        if (amplifier < 0) return true;                 // 被解除、死亡重生或已过期
        if (amplifier > wantedAmplifier) return false;  // 玩家身上有更强的效果，不去覆盖
        if (amplifier < wantedAmplifier) return true;   // 更弱，升级到配置等级
        // 同等级：剩余不足一半就续期（续期由原版的叠加规则延长时长），无限时长不动它。
        return remainingTicks >= 0 && remainingTicks <= durationTicks / 2;
    }
    /** 每秒对所有在线玩家调用一次；{@code entries} 为空时什么都不做。 */
    public static void apply(ServerPlayer player, List<AttackEffects.Entry> entries) {
        if (entries.isEmpty() || !player.isAlive()) return;
        for (AttackEffects.Entry entry : entries) {
            MobEffectInstance current = player.getEffect(entry.effect());
            int amplifier = current == null ? -1 : current.getAmplifier();
            int remaining = current == null ? 0 : current.getDuration();
            if (!needsRefresh(amplifier, remaining, entry.amplifier(), entry.durationTicks())) continue;
            player.addEffect(new MobEffectInstance(entry.effect(), entry.durationTicks(), entry.amplifier()));
        }
    }
    private PlayerEffects() {}
}
