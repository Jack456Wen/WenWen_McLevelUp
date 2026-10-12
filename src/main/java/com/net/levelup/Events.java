package com.net.levelup;

import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Events {
    private static final String TAG = "levelup_data";
    private final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private long observedDay = Long.MIN_VALUE;
    private Rules.Day active;
    private final DailyEvents dailyEvents = new DailyEvents();
    private EventHistory eventHistory;
    private final java.util.Map<ServerPlayer, Long> bgmNextPlayback = new java.util.HashMap<>();
    /** 已经提醒过「客户端没有本模组通道」的玩家，避免每秒刷日志。 */
    private final java.util.Set<ServerPlayer> bgmUnsupported = new java.util.HashSet<>();

    @SubscribeEvent public void start(ServerAboutToStartEvent event) {
        dailyEvents.clear();
        eventHistory = null;
        observedDay = Long.MIN_VALUE;
        active = null;
        bgmNextPlayback.clear();
        bgmUnsupported.clear();
        AttackEffects.clear();
        load();
    }
    private boolean load() {
        try { Rules.reload(); return true; }
        catch (Exception e) { LevelUp.LOGGER.error("Cannot load levelup JSON; retaining previous valid rules", e); return false; }
    }
    @SubscribeEvent public void stop(ServerStoppedEvent event) {
        dailyEvents.clear();
        eventHistory = null;
        bar.removeAllPlayers();
        bgmNextPlayback.clear();
        bgmUnsupported.clear();
        active = null;
        observedDay = Long.MIN_VALUE;
        Rules.current = new Rules.Snapshot(java.util.List.of(), java.util.Map.of());
        AttackEffects.clear();
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("levelup").requires(s -> s.hasPermission(2))
            .then(Commands.literal("reload").executes(context -> {
                if (!load()) {
                    context.getSource().sendFailure(Component.literal("重载失败，请检查服务器日志；已保留原配置。"));
                    return 0;
                }
                context.getSource().sendSuccess(() -> Component.literal("LevelUp JSON 已重载。"), false);
                return 1;
            })));
    }
    @SubscribeEvent public void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // The old client entity is discarded on dimension change; replay for the replacement.
            if (active != null) active.bgm().stop(player);
            bgmNextPlayback.remove(player);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void join(EntityJoinLevelEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
            || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) return;
        CompoundTag data = mob.getPersistentData();
        if (data.contains(TAG)) return;
        // Loaded legacy mobs are marked but never retroactively strengthened.
        CompoundTag record = new CompoundTag();
        data.put(TAG, record);
        if (event.loadedFromDisk()) return;
        Rules.Day day = level.dimension().equals(Level.OVERWORLD) ? today(level) : null;
        boolean nether = level.dimension().equals(Level.NETHER);
        boolean end = level.dimension().equals(Level.END);
        double max = nether ? Settings.NETHER.get() : end ? Settings.END.get() : 1;
        if (day == null && !nether && !end) return;
        double factor = 1 + mob.getRandom().nextDouble() * (max - 1);
        double health = day == null ? factor : day.health();
        double attack = day == null ? factor : day.attack();
        double speed = day == null ? 1 + mob.getRandom().nextDouble()
            * ((nether ? Settings.NETHER_SPEED.get() : Settings.END_SPEED.get()) - 1) : day.speed();
        multiply(mob, Attributes.MAX_HEALTH, health, "health");
        multiply(mob, Attributes.ATTACK_DAMAGE, attack, "attack");
        multiply(mob, Attributes.MOVEMENT_SPEED, speed, "speed");
        if (day == null) {
            setBase(mob, Attributes.ARMOR, mob.getRandom().nextDouble()
                * (nether ? Settings.NETHER_ARMOR.get() : Settings.END_ARMOR.get()));
        } else {
            setBase(mob, Attributes.ARMOR, day.armor());
        }
        mob.setHealth(mob.getMaxHealth());
        // A special day's fixed armor is included in reward strength without allowing zero-base division.
        double strength = day == null ? factor : Math.max(1, (health + attack + speed + 1 + day.armor() / 20) / 4);
        record.putDouble("strength", strength);
        record.putDouble("attackMultiplier", attack);
        // Snapshot the event's multiplier so surviving mobs keep it across days and reloads.
        record.putDouble("experienceMultiplier", day == null ? 1 : day.experienceMultiplier());
        record.putString("loot", day == null ? "dimension" : day.loot());
        if (day != null) {
            AttackEffects.save(mob, record, day.attackEffects());
            record.putString("eventName", day.title());
            record.putInt("eventColor", MobVisualMessage.color(day.bossBarColor()));
            record.putBoolean("mobParticles", day.mobParticles());
        }
    }
    @SubscribeEvent public void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof Mob mob)) return;
        CompoundTag record = mob.getPersistentData().getCompound(TAG);
        if (record.contains("eventName")) Network.visual(player, mob, record);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void attackEffects(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide
            || !(event.getSource().getEntity() instanceof Mob attacker)) return;
        AttackEffects.apply(attacker, event.getEntity(), attacker.getPersistentData().getCompound(TAG));
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    public void rangedDamage(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide
            || !(event.getSource().getEntity() instanceof Mob attacker)) return;
        // Attribute-based melee is already multiplied. Scale owner-attributed projectiles separately.
        if (event.getSource().getDirectEntity() == attacker && attacker.getAttribute(Attributes.ATTACK_DAMAGE) != null) return;
        CompoundTag record = attacker.getPersistentData().getCompound(TAG);
        if (record.contains("attackMultiplier"))
            event.setAmount((float)(event.getAmount() * record.getDouble("attackMultiplier")));
    }
    private static void multiply(Mob mob, Attribute attribute, double factor, String name) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || factor == 1) return;
        UUID id = UUID.nameUUIDFromBytes(("levelup:" + name).getBytes(StandardCharsets.UTF_8));
        if (instance.getModifier(id) == null) instance.addPermanentModifier(new AttributeModifier(id,
            "levelup " + name, factor - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private static void setBase(Mob mob, Attribute attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    public void drops(LivingDropsEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide
            || !(event.getSource().getEntity() instanceof Player)
            || !event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        CompoundTag record = event.getEntity().getPersistentData().getCompound(TAG);
        if (!record.contains("strength")) return;
        var stack = Rules.roll(record.getString("loot"), record.getDouble("strength"), event.getEntity().getRandom());
        if (!stack.isEmpty()) event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
            event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), stack));
    }
    @SubscribeEvent(priority = EventPriority.LOW)
    public void experience(LivingExperienceDropEvent event) {
        if (event.isCanceled() || event.getAttackingPlayer() == null || event.getEntity().level().isClientSide) return;
        CompoundTag record = event.getEntity().getPersistentData().getCompound(TAG);
        double eventMultiplier = record.contains("experienceMultiplier") ? record.getDouble("experienceMultiplier") : 1;
        if (record.contains("strength")) event.setDroppedExperience(BalanceMath.experience(
            event.getDroppedExperience(), record.getDouble("strength"), Settings.XP.get(), eventMultiplier, Settings.XP_CAP.get()));
    }
    private Rules.Day today(ServerLevel level) {
        if (eventHistory == null) eventHistory = EventHistory.get(level);
        return dailyEvents.get(Rules.current, BalanceMath.day(level.getDayTime()), level.getSeed(), Settings.DAYS.get(), eventHistory);
    }
    @SubscribeEvent public void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
            || !level.dimension().equals(Level.OVERWORLD)) return;
        // One global O(players) update per second; progress follows world time, including sleep/time commands.
        if (level.getServer().getTickCount() % 20 != 0) return;
        MinecraftServer server = level.getServer();
        long time = level.getDayTime();
        long day = BalanceMath.day(time);
        Rules.Day next = today(level);
        if (day != observedDay || !java.util.Objects.equals(active, next)) {
            if (active != null) for (ServerPlayer player : server.getPlayerList().getPlayers()) active.bgm().stop(player);
            bgmNextPlayback.clear();
            if (next != null) server.getPlayerList().broadcastSystemMessage(Component.literal(next.announcement()), false);
            observedDay = day;
            active = next;
        }
        if (active == null) { bar.removeAllPlayers(); return; }
        bar.setName(Component.literal(active.title()));
        bar.setColor(active.bossBarColor());
        bar.setProgress(BalanceMath.remaining(time));
        // Display to every online player; gameplay effects still apply only to the overworld.
        var online = new java.util.HashSet<>(server.getPlayerList().getPlayers());
        bgmNextPlayback.keySet().retainAll(online);
        bgmUnsupported.retainAll(online);
        long now = System.nanoTime();
        for (ServerPlayer player : online) {
            bar.addPlayer(player);
            // 事件期间保持在线的玩家身上：被解除、死亡重生或快过期都会在下一秒补回来。
            PlayerEffects.apply(player, active.playerEffects());
            if (!active.bgm().enabled() || !EventBgm.due(now, bgmNextPlayback.get(player))) continue;
            // 客户端自己替换上一遍的声音，所以重播只需要再发一次播放指令。
            if (active.bgm().play(player)) {
                bgmNextPlayback.put(player, active.bgm().nextPlayback(now));
            } else if (bgmUnsupported.add(player)) {
                // 收不到指令时事件音乐不会播放；说了「客户端必装模组」，这里给出明确原因。
                LevelUp.LOGGER.warn("{} 的客户端没有 levelup:music 通道，事件 BGM 无法播放（客户端需要安装同版本模组）。",
                    player.getGameProfile().getName());
            }
        }
        for (ServerPlayer player : java.util.List.copyOf(bar.getPlayers()))
            if (!online.contains(player)) bar.removePlayer(player);
    }
}
