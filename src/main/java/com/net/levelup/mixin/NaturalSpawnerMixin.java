package com.net.levelup.mixin;

import com.net.levelup.Events;
import com.net.levelup.SpecialDaySpawn;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    private static final String SPAWN_PACK = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;"
        + "Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;"
        + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;"
        + "Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V";
    @Shadow
    private static WeightedRandomList<SpawnerData> mobsAt(ServerLevel level, StructureManager structures,
        ChunkGenerator generator, MobCategory category, BlockPos pos, Holder<Biome> biome) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean isValidSpawnPostitionForType(ServerLevel level, MobCategory category,
        StructureManager structures, ChunkGenerator generator, SpawnerData data, BlockPos.MutableBlockPos pos,
        double playerDistance) {
        throw new AssertionError();
    }

    @Redirect(method = "getRandomSpawnMobAt", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/random/WeightedRandomList;getRandom(Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
    private static Optional<SpawnerData> levelup$selectSpawn(WeightedRandomList<SpawnerData> pool, RandomSource random,
        ServerLevel level, StructureManager structures, ChunkGenerator generator, MobCategory category,
        RandomSource spawnRandom, BlockPos pos) {
        if (!pool.isEmpty()) {
            EntityType<?> target = Events.rollNaturalSpawn(level, random);
            if (target != null && target.getCategory() != MobCategory.MISC) {
                var targetPool = target.getCategory() == category ? pool
                    : mobsAt(level, structures, generator, target.getCategory(), pos, null);
                SpawnerData candidate = targetPool.unwrap().stream().filter(entry -> entry.type == target)
                    .findFirst().orElseGet(() -> new SpawnerData(target, 1, 1, 1));
                double x = pos.getX() + 0.5;
                double z = pos.getZ() + 0.5;
                Player player = level.getNearestPlayer(x, pos.getY(), z, -1.0, false);
                if (player != null && levelup$canPlaceTarget(level, target, pos,
                    player.distanceToSqr(x, pos.getY(), z))) {
                    return Optional.of(new SpecialDaySpawn(candidate, pos.immutable()));
                }
            }
        }
        return pool.getRandom(random);
    }

    @Redirect(method = SPAWN_PACK, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/NaturalSpawner;isValidSpawnPostitionForType(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z"))
    private static boolean levelup$validateSpawn(ServerLevel level, MobCategory category, StructureManager structures,
        ChunkGenerator generator, SpawnerData data, BlockPos.MutableBlockPos pos, double playerDistance) {
        if (data instanceof SpecialDaySpawn special) {
            if (!Events.hasSpecialSpawnCapacity()) return false;
            if (special.useInitialCheck(pos)) return true;
            return levelup$canPlaceTarget(level, data.type, pos, playerDistance);
        }
        return isValidSpawnPostitionForType(level, category, structures, generator, data, pos, playerDistance);
    }

    @Inject(method = SPAWN_PACK, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Mob;moveTo(DDDFF)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void levelup$markReplacement(MobCategory category, ServerLevel level, ChunkAccess chunk,
        BlockPos origin, NaturalSpawner.SpawnPredicate predicate, NaturalSpawner.AfterSpawnCallback callback,
        CallbackInfo ci, StructureManager structures, ChunkGenerator generator, int y, BlockState block,
        BlockPos.MutableBlockPos pos, int spawned, int pack, int x, int z, int spread, SpawnerData data,
        SpawnGroupData group, int packSize, int packSpawned, int attempt, double spawnX, double spawnZ,
        Player player, double playerDistance, Mob mob) {
        if (data instanceof SpecialDaySpawn) mob.getPersistentData().putBoolean(Events.SPECIAL_SPAWN_TAG, true);
    }

    private static boolean levelup$canPlaceTarget(ServerLevel level, EntityType<?> type, BlockPos pos,
        double playerDistance) {
        int despawnDistance = type.getCategory().getDespawnDistance();
        return type.canSummon()
            && (type.canSpawnFarFromPlayer() || playerDistance <= (double) despawnDistance * despawnDistance)
            && NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(type), level, pos, type)
            && SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, level.random)
            && level.noCollision(type.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
    }
}
