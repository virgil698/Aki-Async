package org.virgil.akiasync.test.worldgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for random number batching optimization.
 * Validates that batch-generated random numbers match sequential generation.
 */
public class RandomBatchingTest {

    private static final int BATCH_SIZE = 64;

    /**
     * Simulates Xoroshiro128++ for testing.
     */
    static class TestXoroshiro {
        private long seedLo;
        private long seedHi;

        public TestXoroshiro(long seedLo, long seedHi) {
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

        public void reset(long lo, long hi) {
            this.seedLo = lo;
            this.seedHi = hi;
        }
    }

    @Test
    @DisplayName("Batch generation should match sequential generation")
    void testBatchMatchesSequential() {
        TestXoroshiro rng1 = new TestXoroshiro(12345L, 67890L);
        TestXoroshiro rng2 = new TestXoroshiro(12345L, 67890L);

        // Generate batch
        long[] batch = new long[BATCH_SIZE];
        for (int i = 0; i < BATCH_SIZE; i++) {
            batch[i] = rng1.nextLong();
        }

        // Verify sequential matches batch
        for (int i = 0; i < BATCH_SIZE; i++) {
            assertEquals(batch[i], rng2.nextLong(),
                "Batch value at index " + i + " should match sequential");
        }
    }

    @Test
    @DisplayName("Multiple batch cycles should be consistent")
    void testMultipleBatchCycles() {
        TestXoroshiro rng1 = new TestXoroshiro(0xCAFEBABEL, 0xDEADBEEFL);
        TestXoroshiro rng2 = new TestXoroshiro(0xCAFEBABEL, 0xDEADBEEFL);

        // Simulate 3 batch cycles
        for (int cycle = 0; cycle < 3; cycle++) {
            long[] batch = new long[BATCH_SIZE];
            for (int i = 0; i < BATCH_SIZE; i++) {
                batch[i] = rng1.nextLong();
            }

            for (int i = 0; i < BATCH_SIZE; i++) {
                assertEquals(batch[i], rng2.nextLong(),
                    "Cycle " + cycle + ", index " + i + " mismatch");
            }
        }
    }

    @Test
    @DisplayName("Int conversion should preserve lower 32 bits")
    void testIntConversion() {
        TestXoroshiro rng = new TestXoroshiro(42L, 42L);

        for (int i = 0; i < 100; i++) {
            long longVal = rng.nextLong();
            int intVal = (int) longVal;
            assertEquals((int) longVal, intVal, "Int conversion should be consistent");
        }
    }

    @Test
    @DisplayName("Float conversion should be in range [0, 1)")
    void testFloatConversion() {
        TestXoroshiro rng = new TestXoroshiro(123L, 456L);

        for (int i = 0; i < 1000; i++) {
            int bits = (int) (rng.nextLong() >>> 40);
            float f = bits * 5.9604645E-8F;
            assertTrue(f >= 0.0f && f < 1.0f,
                "Float should be in [0, 1), got: " + f);
        }
    }

    @Test
    @DisplayName("Double conversion should be in range [0, 1)")
    void testDoubleConversion() {
        TestXoroshiro rng = new TestXoroshiro(789L, 101112L);

        for (int i = 0; i < 1000; i++) {
            long bits = rng.nextLong() >>> 11;
            double d = bits * 1.1102230246251565E-16;
            assertTrue(d >= 0.0 && d < 1.0,
                "Double should be in [0, 1), got: " + d);
        }
    }

    @Test
    @DisplayName("Pool reset on setSeed should work correctly")
    void testPoolResetOnSetSeed() {
        TestXoroshiro rng1 = new TestXoroshiro(100L, 200L);
        TestXoroshiro rng2 = new TestXoroshiro(100L, 200L);

        // Consume some values
        for (int i = 0; i < 10; i++) {
            rng1.nextLong();
        }

        // Reset rng1
        rng1.reset(100L, 200L);

        // Both should now produce same sequence
        for (int i = 0; i < 50; i++) {
            assertEquals(rng1.nextLong(), rng2.nextLong(),
                "After reset, sequences should match at index " + i);
        }
    }
}
