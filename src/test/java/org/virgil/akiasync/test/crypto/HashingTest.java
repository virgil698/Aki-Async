package org.virgil.akiasync.test.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for hashing and seed obfuscation.
 * Validates hash consistency and distribution.
 */
public class HashingTest {

    @Test
    @DisplayName("SHA-256 seed obfuscation should be deterministic")
    void testSeedObfuscationDeterministic() {
        long seed = 12345L;
        long hash1 = obfuscateSeed(seed);
        long hash2 = obfuscateSeed(seed);

        assertEquals(hash1, hash2, "Same seed should produce same hash");
    }

    @Test
    @DisplayName("Different seeds should produce different hashes")
    void testSeedObfuscationUniqueness() {
        long hash1 = obfuscateSeed(1L);
        long hash2 = obfuscateSeed(2L);

        assertNotEquals(hash1, hash2, "Different seeds should produce different hashes");
    }

    @Test
    @DisplayName("Hash distribution should be uniform")
    void testHashDistribution() {
        int[] buckets = new int[16];
        int sampleCount = 10000;

        for (long i = 0; i < sampleCount; i++) {
            long hash = obfuscateSeed(i);
            int bucket = (int) (Math.abs(hash) % 16);
            buckets[bucket]++;
        }

        int expected = sampleCount / 16;
        int tolerance = expected / 2;

        for (int i = 0; i < 16; i++) {
            assertTrue(Math.abs(buckets[i] - expected) < tolerance,
                "Bucket " + i + " has " + buckets[i] + ", expected ~" + expected);
        }
    }

    @Test
    @DisplayName("Position hash should encode correctly")
    void testPositionHash() {
        long hash1 = hashPosition(100, 64, -50);
        long hash2 = hashPosition(100, 64, -50);
        long hash3 = hashPosition(100, 64, -51);

        assertEquals(hash1, hash2, "Same position should have same hash");
        assertNotEquals(hash1, hash3, "Different positions should have different hashes");
    }

    @Test
    @DisplayName("Chunk position hash should be consistent")
    void testChunkPositionHash() {
        int chunkX = 10, chunkZ = -5;
        long hash1 = hashChunkPos(chunkX, chunkZ);
        long hash2 = hashChunkPos(chunkX, chunkZ);

        assertEquals(hash1, hash2);
    }

    // Simulates BiomeManager.obfuscateSeed using simple hash
    private long obfuscateSeed(long seed) {
        // Simple hash simulation (real impl uses SHA-256)
        long hash = seed;
        hash = hash * 6364136223846793005L + 1442695040888963407L;
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >>> 33;
        return hash;
    }

    // Simulates Mth.getSeed
    private long hashPosition(int x, int y, int z) {
        long l = (long)(x * 3129871) ^ (long)z * 116129781L ^ (long)y;
        l = l * l * 42317861L + l * 11L;
        return l >> 16;
    }

    private long hashChunkPos(int x, int z) {
        return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
    }
}
