package org.virgil.akiasync.mixin.util;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.world.level.ChunkPos;

public class EntityDensityTracker {

    public enum DensityLevel {
        LOW,
        MEDIUM,
        HIGH,
        EXTREME
    }

    private static final Long2IntOpenHashMap chunkDensity = new Long2IntOpenHashMap();

    private static volatile long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 1000;

    private static final int LOW_DENSITY_THRESHOLD = 100;
    private static final int MEDIUM_DENSITY_THRESHOLD = 512;
    private static final int HIGH_DENSITY_THRESHOLD = 2048;
    private static final int EXTREME_DENSITY_THRESHOLD = 4096;

    public static void updateChunkDensity(int chunkX, int chunkZ, int entityCount) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        chunkDensity.put(chunkKey, entityCount);
    }

    public static DensityLevel getDensityLevel(int chunkX, int chunkZ) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        int entityCount = chunkDensity.getOrDefault(chunkKey, 0);

        if (entityCount < LOW_DENSITY_THRESHOLD) {
            return DensityLevel.LOW;
        } else if (entityCount < MEDIUM_DENSITY_THRESHOLD) {
            return DensityLevel.MEDIUM;
        } else if (entityCount < HIGH_DENSITY_THRESHOLD) {
            return DensityLevel.HIGH;
        } else {
            return DensityLevel.EXTREME;
        }
    }

    public static DensityLevel getRegionDensityLevel(double x, double z) {
        int chunkX = ((int) Math.floor(x)) >> 4;
        int chunkZ = ((int) Math.floor(z)) >> 4;

        int totalEntities = 0;
        int checkedChunks = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long chunkKey = ChunkPos.asLong(chunkX + dx, chunkZ + dz);
                if (chunkDensity.containsKey(chunkKey)) {
                    totalEntities += chunkDensity.get(chunkKey);
                    checkedChunks++;
                }
            }
        }

        if (checkedChunks == 0) {
            return DensityLevel.LOW;
        }

        int avgDensity = totalEntities / checkedChunks;

        if (avgDensity < LOW_DENSITY_THRESHOLD) {
            return DensityLevel.LOW;
        } else if (avgDensity < MEDIUM_DENSITY_THRESHOLD) {
            return DensityLevel.MEDIUM;
        } else if (avgDensity < HIGH_DENSITY_THRESHOLD) {
            return DensityLevel.HIGH;
        } else {
            return DensityLevel.EXTREME;
        }
    }

    public static boolean shouldUseAggressiveOptimization(double x, double z) {
        DensityLevel level = getRegionDensityLevel(x, z);
        return level == DensityLevel.HIGH || level == DensityLevel.EXTREME;
    }

    public static boolean shouldUseSpatialIndex(double x, double z) {
        DensityLevel level = getRegionDensityLevel(x, z);
        return level == DensityLevel.MEDIUM || level == DensityLevel.HIGH || level == DensityLevel.EXTREME;
    }

    public static boolean shouldSkipOptimization(double x, double z) {
        DensityLevel level = getRegionDensityLevel(x, z);
        return level == DensityLevel.LOW;
    }

    public static void cleanup() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUpdateTime > UPDATE_INTERVAL_MS * 60) {

            chunkDensity.clear();
            lastUpdateTime = currentTime;
        }
    }

    public static String getStats() {
        int lowCount = 0;
        int mediumCount = 0;
        int highCount = 0;
        int extremeCount = 0;

        for (int entityCount : chunkDensity.values()) {
            if (entityCount < LOW_DENSITY_THRESHOLD) {
                lowCount++;
            } else if (entityCount < MEDIUM_DENSITY_THRESHOLD) {
                mediumCount++;
            } else if (entityCount < HIGH_DENSITY_THRESHOLD) {
                highCount++;
            } else {
                extremeCount++;
            }
        }

        return String.format("Density: Low=%d, Medium=%d, High=%d, Extreme=%d",
            lowCount, mediumCount, highCount, extremeCount);
    }

    public static void clear() {
        chunkDensity.clear();
        lastUpdateTime = 0;
    }
}
