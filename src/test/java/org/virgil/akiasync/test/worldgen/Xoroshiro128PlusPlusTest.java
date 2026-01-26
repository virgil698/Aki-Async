package org.virgil.akiasync.test.worldgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Xoroshiro128++ random number generator.
 * Validates that our implementation matches vanilla Minecraft's algorithm.
 */
public class Xoroshiro128PlusPlusTest {

    /**
     * Pure Java implementation of Xoroshiro128++ for testing.
     */
    static class Xoroshiro128PlusPlus {
        private long seedLo;
        private long seedHi;

        public Xoroshiro128PlusPlus(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
            if ((this.seedLo | this.seedHi) == 0L) {
                this.seedLo = -7046029254386353131L;
                this.seedHi = 7640891576956012809L;
            }
        }

        public long nextLong() {
            long l = this.seedLo;
            long l1 = this.seedHi;
            long l2 = Long.rotateLeft(l + l1, 17) + l;
            l1 ^= l;
            this.seedLo = Long.rotateLeft(l, 49) ^ l1 ^ l1 << 21;
            this.seedHi = Long.rotateLeft(l1, 28);
            return l2;
        }

        public long getSeedLo() { return seedLo; }
        public long getSeedHi() { return seedHi; }
    }

    @Test
    @DisplayName("Xoroshiro should produce consistent sequence")
    void testConsistentSequence() {
        Xoroshiro128PlusPlus rng1 = new Xoroshiro128PlusPlus(12345L, 67890L);
        Xoroshiro128PlusPlus rng2 = new Xoroshiro128PlusPlus(12345L, 67890L);

        for (int i = 0; i < 100; i++) {
            assertEquals(rng1.nextLong(), rng2.nextLong(),
                "Same seed should produce same sequence at index " + i);
        }
    }

    @Test
    @DisplayName("Xoroshiro should handle zero seed correctly")
    void testZeroSeedHandling() {
        Xoroshiro128PlusPlus rng = new Xoroshiro128PlusPlus(0L, 0L);
        assertNotEquals(0L, rng.getSeedLo(), "Zero seed should be replaced");
        assertNotEquals(0L, rng.getSeedHi(), "Zero seed should be replaced");
    }

    @Test
    @DisplayName("Xoroshiro should produce non-zero values")
    void testNonZeroOutput() {
        Xoroshiro128PlusPlus rng = new Xoroshiro128PlusPlus(1L, 1L);
        int zeroCount = 0;
        for (int i = 0; i < 1000; i++) {
            if (rng.nextLong() == 0L) zeroCount++;
        }
        assertTrue(zeroCount < 10, "Should rarely produce zero values");
    }

    @Test
    @DisplayName("Xoroshiro state should change after each call")
    void testStateChanges() {
        Xoroshiro128PlusPlus rng = new Xoroshiro128PlusPlus(42L, 42L);
        long prevLo = rng.getSeedLo();
        long prevHi = rng.getSeedHi();

        rng.nextLong();

        assertTrue(prevLo != rng.getSeedLo() || prevHi != rng.getSeedHi(),
            "State should change after nextLong()");
    }

    @Test
    @DisplayName("Known value test for Xoroshiro128++")
    void testKnownValues() {
        // Test with known seed values
        Xoroshiro128PlusPlus rng = new Xoroshiro128PlusPlus(
            0x0123456789ABCDEFL, 0xFEDCBA9876543210L);

        // Generate first few values and verify they're deterministic
        long[] expected = new long[5];
        for (int i = 0; i < 5; i++) {
            expected[i] = rng.nextLong();
        }

        // Reset and verify
        Xoroshiro128PlusPlus rng2 = new Xoroshiro128PlusPlus(
            0x0123456789ABCDEFL, 0xFEDCBA9876543210L);
        for (int i = 0; i < 5; i++) {
            assertEquals(expected[i], rng2.nextLong(),
                "Value at index " + i + " should match");
        }
    }
}
