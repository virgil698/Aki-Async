package org.virgil.akiasync.mixin.mixins.ai.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.sensing.HoglinSpecificSensor;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.virgil.akiasync.mixin.util.ManhattanSearchOffsets;
import org.virgil.akiasync.mixin.util.RepellentSearchCache;

import java.util.Optional;

@Mixin(value = HoglinSpecificSensor.class, priority = 1100)
public class HoglinRepellentSensorOptMixin {

    @Overwrite
    private Optional<BlockPos> findNearestRepellent(ServerLevel level, Hoglin hoglin) {
        RepellentSearchCache.incrementHoglinTick();

        BlockPos entityPos = hoglin.blockPosition();
        int cx = entityPos.getX();
        int cy = entityPos.getY();
        int cz = entityPos.getZ();

        long cacheKey = ((long) hoglin.getId() << 32) | (entityPos.asLong() >>> 4);
        RepellentSearchCache.CachedResult cached = RepellentSearchCache.getHoglinCached(cacheKey);
        if (cached != null) {
            return cached.result();
        }

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        LevelChunk currentChunk = null;
        int currentChunkX = Integer.MIN_VALUE;
        int currentChunkZ = Integer.MIN_VALUE;

        int[] offsets = ManhattanSearchOffsets.OFFSETS_8_4_8;
        int offsetCount = ManhattanSearchOffsets.OFFSET_COUNT_8_4_8;

        for (int i = 0; i < offsetCount; i++) {
            int base = i * 3;
            int x = cx + offsets[base];
            int y = cy + offsets[base + 1];
            int z = cz + offsets[base + 2];

            if (y < level.getMinY() || y >= level.getMaxY()) {
                continue;
            }

            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            if (chunkX != currentChunkX || chunkZ != currentChunkZ) {
                currentChunk = level.getChunkIfLoaded(chunkX, chunkZ);
                currentChunkX = chunkX;
                currentChunkZ = chunkZ;
            }

            if (currentChunk == null) {
                continue;
            }

            int sectionIdx = currentChunk.getSectionIndex(y);
            if (sectionIdx < 0 || sectionIdx >= currentChunk.getSectionsCount()) {
                continue;
            }

            LevelChunkSection section = currentChunk.getSection(sectionIdx);
            if (section.hasOnlyAir()) {
                continue;
            }

            BlockState state = section.getBlockState(x & 15, y & 15, z & 15);

            if (!state.is(BlockTags.HOGLIN_REPELLENTS)) {
                continue;
            }

            mutablePos.set(x, y, z);
            Optional<BlockPos> result = Optional.of(mutablePos.immutable());
            RepellentSearchCache.putHoglinCache(cacheKey, result);
            return result;
        }

        Optional<BlockPos> result = Optional.empty();
        RepellentSearchCache.putHoglinCache(cacheKey, result);
        return result;
    }
}
