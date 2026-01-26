package org.virgil.akiasync.test.worldgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for biome sampling algorithms.
 * Validates that optimized implementations match vanilla Minecraft's behavior.
 */
public class BiomeSamplerTest {

    private static final long LCG_MULT = 6364136223846793005L;
    private static final long LCG_ADD = 1442695040888963407L;

    /**
     * Vanilla MC implementation of getFiddledDistance.
     * From: net.minecraft.world.level.biome.BiomeManager
     */
    private static double vanillaGetFiddledDistance(long seed, int x, int y, int z,
                                                     double xNoise, double yNoise, double zNoise) {
        long l = next(seed, x);
        l = next(l, y);
        l = next(l, z);
        l = next(l, x);
        l = next(l, y);
        l = next(l, z);
        double fiddle = getFiddle(l);
        l = next(l, seed);
        double fiddle1 = getFiddle(l);
        l = next(l, seed);
        double fiddle2 = getFiddle(l);
        return square(zNoise + fiddle2) + square(yNoise + fiddle1) + square(xNoise + fiddle);
    }

    private static long next(long left, long right) {
        left *= left * LCG_MULT + LCG_ADD;
        return left + right;
    }

    private static double getFiddle(long seed) {
        return (double)(((seed >> 24) & 1023) - 512) * (0.9 / 1024.0);
    }

    private static double square(double x) {
        return x * x;
    }

    /**
     * Optimized implementation (matches BiomeAccessOptimizationMixin).
     */
    private static double optimizedGetFiddledDistance(long seed, int x, int y, int z,
                                                       double xFraction, double yFraction, double zFraction) {
        long l = seed * (seed * LCG_MULT + LCG_ADD) + x;
        l = l * (l * LCG_MULT + LCG_ADD) + y;
        l = l * (l * LCG_MULT + LCG_ADD) + z;
        l = l * (l * LCG_MULT + LCG_ADD) + x;
        l = l * (l * LCG_MULT + LCG_ADD) + y;
        l = l * (l * LCG_MULT + LCG_ADD) + z;

        double d = optimizedGetFiddle(l);
        l = l * (l * LCG_MULT + LCG_ADD) + seed;
        double e = optimizedGetFiddle(l);
        l = l * (l * LCG_MULT + LCG_ADD) + seed;
        double f = optimizedGetFiddle(l);

        return square(zFraction + f) + square(yFraction + e) + square(xFraction + d);
    }

    private static double optimizedGetFiddle(long seed) {
        return ((double)((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
    }

    @Test
    @DisplayName("Optimized getFiddledDistance should match vanilla")
    void testGetFiddledDistanceMatchesVanilla() {
        long[] seeds = {0L, 1L, -1L, 12345L, 0xDEADBEEFL, Long.MAX_VALUE, Long.MIN_VALUE};
        int[][] coords = {
            {0, 0, 0}, {1, 1, 1}, {-1, -1, -1},
            {100, 64, -200}, {-1000, 256, 1000}
        };
        double[][] fractions = {
            {0.0, 0.0, 0.0}, {0.25, 0.25, 0.25}, {0.5, 0.5, 0.5},
            {0.75, 0.75, 0.75}, {0.125, 0.375, 0.625}
        };

        for (long seed : seeds) {
            for (int[] coord : coords) {
                for (double[] frac : fractions) {
                    double vanilla = vanillaGetFiddledDistance(seed,
                        coord[0], coord[1], coord[2],
                        frac[0], frac[1], frac[2]);
                    double optimized = optimizedGetFiddledDistance(seed,
                        coord[0], coord[1], coord[2],
                        frac[0], frac[1], frac[2]);

                    assertEquals(vanilla, optimized, 1e-10,
                        String.format("Mismatch at seed=%d, coord=(%d,%d,%d), frac=(%.2f,%.2f,%.2f)",
                            seed, coord[0], coord[1], coord[2], frac[0], frac[1], frac[2]));
                }
            }
        }
    }

    @Test
    @DisplayName("findClosestCorner should return valid index 0-7")
    void testFindClosestCornerRange() {
        long seed = 12345L;
        for (int i = 0; i < 1000; i++) {
            int quartX = (i * 7) % 1000 - 500;
            int quartY = (i * 13) % 256;
            int quartZ = (i * 17) % 1000 - 500;
            double fracX = (i % 4) / 4.0;
            double fracY = ((i + 1) % 4) / 4.0;
            double fracZ = ((i + 2) % 4) / 4.0;

            int result = findClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);
            assertTrue(result >= 0 && result <= 7,
                "Corner index should be 0-7, got: " + result);
        }
    }

    @Test
    @DisplayName("findClosestCorner should be deterministic")
    void testFindClosestCornerDeterministic() {
        long seed = 0xCAFEBABEL;
        int quartX = 100, quartY = 64, quartZ = -50;
        double fracX = 0.3, fracY = 0.6, fracZ = 0.1;

        int result1 = findClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);
        int result2 = findClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);

        assertEquals(result1, result2, "Same inputs should produce same corner");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 0.0, 0.0, 0.0",
        "100, 64, -100, 0.25, 0.5, 0.75",
        "-500, 128, 500, 0.125, 0.375, 0.625"
    })
    @DisplayName("Optimized corner finding should match vanilla loop")
    void testCornerFindingMatchesVanilla(int quartX, int quartY, int quartZ,
                                          double fracX, double fracY, double fracZ) {
        long seed = 0xDEADBEEFL;

        int vanillaResult = vanillaFindClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);
        int optimizedResult = findClosestCorner(seed, quartX, quartY, quartZ, fracX, fracY, fracZ);

        assertEquals(vanillaResult, optimizedResult,
            String.format("Corner mismatch at (%d,%d,%d) frac=(%.2f,%.2f,%.2f)",
                quartX, quartY, quartZ, fracX, fracY, fracZ));
    }

    /**
     * Vanilla implementation of corner finding (from BiomeManager.getBiome).
     */
    private int vanillaFindClosestCorner(long biomeZoomSeed, int quartX, int quartY, int quartZ,
                                          double fracX, double fracY, double fracZ) {
        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 8; i++) {
            boolean xFlag = (i & 4) == 0;
            boolean yFlag = (i & 2) == 0;
            boolean zFlag = (i & 1) == 0;

            int sampleX = xFlag ? quartX : quartX + 1;
            int sampleY = yFlag ? quartY : quartY + 1;
            int sampleZ = zFlag ? quartZ : quartZ + 1;

            double offsetX = xFlag ? fracX : fracX - 1.0;
            double offsetY = yFlag ? fracY : fracY - 1.0;
            double offsetZ = zFlag ? fracZ : fracZ - 1.0;

            double distance = vanillaGetFiddledDistance(biomeZoomSeed,
                sampleX, sampleY, sampleZ, offsetX, offsetY, offsetZ);

            if (distance < bestDistance) {
                bestIndex = i;
                bestDistance = distance;
            }
        }
        return bestIndex;
    }

    /**
     * Optimized implementation (matches VectorizedBiomeSampler scalar path).
     */
    private int findClosestCorner(long biomeZoomSeed, int quartX, int quartY, int quartZ,
                                   double fracX, double fracY, double fracZ) {
        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 8; i++) {
            boolean xFlag = (i & 4) == 0;
            boolean yFlag = (i & 2) == 0;
            boolean zFlag = (i & 1) == 0;

            int sampleX = xFlag ? quartX : quartX + 1;
            int sampleY = yFlag ? quartY : quartY + 1;
            int sampleZ = zFlag ? quartZ : quartZ + 1;

            double offsetX = xFlag ? fracX : fracX - 1.0;
            double offsetY = yFlag ? fracY : fracY - 1.0;
            double offsetZ = zFlag ? fracZ : fracZ - 1.0;

            double distance = optimizedGetFiddledDistance(biomeZoomSeed,
                sampleX, sampleY, sampleZ, offsetX, offsetY, offsetZ);

            if (distance < bestDistance) {
                bestIndex = i;
                bestDistance = distance;
            }
        }
        return bestIndex;
    }
}
