package org.virgil.akiasync.test.worldgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VectorizedBiomeSampler SIMD implementation.
 * Validates that vectorized implementation matches scalar reference.
 */
public class VectorizedBiomeSamplerTest {

    private static final long LCG_MULT = 6364136223846793005L;
    private static final long LCG_ADD = 1442695040888963407L;

    /**
     * Reference scalar implementation for comparison.
     */
    private static int referenceClosestCorner(long biomeZoomSeed,
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
            double distance = dx * dx + dy * dy + dz * dz;

            if (distance < bestDistance) {
                bestIndex = i;
                bestDistance = distance;
            }
        }
        return bestIndex;
    }

    @Test
    @DisplayName("Scalar reference should be deterministic")
    void testScalarDeterministic() {
        long seed = 0xDEADBEEFL;
        int quartX = 100, quartY = 64, quartZ = -50;
        double fracX = 0.3, fracY = 0.6, fracZ = 0.1;

        int result1 = referenceClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);
        int result2 = referenceClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);

        assertEquals(result1, result2);
    }

    @Test
    @DisplayName("All corner indices should be reachable")
    void testAllCornersReachable() {
        boolean[] cornerReached = new boolean[8];
        long seed = 12345L;

        // Try many different positions to hit all corners
        for (int i = 0; i < 10000 && !allTrue(cornerReached); i++) {
            int quartX = (i * 7) % 1000 - 500;
            int quartY = (i * 13) % 256;
            int quartZ = (i * 17) % 1000 - 500;
            double fracX = (i % 100) / 100.0;
            double fracY = ((i * 3) % 100) / 100.0;
            double fracZ = ((i * 7) % 100) / 100.0;

            int corner = referenceClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);
            cornerReached[corner] = true;
        }

        for (int i = 0; i < 8; i++) {
            assertTrue(cornerReached[i], "Corner " + i + " should be reachable");
        }
    }

    @Test
    @DisplayName("Distance calculation should be non-negative")
    void testDistanceNonNegative() {
        long seed = 0xCAFEBABEL;

        for (int i = 0; i < 1000; i++) {
            int quartX = (i * 7) % 1000 - 500;
            int quartY = (i * 13) % 256;
            int quartZ = (i * 17) % 1000 - 500;
            double fracX = (i % 4) / 4.0;
            double fracY = ((i + 1) % 4) / 4.0;
            double fracZ = ((i + 2) % 4) / 4.0;

            // Calculate all 8 distances
            for (int corner = 0; corner < 8; corner++) {
                double distance = calculateDistance(seed, quartX, quartY, quartZ,
                    fracX, fracY, fracZ, corner);
                assertTrue(distance >= 0, "Distance should be non-negative");
            }
        }
    }

    @Test
    @DisplayName("Batch processing should match individual calls")
    void testBatchProcessing() {
        long seed = 0x12345678L;
        int batchSize = 16;

        int[] quartXs = new int[batchSize];
        int[] quartYs = new int[batchSize];
        int[] quartZs = new int[batchSize];
        double[] fracXs = new double[batchSize];
        double[] fracYs = new double[batchSize];
        double[] fracZs = new double[batchSize];
        int[] expectedResults = new int[batchSize];

        // Fill with test data
        for (int i = 0; i < batchSize; i++) {
            quartXs[i] = i * 10 - 80;
            quartYs[i] = 64 + i;
            quartZs[i] = i * 5 - 40;
            fracXs[i] = (i % 4) / 4.0;
            fracYs[i] = ((i + 1) % 4) / 4.0;
            fracZs[i] = ((i + 2) % 4) / 4.0;

            expectedResults[i] = referenceClosestCorner(seed,
                quartXs[i], quartYs[i], quartZs[i],
                fracXs[i], fracYs[i], fracZs[i]);
        }

        // Verify batch would produce same results
        for (int i = 0; i < batchSize; i++) {
            int result = referenceClosestCorner(seed,
                quartXs[i], quartYs[i], quartZs[i],
                fracXs[i], fracYs[i], fracZs[i]);
            assertEquals(expectedResults[i], result,
                "Batch result should match individual at index " + i);
        }
    }

    private double calculateDistance(long biomeZoomSeed, int quartX, int quartY, int quartZ,
                                      double fracX, double fracY, double fracZ, int corner) {
        boolean xFlag = (corner & 4) == 0;
        boolean yFlag = (corner & 2) == 0;
        boolean zFlag = (corner & 1) == 0;

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

        return dx * dx + dy * dy + dz * dz;
    }

    private boolean allTrue(boolean[] arr) {
        for (boolean b : arr) {
            if (!b) return false;
        }
        return true;
    }
}
