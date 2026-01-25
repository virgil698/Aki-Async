package org.virgil.akiasync.mixin.async.explosion;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

public class OptimizedExplosionCache {

    private final Long2ObjectOpenHashMap<BlockState> blockStateCache;

    private final Long2ObjectOpenHashMap<FluidState> fluidStateCache;

    private final Long2FloatOpenHashMap resistanceCache;

    private final Long2FloatOpenHashMap densityCache;

    private final Level level;

    private long lastCleanupTime = 0;

    private int getCacheExpiryTicks() {
        Bridge bridge = BridgeManager.getBridge();
        return bridge != null ? bridge.getTNTCacheExpiryTicks() : 600;
    }

    public OptimizedExplosionCache(Level level) {

        this.level = level;
        this.blockStateCache = new Long2ObjectOpenHashMap<>(512);
        this.fluidStateCache = new Long2ObjectOpenHashMap<>(256);
        this.resistanceCache = new Long2FloatOpenHashMap(512);
        this.densityCache = new Long2FloatOpenHashMap(256);

        this.resistanceCache.defaultReturnValue(-1.0f);
        this.densityCache.defaultReturnValue(-1.0f);
    }

    public BlockState getBlockState(BlockPos pos) {
        long key = pos.asLong();
        BlockState cached = blockStateCache.get(key);

        if (cached != null) {
            return cached;
        }

        BlockState state = level.getBlockState(pos);
        blockStateCache.put(key, state);

        return state;
    }

    public FluidState getFluidState(BlockPos pos) {
        long key = pos.asLong();
        FluidState cached = fluidStateCache.get(key);

        if (cached != null) {
            return cached;
        }

        BlockState state = getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        fluidStateCache.put(key, fluidState);

        return fluidState;
    }

    public float getResistance(BlockPos pos) {
        long key = pos.asLong();
        float cached = resistanceCache.get(key);

        if (cached >= 0) {
            return cached;
        }

        BlockState state = getBlockState(pos);
        float resistance = state.getBlock().getExplosionResistance();
        resistanceCache.put(key, resistance);

        return resistance;
    }

    public float getDensity(BlockPos pos) {
        long key = pos.asLong();
        float cached = densityCache.get(key);

        if (cached >= 0) {
            return cached;
        }

        float resistance = getResistance(pos);
        float density = resistance * 0.3f + 0.3f;
        densityCache.put(key, density);

        return density;
    }

    public void getResistanceBatch(BlockPos[] positions, float[] resistances) {
        for (int i = 0; i < positions.length; i++) {
            resistances[i] = getResistance(positions[i]);
        }
    }

    public void getDensityBatch(BlockPos[] positions, float[] densities) {
        for (int i = 0; i < positions.length; i++) {
            densities[i] = getDensity(positions[i]);
        }
    }

    public void warmup(BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= radius) {
                        BlockPos pos = center.offset(x, y, z);
                        getBlockState(pos);
                        getResistance(pos);
                    }
                }
            }
        }
    }

    public void cleanup() {
        long currentTime = level.getGameTime();

        if (currentTime - lastCleanupTime < getCacheExpiryTicks()) {
            return;
        }

        blockStateCache.clear();
        fluidStateCache.clear();
        resistanceCache.clear();
        densityCache.clear();

        lastCleanupTime = currentTime;
    }

    public void clear() {
        blockStateCache.clear();
        fluidStateCache.clear();
        resistanceCache.clear();
        densityCache.clear();
    }

    public String getStats() {
        return String.format("BlockState: %d, FluidState: %d, Resistance: %d, Density: %d",
            blockStateCache.size(),
            fluidStateCache.size(),
            resistanceCache.size(),
            densityCache.size());
    }

    public CacheStats getCacheStats() {
        return new CacheStats(
            blockStateCache.size(),
            fluidStateCache.size(),
            resistanceCache.size(),
            densityCache.size()
        );
    }

    public static class CacheStats {
        public final int blockStateCount;
        public final int fluidStateCount;
        public final int resistanceCount;
        public final int densityCount;

        public CacheStats(int blockStateCount, int fluidStateCount,
                         int resistanceCount, int densityCount) {
            this.blockStateCount = blockStateCount;
            this.fluidStateCount = fluidStateCount;
            this.resistanceCount = resistanceCount;
            this.densityCount = densityCount;
        }

        public int getTotalCount() {
            return blockStateCount + fluidStateCount + resistanceCount + densityCount;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{blockState=%d, fluidState=%d, resistance=%d, density=%d, total=%d}",
                blockStateCount, fluidStateCount, resistanceCount, densityCount, getTotalCount());
        }
    }
}
