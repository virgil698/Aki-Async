package org.virgil.akiasync.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for chunk position calculations and conversions.
 * Validates block-to-chunk, chunk-to-region, and position encoding.
 */
public class ChunkPositionTest {

    @Test
    @DisplayName("Block to chunk conversion should be correct")
    void testBlockToChunk() {
        // Positive coordinates
        assertEquals(0, blockToChunk(0));
        assertEquals(0, blockToChunk(15));
        assertEquals(1, blockToChunk(16));
        assertEquals(1, blockToChunk(31));
        assertEquals(2, blockToChunk(32));
        
        // Negative coordinates
        assertEquals(-1, blockToChunk(-1));
        assertEquals(-1, blockToChunk(-16));
        assertEquals(-2, blockToChunk(-17));
        assertEquals(-2, blockToChunk(-32));
    }

    @Test
    @DisplayName("Chunk to region conversion should be correct")
    void testChunkToRegion() {
        assertEquals(0, chunkToRegion(0));
        assertEquals(0, chunkToRegion(31));
        assertEquals(1, chunkToRegion(32));
        assertEquals(-1, chunkToRegion(-1));
        assertEquals(-1, chunkToRegion(-32));
        assertEquals(-2, chunkToRegion(-33));
    }

    @Test
    @DisplayName("Block position within chunk should be correct")
    void testBlockPositionInChunk() {
        assertEquals(0, blockPosInChunk(0));
        assertEquals(15, blockPosInChunk(15));
        assertEquals(0, blockPosInChunk(16));
        assertEquals(5, blockPosInChunk(21));
        
        // Negative coordinates
        assertEquals(15, blockPosInChunk(-1));
        assertEquals(0, blockPosInChunk(-16));
        assertEquals(15, blockPosInChunk(-17));
    }

    @Test
    @DisplayName("Chunk position encoding should be reversible")
    void testChunkPositionEncoding() {
        int[][] testPositions = {
            {0, 0},
            {100, -50},
            {-1000, 1000},
            {Integer.MAX_VALUE >> 5, Integer.MIN_VALUE >> 5}
        };
        
        for (int[] pos : testPositions) {
            long encoded = encodeChunkPos(pos[0], pos[1]);
            int[] decoded = decodeChunkPos(encoded);
            
            assertEquals(pos[0], decoded[0], "X should match for " + java.util.Arrays.toString(pos));
            assertEquals(pos[1], decoded[1], "Z should match for " + java.util.Arrays.toString(pos));
        }
    }

    @Test
    @DisplayName("Section Y calculation should be correct")
    void testSectionY() {
        assertEquals(0, blockToSection(0));
        assertEquals(0, blockToSection(15));
        assertEquals(1, blockToSection(16));
        assertEquals(-1, blockToSection(-1));
        assertEquals(-1, blockToSection(-16));
        assertEquals(-2, blockToSection(-17));
    }

    @Test
    @DisplayName("Chunk center calculation should be correct")
    void testChunkCenter() {
        // Chunk 0,0 center should be at 8,8
        int[] center = getChunkCenter(0, 0);
        assertEquals(8, center[0]);
        assertEquals(8, center[1]);
        
        // Chunk 1,1 center should be at 24,24
        center = getChunkCenter(1, 1);
        assertEquals(24, center[0]);
        assertEquals(24, center[1]);
        
        // Chunk -1,-1 center should be at -8,-8
        center = getChunkCenter(-1, -1);
        assertEquals(-8, center[0]);
        assertEquals(-8, center[1]);
    }

    @Test
    @DisplayName("Manhattan distance between chunks should be correct")
    void testChunkManhattanDistance() {
        assertEquals(0, chunkManhattanDistance(0, 0, 0, 0));
        assertEquals(2, chunkManhattanDistance(0, 0, 1, 1));
        assertEquals(10, chunkManhattanDistance(0, 0, 5, 5));
        assertEquals(20, chunkManhattanDistance(-5, -5, 5, 5));
    }

    @Test
    @DisplayName("Chebyshev distance between chunks should be correct")
    void testChunkChebyshevDistance() {
        assertEquals(0, chunkChebyshevDistance(0, 0, 0, 0));
        assertEquals(1, chunkChebyshevDistance(0, 0, 1, 1));
        assertEquals(5, chunkChebyshevDistance(0, 0, 5, 3));
        assertEquals(10, chunkChebyshevDistance(-5, -5, 5, 5));
    }

    @Test
    @DisplayName("Chunk in view distance check should be correct")
    void testChunkInViewDistance() {
        int viewDistance = 10;
        int playerChunkX = 0, playerChunkZ = 0;
        
        // Within view distance
        assertTrue(isChunkInViewDistance(0, 0, playerChunkX, playerChunkZ, viewDistance));
        assertTrue(isChunkInViewDistance(5, 5, playerChunkX, playerChunkZ, viewDistance));
        assertTrue(isChunkInViewDistance(-10, 0, playerChunkX, playerChunkZ, viewDistance));
        
        // Outside view distance
        assertFalse(isChunkInViewDistance(11, 0, playerChunkX, playerChunkZ, viewDistance));
        assertFalse(isChunkInViewDistance(0, 11, playerChunkX, playerChunkZ, viewDistance));
    }

    @Test
    @DisplayName("Region file name calculation should be correct")
    void testRegionFileName() {
        assertEquals("r.0.0.mca", getRegionFileName(0, 0));
        assertEquals("r.1.0.mca", getRegionFileName(32, 0));
        assertEquals("r.-1.-1.mca", getRegionFileName(-1, -1));
        assertEquals("r.-2.1.mca", getRegionFileName(-33, 32));
    }

    // Helper methods
    private int blockToChunk(int blockPos) {
        return blockPos >> 4;
    }

    private int chunkToRegion(int chunkPos) {
        return chunkPos >> 5;
    }

    private int blockPosInChunk(int blockPos) {
        return blockPos & 15;
    }

    private int blockToSection(int blockY) {
        return blockY >> 4;
    }

    private long encodeChunkPos(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    private int[] decodeChunkPos(long encoded) {
        int x = (int) encoded;
        int z = (int) (encoded >> 32);
        return new int[]{x, z};
    }

    private int[] getChunkCenter(int chunkX, int chunkZ) {
        return new int[]{
            (chunkX << 4) + 8,
            (chunkZ << 4) + 8
        };
    }

    private int chunkManhattanDistance(int x1, int z1, int x2, int z2) {
        return Math.abs(x2 - x1) + Math.abs(z2 - z1);
    }

    private int chunkChebyshevDistance(int x1, int z1, int x2, int z2) {
        return Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
    }

    private boolean isChunkInViewDistance(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ, int viewDistance) {
        return chunkChebyshevDistance(chunkX, chunkZ, playerChunkX, playerChunkZ) <= viewDistance;
    }

    private String getRegionFileName(int chunkX, int chunkZ) {
        int regionX = chunkToRegion(chunkX);
        int regionZ = chunkToRegion(chunkZ);
        return "r." + regionX + "." + regionZ + ".mca";
    }
}
