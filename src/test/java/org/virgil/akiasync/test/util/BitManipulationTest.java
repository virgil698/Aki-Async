package org.virgil.akiasync.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for bit manipulation utilities used in position encoding and hashing.
 * Validates bit operations, masking, and shifting.
 */
public class BitManipulationTest {

    @Test
    @DisplayName("Long rotation should be correct")
    void testLongRotation() {
        long value = 0x123456789ABCDEF0L;
        
        // Rotate left then right should restore original
        long rotatedLeft = Long.rotateLeft(value, 17);
        long restored = Long.rotateRight(rotatedLeft, 17);
        
        assertEquals(value, restored);
    }

    @Test
    @DisplayName("Bit masking should extract correct bits")
    void testBitMasking() {
        long value = 0xABCDEF1234567890L;
        
        // Extract lower 32 bits
        int lower32 = (int) value;
        assertEquals(0x34567890, lower32);
        
        // Extract upper 32 bits
        int upper32 = (int) (value >>> 32);
        assertEquals(0xABCDEF12, upper32);
    }

    @Test
    @DisplayName("Sign extension should work correctly")
    void testSignExtension() {
        // 21-bit value with sign bit set
        int value21bit = 0x1FFFFF; // All 1s in 21 bits
        
        // Check if sign bit is set
        boolean signBitSet = (value21bit & 0x100000) != 0;
        assertTrue(signBitSet);
        
        // Sign extend to 32 bits
        int signExtended = value21bit;
        if ((value21bit & 0x100000) != 0) {
            signExtended |= 0xFFE00000;
        }
        
        assertEquals(-1, signExtended);
    }

    @Test
    @DisplayName("Block position encoding should use correct bit layout")
    void testBlockPosEncoding() {
        // Minecraft BlockPos uses: X(26 bits) | Z(26 bits) | Y(12 bits)
        int x = 100, y = 64, z = -50;
        
        long encoded = encodeBlockPos(x, y, z);
        int[] decoded = decodeBlockPos(encoded);
        
        assertEquals(x, decoded[0]);
        assertEquals(y, decoded[1]);
        assertEquals(z, decoded[2]);
    }

    @Test
    @DisplayName("Chunk position encoding should pack correctly")
    void testChunkPosEncoding() {
        int chunkX = 100, chunkZ = -200;
        
        long packed = ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
        
        int unpackedX = (int) packed;
        int unpackedZ = (int) (packed >> 32);
        
        assertEquals(chunkX, unpackedX);
        assertEquals(chunkZ, unpackedZ);
    }

    @Test
    @DisplayName("Section index calculation should be correct")
    void testSectionIndex() {
        // Y range: -64 to 319 (384 blocks, 24 sections)
        int minY = -64;
        
        assertEquals(0, getSectionIndex(-64, minY));
        assertEquals(0, getSectionIndex(-49, minY));
        assertEquals(1, getSectionIndex(-48, minY));
        assertEquals(4, getSectionIndex(0, minY));
        assertEquals(8, getSectionIndex(64, minY));
    }

    @Test
    @DisplayName("Bit counting should be accurate")
    void testBitCounting() {
        assertEquals(0, Long.bitCount(0L));
        assertEquals(1, Long.bitCount(1L));
        assertEquals(64, Long.bitCount(-1L));
        assertEquals(32, Long.bitCount(0xAAAAAAAAAAAAAAAAL));
    }

    @Test
    @DisplayName("Leading zeros should be correct")
    void testLeadingZeros() {
        assertEquals(64, Long.numberOfLeadingZeros(0L));
        assertEquals(63, Long.numberOfLeadingZeros(1L));
        assertEquals(0, Long.numberOfLeadingZeros(-1L));
        assertEquals(32, Long.numberOfLeadingZeros(0xFFFFFFFFL));
    }

    @Test
    @DisplayName("Trailing zeros should be correct")
    void testTrailingZeros() {
        assertEquals(64, Long.numberOfTrailingZeros(0L));
        assertEquals(0, Long.numberOfTrailingZeros(1L));
        assertEquals(0, Long.numberOfTrailingZeros(-1L));
        assertEquals(4, Long.numberOfTrailingZeros(0x10L));
    }

    @Test
    @DisplayName("Power of two check should be correct")
    void testPowerOfTwo() {
        assertTrue(isPowerOfTwo(1));
        assertTrue(isPowerOfTwo(2));
        assertTrue(isPowerOfTwo(16));
        assertTrue(isPowerOfTwo(1024));
        
        assertFalse(isPowerOfTwo(0));
        assertFalse(isPowerOfTwo(3));
        assertFalse(isPowerOfTwo(15));
    }

    @Test
    @DisplayName("Next power of two should be correct")
    void testNextPowerOfTwo() {
        assertEquals(1, nextPowerOfTwo(1));
        assertEquals(2, nextPowerOfTwo(2));
        assertEquals(4, nextPowerOfTwo(3));
        assertEquals(16, nextPowerOfTwo(9));
        assertEquals(1024, nextPowerOfTwo(1000));
    }

    @Test
    @DisplayName("XOR swap should work correctly")
    void testXorSwap() {
        int a = 42, b = 17;
        
        a = a ^ b;
        b = a ^ b;
        a = a ^ b;
        
        assertEquals(17, a);
        assertEquals(42, b);
    }

    @Test
    @DisplayName("Byte reversal should be correct")
    void testByteReversal() {
        long value = 0x0102030405060708L;
        long reversed = Long.reverseBytes(value);
        
        assertEquals(0x0807060504030201L, reversed);
    }

    // Helper methods
    private long encodeBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | 
               ((long) (z & 0x3FFFFFF) << 12) | 
               (y & 0xFFF);
    }

    private int[] decodeBlockPos(long packed) {
        int x = (int) (packed >> 38);
        int z = (int) (packed << 26 >> 38);
        int y = (int) (packed << 52 >> 52);
        return new int[]{x, y, z};
    }

    private int getSectionIndex(int y, int minY) {
        return (y - minY) >> 4;
    }

    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    private int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
}
