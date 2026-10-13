package com.net.levelup;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adapted from Champions (GPL-3.0); see CHAMPIONS-NOTICE.txt and CHAMPIONS-LICENSE.txt. */
final class RankParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private RankParticle(ClientLevel level, double x, double y, double z,
                         double red, double green, double blue, SpriteSet sprites) {
        super(level, x, y, z, 0.5 - level.random.nextDouble(), green, 0.5 - level.random.nextDouble());
        this.sprites = sprites;
        yd *= 0.2F;
        if (red == 0 && blue == 0) { xd *= 0.1F; zd *= 0.1F; }
        quadSize *= 0.75F;
        lifetime = (int) (8 / (level.random.nextDouble() * 0.8 + 0.2));
        hasPhysics = false;
        setSpriteFromAge(sprites);
        float brightness = level.random.nextFloat() * 0.5F + 0.35F;
        setColor((float) red * brightness, (float) green * brightness, (float) blue * brightness);
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        setSpriteFromAge(sprites);
        yd += 0.004;
        move(xd, yd, zd);
        if (y == yo) { xd *= 1.1; zd *= 1.1; }
        xd *= 0.96F; yd *= 0.96F; zd *= 0.96F;
        if (onGround) { xd *= 0.7F; zd *= 0.7F; }
    }
    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    private record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double red, double green, double blue) {
            return new RankParticle(level, x, y, z, red, green, blue, sprites);
        }
    }
    @Mod.EventBusSubscriber(modid = LevelUp.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(VisualParticles.RANK.get(), Provider::new);
        }
    }
}
