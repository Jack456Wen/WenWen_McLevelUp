package com.net.levelup;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;

/** Marks a configured spawn and reuses the first placement check for this pack. */
public final class SpecialDaySpawn extends SpawnerData {
    private BlockPos initialPosition;

    public SpecialDaySpawn(SpawnerData original, BlockPos initialPosition) {
        super(original.type, original.getWeight(), original.minCount, original.maxCount);
        this.initialPosition = initialPosition;
    }

    public boolean useInitialCheck(BlockPos pos) {
        boolean checked = pos.equals(initialPosition);
        initialPosition = null;
        return checked;
    }
}
