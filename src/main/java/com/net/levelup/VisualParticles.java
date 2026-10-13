package com.net.levelup;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

final class VisualParticles {
    static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LevelUp.ID);
    static final RegistryObject<SimpleParticleType> RANK = TYPES.register("rank", () -> new SimpleParticleType(false));
    private VisualParticles() { }
}
