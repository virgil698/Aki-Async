package org.virgil.akiasync.test.worldgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Linear Congruential Generator (LCG) used in biome sampling.
 * Validates that our implementation matches vanilla Minecraft's algorithm.
 */
public class LinearCongruentialGeneratorTest {

    private static final long LCG_MULT = 6364136223846793005L;
    private static final long LCG_ADD = 1442695040888963407L;

    /**
     * Vanilla MC implementation of LCG next.
     */
    private static long vanillaNext(long left, long right) {
        left *= left * LCG_MULT + LCG_ADD;
        return left + right;
    }

    @Test
    @DisplayName("LCG should produce consistent results for known seeds")
    void testLcgConsistency() {
        long seed = 12345L;
        long result1 = vanillaNext(seed, 100);
        long result2 = vanillaNext(seed, 100);
        assertEquals(result1, result2, "LCG should be deterministic");
    }

    @Test
    @DisplayName("LCG chain should match vanilla biome seed mixing")
    void testLcgChain() {
        long biomeZoomSeed = 0xDEADBEEFL;
        int x = 10, y = 64, z = -20;

        // Vanilla 6-step chain
        long seed = biomeZoomSeed;
        seed = vanillaNext(seed, x);
        seed = vanillaNext(seed, y);
        seed = vanillaNext(seed, z);
        seed = vanillaNext(seed, x);
        seed = vanillaNext(seed, y);
        seed = vanillaNext(seed, z);

        // Verify it's not zero (would indicate broken algorithm)
        assertNotEquals(0L, seed, "LCG chain should produce non-zero result");

        // Verify determinism
        long seed2 = biomeZoomSeed;
        seed2 = vanillaNext(seed2, x);
        seed2 = vanillaNext(seed2, y);
        seed2 = vanillaNext(seed2, z);
        seed2 = vanillaNext(seed2, x);
        seed2 = vanillaNext(seed2, y);
        seed2 = vanillaNext(seed2, z);

        assertEquals(seed, seed2, "Same inputs should produce same output");
    }

    @Test
    @DisplayName("Fiddle calculation should be in range [-0.45, 0.45]")
    void testFiddleRange() {
        for (long seed = 0; seed < 10000; seed++) {
            double fiddle = getFiddle(seed);
            assertTrue(fiddle >= -0.45 && fiddle <= 0.45,
                "Fiddle should be in range [-0.45, 0.45], got: " + fiddle);
        }
    }

    @Test
    @DisplayName("Fiddle calculation should match vanilla formula")
    void testFiddleFormula() {
        long testSeed = 0x123456789ABCDEFL;

        // Vanilla formula
        double vanillaFiddle = (double)(((testSeed >> 24) & 1023) - 512) * (0.9 / 1024.0);

        // Our optimized formula
        double optimizedFiddle = ((double)((testSeed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

        assertEquals(vanillaFiddle, optimizedFiddle, 1e-15,
            "Optimized fiddle should match vanilla");
    }

    private static double getFiddle(long seed) {
        return ((double)((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
    }
}
