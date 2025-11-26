package org.virgil.akiasync.mixin.optimization.cache;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import java.util.concurrent.ConcurrentHashMap;

public class BlockPosIterationCache {

    private final ConcurrentHashMap<CacheKey, LongList> cache = new ConcurrentHashMap<>();
    private final int maxCacheSize;

    public static final BlockPosIterationCache INSTANCE = new BlockPosIterationCache(100);

    public static final LongList VILLAGER_WORK_RANGE = INSTANCE.getOrCompute(16, 16, 16);

    public static final LongList ZOMBIE_MINECART_RANGE = INSTANCE.getOrCompute(8, 4, 8);

    public static final LongList ENTITY_AWARENESS_RANGE = INSTANCE.getOrCompute(8, 8, 8);

    public static final LongList VILLAGER_BREED_RANGE = INSTANCE.getOrCompute(5, 5, 5);

    public BlockPosIterationCache(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    public LongList getOrCompute(int rangeX, int rangeY, int rangeZ) {
        CacheKey key = new CacheKey(rangeX, rangeY, rangeZ);
        return cache.computeIfAbsent(key, k -> {
            if (cache.size() > maxCacheSize) {
                cache.clear();
            }
            return computeBlockPositions(rangeX, rangeY, rangeZ);
        });
    }

    private LongList computeBlockPositions(int rangeX, int rangeY, int rangeZ) {
        LongArrayList positions = new LongArrayList();

        for (int distance = 0; distance <= Math.max(rangeX, Math.max(rangeY, rangeZ)); distance++) {
            for (int x = -rangeX; x <= rangeX; x++) {
                for (int y = -rangeY; y <= rangeY; y++) {
                    for (int z = -rangeZ; z <= rangeZ; z++) {
                        int manhattanDistance = Math.abs(x) + Math.abs(y) + Math.abs(z);
                        if (manhattanDistance == distance) {
                            positions.add(BlockPos.asLong(x, y, z));
                        }
                    }
                }
            }
        }

        return positions;
    }

    public static class BlockPosIterable implements Iterable<BlockPos> {
        private final BlockPos center;
        private final LongList positions;
        private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        public BlockPosIterable(BlockPos center, LongList positions) {
            this.center = center;
            this.positions = positions;
        }

        @Override
        public java.util.Iterator<BlockPos> iterator() {
            return new java.util.Iterator<BlockPos>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < positions.size();
                }

                @Override
                public BlockPos next() {
                    long packedPos = positions.getLong(index++);
                    int x = BlockPos.getX(packedPos) + center.getX();
                    int y = BlockPos.getY(packedPos) + center.getY();
                    int z = BlockPos.getZ(packedPos) + center.getZ();
                    return mutablePos.set(x, y, z);
                }
            };
        }
    }

    private static class CacheKey {
        private final int rangeX, rangeY, rangeZ;
        private final int hashCode;

        public CacheKey(int rangeX, int rangeY, int rangeZ) {
            this.rangeX = rangeX;
            this.rangeY = rangeY;
            this.rangeZ = rangeZ;
            this.hashCode = java.util.Objects.hash(rangeX, rangeY, rangeZ);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) obj;
            return rangeX == other.rangeX && rangeY == other.rangeY && rangeZ == other.rangeZ;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
