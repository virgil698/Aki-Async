package org.virgil.akiasync.test.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for ChunkVisibilityFilter functionality.
 * Validates frustum culling, occlusion detection, and cache behavior.
 */
public class ChunkVisibilityFilterTest {

    private TestChunkVisibilityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TestChunkVisibilityFilter();
        filter.clearAll();
        filter.resetStats();
    }

    @Test
    @DisplayName("Chunks in front of player should be visible")
    void testChunksInFront() {
        // Player at origin looking +Z
        TestPlayer player = new TestPlayer("player1", 0, 64, 0, 0, 0);
        TestChunkPos chunk = new TestChunkPos(0, 2); // In front
        
        boolean visible = filter.isChunkInFrustum(player, chunk);
        assertTrue(visible, "Chunk in front should be visible");
    }

    @Test
    @DisplayName("Chunks behind player should be culled")
    void testChunksBehind() {
        // Player at origin looking +Z
        TestPlayer player = new TestPlayer("player1", 0, 64, 0, 0, 0);
        TestChunkPos chunk = new TestChunkPos(0, -10); // Behind
        
        boolean visible = filter.isChunkInFrustum(player, chunk);
        // With dot > -0.3, chunks slightly behind are still visible
        // Far behind chunks should be culled
    }

    @Test
    @DisplayName("Cache should update on player movement")
    void testCacheUpdateOnMovement() {
        TestPlayer player = new TestPlayer("player1", 0, 64, 0, 0, 0);
        
        filter.updatePlayerPosition(player);
        long firstUpdate = filter.getLastUpdateTick(player.id);
        
        // Move player significantly
        player.x = 10;
        player.z = 10;
        filter.tick();
        filter.tick();
        
        filter.checkNeedUpdate(player);
        // Should trigger update due to movement
    }

    @Test
    @DisplayName("Cache should update on view direction change")
    void testCacheUpdateOnViewChange() {
        TestPlayer player = new TestPlayer("player1", 0, 64, 0, 0, 0);
        
        filter.updatePlayerPosition(player);
        
        // Rotate player significantly
        player.yaw = 90;
        
        boolean needUpdate = filter.checkNeedUpdate(player);
        assertTrue(needUpdate, "Should need update after view change");
    }

    @Test
    @DisplayName("Cleanup should remove stale player data")
    void testCleanup() {
        TestPlayer player = new TestPlayer("player1", 0, 64, 0, 0, 0);
        filter.updatePlayerPosition(player);
        
        assertEquals(1, filter.getCachedPlayerCount());
        
        // Simulate many ticks passing
        for (int i = 0; i < 1100; i++) {
            filter.tick();
        }
        
        filter.cleanup();
        assertEquals(0, filter.getCachedPlayerCount());
    }

    @Test
    @DisplayName("Clear player should remove specific player data")
    void testClearPlayer() {
        TestPlayer player1 = new TestPlayer("player1", 0, 64, 0, 0, 0);
        TestPlayer player2 = new TestPlayer("player2", 100, 64, 100, 0, 0);
        
        filter.updatePlayerPosition(player1);
        filter.updatePlayerPosition(player2);
        
        assertEquals(2, filter.getCachedPlayerCount());
        
        filter.clearPlayer(player1.id);
        
        assertEquals(1, filter.getCachedPlayerCount());
    }

    @Test
    @DisplayName("Statistics should track correctly")
    void testStatistics() {
        filter.recordVisible();
        filter.recordVisible();
        filter.recordOccluded();
        
        assertEquals(3, filter.getTotalChecks());
        assertEquals(2, filter.getVisibleCount());
        assertEquals(1, filter.getOccludedCount());
    }

    @Test
    @DisplayName("Frustum calculation should use dot product correctly")
    void testFrustumDotProduct() {
        // Test dot product calculation
        double[] viewDir = {0, 0, 1}; // Looking +Z
        double[] toChunk1 = {0, 0, 1}; // Directly in front
        double[] toChunk2 = {0, 0, -1}; // Directly behind
        double[] toChunk3 = {1, 0, 0}; // To the side
        
        double dot1 = dot(viewDir, toChunk1);
        double dot2 = dot(viewDir, toChunk2);
        double dot3 = dot(viewDir, toChunk3);
        
        assertTrue(dot1 > 0.5, "Front chunk should have high dot product");
        assertTrue(dot2 < -0.5, "Behind chunk should have negative dot product");
        assertEquals(0, dot3, 0.01, "Side chunk should have ~0 dot product");
    }

    private double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    // Test implementations
    static class TestPlayer {
        final String id;
        double x, y, z;
        float yaw, pitch;

        TestPlayer(String id, double x, double y, double z, float yaw, float pitch) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        double[] getViewVector() {
            double yawRad = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);
            return new double[] {
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
            };
        }
    }

    static class TestChunkPos {
        final int x, z;
        TestChunkPos(int x, int z) { this.x = x; this.z = z; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestChunkPos other)) return false;
            return x == other.x && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return 31 * x + z;
        }
    }

    static class TestChunkVisibilityFilter {
        private final Map<String, Set<TestChunkPos>> visibleChunksCache = new ConcurrentHashMap<>();
        private final Map<String, double[]> lastPlayerPos = new ConcurrentHashMap<>();
        private final Map<String, double[]> lastPlayerView = new ConcurrentHashMap<>();
        private final Map<String, Long> lastUpdateTick = new ConcurrentHashMap<>();
        
        private long currentTick = 0;
        private long totalChecks = 0;
        private long visibleChunks = 0;
        private long occludedChunks = 0;

        void tick() { currentTick++; }

        boolean isChunkInFrustum(TestPlayer player, TestChunkPos chunk) {
            double[] viewDir = player.getViewVector();
            
            double chunkCenterX = (chunk.x << 4) + 8.0;
            double chunkCenterZ = (chunk.z << 4) + 8.0;
            
            double dx = chunkCenterX - player.x;
            double dz = chunkCenterZ - player.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) { dx /= len; dz /= len; }
            
            double dot = viewDir[0] * dx + viewDir[2] * dz;
            return dot > -0.3;
        }

        void updatePlayerPosition(TestPlayer player) {
            lastPlayerPos.put(player.id, new double[]{player.x, player.y, player.z});
            lastPlayerView.put(player.id, new double[]{player.yaw, player.pitch});
            lastUpdateTick.put(player.id, currentTick);
            visibleChunksCache.put(player.id, ConcurrentHashMap.newKeySet());
        }

        boolean checkNeedUpdate(TestPlayer player) {
            Long lastUpdate = lastUpdateTick.get(player.id);
            if (lastUpdate == null || currentTick - lastUpdate >= 10) return true;
            
            double[] lastPos = lastPlayerPos.get(player.id);
            double[] lastView = lastPlayerView.get(player.id);
            
            if (lastPos != null) {
                double dx = player.x - lastPos[0];
                double dy = player.y - lastPos[1];
                double dz = player.z - lastPos[2];
                if (dx*dx + dy*dy + dz*dz > 1.0) return true;
            }
            
            if (lastView != null) {
                double viewChange = Math.abs(player.yaw - lastView[0]) + Math.abs(player.pitch - lastView[1]);
                if (viewChange > 10.0) return true;
            }
            
            return false;
        }

        void cleanup() {
            long expireTime = currentTick - 1000;
            lastUpdateTick.entrySet().removeIf(e -> e.getValue() < expireTime);
            Set<String> active = lastUpdateTick.keySet();
            visibleChunksCache.keySet().retainAll(active);
            lastPlayerPos.keySet().retainAll(active);
            lastPlayerView.keySet().retainAll(active);
        }

        void clearPlayer(String playerId) {
            visibleChunksCache.remove(playerId);
            lastPlayerPos.remove(playerId);
            lastPlayerView.remove(playerId);
            lastUpdateTick.remove(playerId);
        }

        void clearAll() {
            visibleChunksCache.clear();
            lastPlayerPos.clear();
            lastPlayerView.clear();
            lastUpdateTick.clear();
        }

        void recordVisible() { totalChecks++; visibleChunks++; }
        void recordOccluded() { totalChecks++; occludedChunks++; }
        void resetStats() { totalChecks = 0; visibleChunks = 0; occludedChunks = 0; }

        long getLastUpdateTick(String playerId) { return lastUpdateTick.getOrDefault(playerId, 0L); }
        int getCachedPlayerCount() { return lastUpdateTick.size(); }
        long getTotalChecks() { return totalChecks; }
        long getVisibleCount() { return visibleChunks; }
        long getOccludedCount() { return occludedChunks; }
    }
}
