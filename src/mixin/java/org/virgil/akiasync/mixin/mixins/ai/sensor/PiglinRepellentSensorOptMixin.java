package org.virgil.akiasync.mixin.mixins.ai.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.virgil.akiasync.mixin.util.ManhattanSearchOffsets;
import org.virgil.akiasync.mixin.util.RepellentSearchCache;

import java.util.Optional;

@Mixin(value = PiglinSpecificSensor.class, priority = 1100)
public class PiglinRepellentSensorOptMixin {

    @Overwrite
    private static Optional<BlockPos> findNearestRepellent(ServerLevel level, LivingEntity entity) {
        RepellentSearchCache.incrementPiglinTick();

        BlockPos entityPos = entity.blockPosition();
        int cx = entityPos.getX();
        int cy = entityPos.getY();
        int cz = entityPos.getZ();

        long chunkKey = ((long) (cx >> 4) << 32) | (cz & 0xFFFFFFFFL);
        long entityBucket = (entity.getId() & 0xFFFFL) << 16 | ((cx >> 4) & 0xF) << 8 | ((cz >> 4) & 0xF);
        long cacheKey = (chunkKey << 16) | entityBucket;

        RepellentSearchCache.CachedResult cached = RepellentSearchCache.getPiglinCached(cacheKey);
        if (cached != null) {

            if (cached.result().isPresent()) {
                BlockPos cachedPos = cached.result().get();
                int dx = Math.abs(cachedPos.getX() - cx);
                int dy = Math.abs(cachedPos.getY() - cy);
                int dz = Math.abs(cachedPos.getZ() - cz);

                if (dx <= 32 && dy <= 32 && dz <= 32) {
                    return cached.result();
                }
            } else {

                return cached.result();
            }
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

            if (!state.is(BlockTags.PIGLIN_REPELLENTS)) {
                continue;
            }

            if (state.is(Blocks.SOUL_CAMPFIRE) && !CampfireBlock.isLitCampfire(state)) {
                continue;
            }

            mutablePos.set(x, y, z);
            Optional<BlockPos> result = Optional.of(mutablePos.immutable());
            RepellentSearchCache.putPiglinCache(cacheKey, result);
            return result;
        }

        Optional<BlockPos> result = Optional.empty();
        RepellentSearchCache.putPiglinCache(cacheKey, result);
        return result;
    }
}
