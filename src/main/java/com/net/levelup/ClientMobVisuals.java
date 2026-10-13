package com.net.levelup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Entity-owned state is discarded with the entity; no world scan or retained entity map. */
final class ClientMobVisuals {
    static final String TAG = "levelup_visual";
    private static long budgetTick = Long.MIN_VALUE;
    private static int emitted;
    private ClientMobVisuals() { }
    static void register() {
        MinecraftForge.EVENT_BUS.addListener(ClientMobVisuals::tick);
    }
    static void handle(MobVisualMessage message) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var entity = level.getEntity(message.entityId());
        if (!(entity instanceof Mob) || !entity.getUUID().equals(message.uuid())) return;
        var data = new net.minecraft.nbt.CompoundTag();
        data.putString("name", message.name());
        data.putInt("rgb", message.rgb());
        data.putBoolean("particles", message.particles());
        entity.getPersistentData().put(TAG, data);
    }
    private static void tick(LivingEvent.LivingTickEvent event) {
        var mob = event.getEntity();
        if (!mob.level().isClientSide || !(mob instanceof Mob)
            || !mob.isAlive() || mob.isInvisible()
            || !mob.getPersistentData().contains(TAG)) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.player == null || mob.distanceToSqr(minecraft.player) > 32 * 32
            || minecraft.options.particles().get() == ParticleStatus.MINIMAL) return;
        long tick = mob.level().getGameTime();
        if (budgetTick != tick) { budgetTick = tick; emitted = 0; }
        boolean decreased = minecraft.options.particles().get() == ParticleStatus.DECREASED;
        if (decreased && (mob.tickCount + mob.getId()) % 2 != 0) return;
        int limit = decreased ? 8 : 16;
        if (emitted >= limit) return;
        var data = mob.getPersistentData().getCompound(TAG);
        if (!data.getBoolean("particles")) return;
        int rgb = data.getInt("rgb");
        mob.level().addParticle(VisualParticles.RANK.get(),
            mob.getX() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth(),
            mob.getY() + mob.getRandom().nextDouble() * mob.getBbHeight(),
            mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth(),
            ((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        emitted++;
    }
}
