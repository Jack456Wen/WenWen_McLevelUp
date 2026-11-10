package com.net.levelup.mixin;

import com.net.levelup.Events;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @Redirect(method = "getRandomSpawnMobAt", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/random/WeightedRandomList;getRandom(Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
    private static Optional<SpawnerData> levelup$selectSpawn(WeightedRandomList<SpawnerData> pool, RandomSource random,
                                                           ServerLevel level, StructureManager structures,
                                                           ChunkGenerator generator, MobCategory category,
                                                           RandomSource spawnRandom, BlockPos pos) {
        return Events.selectNaturalSpawn(level, pool, random);
    }
}
