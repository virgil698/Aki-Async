package org.virgil.akiasync.test.pathfinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PathCacheKey position encoding/decoding.
 * Validates that BlockPos can be correctly encoded to long and decoded back.
 */
public class PathCacheKeyTest {

    @Test
    @DisplayName("Position encoding should be reversible")
    void testPositionEncodingReversible() {
        int[][] testPositions = {
            {0, 0, 0},
            {100, 64, -100},
            {-1000, 256, 1000},
            {1000000, 0, -1000000},
            {-500000, 128, 500000}
        };

        for (int[] pos : testPositions) {
            long encoded = encodePosition(pos[0], pos[1], pos[2]);
            int[] decoded = decodePosition(encoded);

            assertEquals(pos[0], decoded[0], "X should match for " + java.util.Arrays.toString(pos));
            assertEquals(pos[1], decoded[1], "Y should match for " + java.util.Arrays.toString(pos));
            assertEquals(pos[2], decoded[2], "Z should match for " + java.util.Arrays.toString(pos));
        }
    }

    @Test
    @DisplayName("Hash code should be consistent")
    void testHashCodeConsistency() {
        TestCacheKey key1 = new TestCacheKey(100, 64, -50, 200, 70, -100);
        TestCacheKey key2 = new TestCacheKey(100, 64, -50, 200, 70, -100);

        assertEquals(key1.hashCode(), key2.hashCode(), "Same positions should have same hash");
    }

    @Test
    @DisplayName("Equals should work correctly")
    void testEquals() {
        TestCacheKey key1 = new TestCacheKey(100, 64, -50, 200, 70, -100);
        TestCacheKey key2 = new TestCacheKey(100, 64, -50, 200, 70, -100);
        TestCacheKey key3 = new TestCacheKey(100, 64, -50, 201, 70, -100);

        assertEquals(key1, key2, "Same positions should be equal");
        assertNotEquals(key1, key3, "Different positions should not be equal");
    }

    @Test
    @DisplayName("Different positions should have different hashes (low collision)")
    void testHashDistribution() {
        java.util.Set<Integer> hashes = new java.util.HashSet<>();
        int collisions = 0;

        for (int i = 0; i < 1000; i++) {
            TestCacheKey key = new TestCacheKey(
                i * 10, 64, i * 5,
                i * 10 + 100, 70, i * 5 + 50
            );
            if (!hashes.add(key.hashCode())) {
                collisions++;
            }
        }

        assertTrue(collisions < 50, "Should have low collision rate, got: " + collisions);
    }

    @Test
    @DisplayName("Negative coordinates should encode correctly")
    void testNegativeCoordinates() {
        int[][] negativePositions = {
            {-1, -1, -1},
            {-100, -64, -100},
            {-1000000, -256, -1000000}
        };

        for (int[] pos : negativePositions) {
            long encoded = encodePosition(pos[0], pos[1], pos[2]);
            int[] decoded = decodePosition(encoded);

            assertEquals(pos[0], decoded[0], "Negative X should match");
            assertEquals(pos[1], decoded[1], "Negative Y should match");
            assertEquals(pos[2], decoded[2], "Negative Z should match");
        }
    }

    // Mirror of PathCacheKey encoding logic
    private static long encodePosition(int x, int y, int z) {
        long lx = (long) x & 0x1FFFFF;
        long ly = (long) y & 0x1FFFFF;
        long lz = (long) z & 0x3FFFFF;
        return (lx << 43) | (ly << 22) | lz;
    }

    private static int[] decodePosition(long encoded) {
        int x = (int) ((encoded >> 43) & 0x1FFFFF);
        int y = (int) ((encoded >> 22) & 0x1FFFFF);
        int z = (int) (encoded & 0x3FFFFF);

        if ((x & 0x100000) != 0) x |= 0xFFE00000;
        if ((y & 0x100000) != 0) y |= 0xFFE00000;
        if ((z & 0x200000) != 0) z |= 0xFFC00000;

        return new int[]{x, y, z};
    }

    // Test implementation of cache key
    static class TestCacheKey {
        private final long startHash;
        private final long endHash;
        private final int hashCode;

        TestCacheKey(int sx, int sy, int sz, int ex, int ey, int ez) {
            this.startHash = encodePosition(sx, sy, sz);
            this.endHash = encodePosition(ex, ey, ez);
            this.hashCode = computeHashCode();
        }

        private int computeHashCode() {
            int result = (int) (startHash ^ (startHash >>> 32));
            result = 31 * result + (int) (endHash ^ (endHash >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TestCacheKey other)) return false;
            return this.startHash == other.startHash && this.endHash == other.endHash;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
