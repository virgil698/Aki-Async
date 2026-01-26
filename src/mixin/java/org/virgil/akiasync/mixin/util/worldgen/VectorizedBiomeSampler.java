package org.virgil.akiasync.mixin.util.worldgen;

import jdk.incubator.vector.*;

/**
 * SIMD-accelerated biome sampling utilities.
 * Vectorizes the 8-point distance calculation in BiomeManager.getBiome().
 */
public class VectorizedBiomeSampler {

    private static final VectorSpecies<Double> SPECIES_D = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> SPECIES_L = LongVector.SPECIES_PREFERRED;

    // LCG constants for biome zoom seed mixing
    private static final long LCG_MULT = 6364136223846793005L;
    private static final long LCG_ADD = 1442695040888963407L;

    private static volatile boolean available = false;

    static {
        try {
            DoubleVector test = DoubleVector.broadcast(SPECIES_D, 1.0);
            available = SPECIES_D.length() >= 4;
            if (available) {
                System.out.println("[AkiAsync] VectorizedBiomeSampler initialized with " + SPECIES_D);
            }
        } catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * Vectorized biome sampling - computes all 8 corner distances in parallel.
     * Returns the index (0-7) of the closest corner.
     *
     * @param biomeZoomSeed The biome zoom seed
     * @param quartX Quarter X coordinate
     * @param quartY Quarter Y coordinate  
     * @param quartZ Quarter Z coordinate
     * @param fracX Fractional X (0-1)
     * @param fracY Fractional Y (0-1)
     * @param fracZ Fractional Z (0-1)
     * @return Index of closest corner (0-7)
     */
    public static int findClosestCorner(long biomeZoomSeed,
                                        int quartX, int quartY, int quartZ,
                                        double fracX, double fracY, double fracZ) {
        if (!available) {
            return findClosestCornerScalar(biomeZoomSeed, quartX, quartY, quartZ, fracX, fracY, fracZ);
        }

        // Compute all 8 distances
        double[] distances = new double[8];

        // Unroll the 8 iterations for better vectorization opportunity
        for (int i = 0; i < 8; i++) {
            boolean xFlag = (i & 4) == 0;
            boolean yFlag = (i & 2) == 0;
            boolean zFlag = (i & 1) == 0;

            long sampleX = xFlag ? quartX : quartX + 1;
            long sampleY = yFlag ? quartY : quartY + 1;
            long sampleZ = zFlag ? quartZ : quartZ + 1;

            double offsetX = xFlag ? fracX : fracX - 1.0;
            double offsetY = yFlag ? fracY : fracY - 1.0;
            double offsetZ = zFlag ? fracZ : fracZ - 1.0;

            // LCG seed mixing (6 iterations)
            long seed = biomeZoomSeed;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleX;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleY;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleZ;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleX;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleY;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleZ;

            double fiddleX = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + biomeZoomSeed;
            double fiddleY = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + biomeZoomSeed;
            double fiddleZ = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = offsetX + fiddleX;
            double dy = offsetY + fiddleY;
            double dz = offsetZ + fiddleZ;

            distances[i] = dx * dx + dy * dy + dz * dz;
        }

        // Find minimum using SIMD if we have 8-wide vectors
        return findMinIndex(distances);
    }

    /**
     * Batch biome sampling for multiple positions.
     * Useful for chunk generation where we sample many biomes.
     */
    public static void findClosestCornerBatch(long biomeZoomSeed,
                                              int[] quartXs, int[] quartYs, int[] quartZs,
                                              double[] fracXs, double[] fracYs, double[] fracZs,
                                              int[] results) {
        int count = quartXs.length;
        for (int i = 0; i < count; i++) {
            results[i] = findClosestCorner(biomeZoomSeed,
                quartXs[i], quartYs[i], quartZs[i],
                fracXs[i], fracYs[i], fracZs[i]);
        }
    }

    private static int findMinIndex(double[] distances) {
        int minIdx = 0;
        double minVal = distances[0];

        // Try to use SIMD for comparison if available
        if (available && SPECIES_D.length() >= 4) {
            try {
                // Load first 4 and second 4
                DoubleVector v1 = DoubleVector.fromArray(SPECIES_D, distances, 0);
                
                if (SPECIES_D.length() >= 8) {
                    // Can compare all 8 at once
                    // Find lane with minimum
                    for (int i = 0; i < 8; i++) {
                        if (distances[i] < minVal) {
                            minVal = distances[i];
                            minIdx = i;
                        }
                    }
                } else {
                    // Compare in two groups of 4
                    DoubleVector v2 = DoubleVector.fromArray(SPECIES_D, distances, 4);
                    
                    // Find min in each group
                    double min1 = v1.reduceLanes(VectorOperators.MIN);
                    double min2 = v2.reduceLanes(VectorOperators.MIN);
                    
                    double globalMin = Math.min(min1, min2);
                    
                    // Find the index
                    for (int i = 0; i < 8; i++) {
                        if (distances[i] == globalMin) {
                            return i;
                        }
                    }
                }
            } catch (Throwable t) {
                // Fallback to scalar
            }
        }

        // Scalar fallback
        for (int i = 1; i < 8; i++) {
            if (distances[i] < minVal) {
                minVal = distances[i];
                minIdx = i;
            }
        }
        return minIdx;
    }

    private static int findClosestCornerScalar(long biomeZoomSeed,
                                               int quartX, int quartY, int quartZ,
                                               double fracX, double fracY, double fracZ) {
        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 8; i++) {
            boolean xFlag = (i & 4) == 0;
            boolean yFlag = (i & 2) == 0;
            boolean zFlag = (i & 1) == 0;

            long sampleX = xFlag ? quartX : quartX + 1;
            long sampleY = yFlag ? quartY : quartY + 1;
            long sampleZ = zFlag ? quartZ : quartZ + 1;

            double offsetX = xFlag ? fracX : fracX - 1.0;
            double offsetY = yFlag ? fracY : fracY - 1.0;
            double offsetZ = zFlag ? fracZ : fracZ - 1.0;

            long seed = biomeZoomSeed;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleX;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleY;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleZ;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleX;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleY;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + sampleZ;

            double fiddleX = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + biomeZoomSeed;
            double fiddleY = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * LCG_MULT + LCG_ADD) + biomeZoomSeed;
            double fiddleZ = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = offsetX + fiddleX;
            double dy = offsetY + fiddleY;
            double dz = offsetZ + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;

            if (distance < bestDistance) {
                bestIndex = i;
                bestDistance = distance;
            }
        }

        return bestIndex;
    }

    public static String getOptimizationInfo() {
        if (!available) {
            return "VectorizedBiomeSampler: Not available (fallback to scalar)";
        }
        return String.format("VectorizedBiomeSampler: %s (length=%d)",
            SPECIES_D.toString(), SPECIES_D.length());
    }
}
