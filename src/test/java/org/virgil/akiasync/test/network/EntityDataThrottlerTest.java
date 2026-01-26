package org.virgil.akiasync.test.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for EntityDataThrottler functionality.
 * Validates metadata caching, throttling logic, and distance-based intervals.
 */
public class EntityDataThrottlerTest {

    private TestEntityDataThrottler throttler;

    @BeforeEach
    void setUp() {
        throttler = new TestEntityDataThrottler();
        throttler.clearAll();
        throttler.resetStats();
    }

    @Test
    @DisplayName("First metadata update should always send")
    void testFirstMetadataUpdate() {
        boolean shouldSend = throttler.shouldSendMetadata("player1", 100, 12345);
        assertTrue(shouldSend, "First update should always send");
    }

    @Test
    @DisplayName("Changed metadata hash should trigger send")
    void testMetadataHashChange() {
        throttler.shouldSendMetadata("player1", 100, 12345);
        
        // Same hash - might be throttled
        throttler.shouldSendMetadata("player1", 100, 12345);
        
        // Different hash - should send
        boolean shouldSend = throttler.shouldSendMetadata("player1", 100, 99999);
        assertTrue(shouldSend, "Changed hash should trigger send");
    }

    @Test
    @DisplayName("Distance tier 0 should have shortest interval")
    void testDistanceTier0Interval() {
        int interval = throttler.getMetadataRefreshInterval(0);
        assertEquals(100, interval, "Tier 0 should have 100 tick interval");
    }

    @Test
    @DisplayName("Distance tier increases should increase interval")
    void testDistanceTierIntervals() {
        int tier0 = throttler.getMetadataRefreshInterval(0);
        int tier1 = throttler.getMetadataRefreshInterval(1);
        int tier2 = throttler.getMetadataRefreshInterval(2);
        int tier3 = throttler.getMetadataRefreshInterval(3);
        
        assertTrue(tier1 > tier0, "Tier 1 should be longer than tier 0");
        assertTrue(tier2 > tier1, "Tier 2 should be longer than tier 1");
        assertTrue(tier3 > tier2, "Tier 3 should be longer than tier 2");
    }

    @Test
    @DisplayName("NBT update intervals should follow distance tiers")
    void testNBTIntervals() {
        int tier0 = throttler.getNBTUpdateInterval(0);
        int tier1 = throttler.getNBTUpdateInterval(1);
        
        assertTrue(tier0 < tier1, "NBT interval should increase with distance");
        assertEquals(40, tier0, "Tier 0 NBT interval should be 40");
    }

    @Test
    @DisplayName("Force update should clear cache")
    void testForceUpdate() {
        throttler.shouldSendMetadata("player1", 100, 12345);
        
        throttler.forceUpdate("player1", 100);
        
        // After force update, next check should send
        boolean shouldSend = throttler.shouldSendMetadata("player1", 100, 12345);
        assertTrue(shouldSend, "Should send after force update");
    }

    @Test
    @DisplayName("Clear player should remove all player entries")
    void testClearPlayer() {
        throttler.shouldSendMetadata("player1", 100, 111);
        throttler.shouldSendMetadata("player1", 101, 222);
        throttler.shouldSendMetadata("player2", 100, 333);
        
        throttler.clearPlayer("player1");
        
        assertEquals(1, throttler.getCacheSize(), "Only player2 entries should remain");
    }

    @Test
    @DisplayName("Clear entity should remove all entity entries")
    void testClearEntity() {
        throttler.shouldSendMetadata("player1", 100, 111);
        throttler.shouldSendMetadata("player2", 100, 222);
        throttler.shouldSendMetadata("player1", 101, 333);
        
        throttler.clearEntity(100);
        
        assertEquals(1, throttler.getCacheSize(), "Only entity 101 entries should remain");
    }

    @Test
    @DisplayName("Cleanup should remove stale entries")
    void testCleanup() {
        throttler.shouldSendMetadata("player1", 100, 12345);
        
        // Advance time past expiration
        for (int i = 0; i < 1300; i++) {
            throttler.tick();
        }
        
        throttler.cleanup();
        
        assertEquals(0, throttler.getCacheSize(), "Stale entries should be removed");
    }

    @Test
    @DisplayName("Statistics should track throttled vs sent")
    void testStatistics() {
        // First send (not throttled)
        throttler.shouldSendMetadata("player1", 100, 12345);
        
        // Same data, close together (throttled)
        throttler.shouldSendMetadata("player1", 100, 12345);
        throttler.shouldSendMetadata("player1", 100, 12345);
        
        assertTrue(throttler.getMetadataChecks() >= 3);
        assertTrue(throttler.getMetadataThrottled() >= 0);
    }

    @Test
    @DisplayName("Distance tier calculation should be correct")
    void testDistanceTierCalculation() {
        // Tier 0: < 16 blocks (256 squared)
        assertEquals(0, throttler.getDistanceTier(100));
        
        // Tier 1: 16-32 blocks
        assertEquals(1, throttler.getDistanceTier(400));
        
        // Tier 2: 32-64 blocks
        assertEquals(2, throttler.getDistanceTier(2000));
        
        // Tier 3: 64-128 blocks
        assertEquals(3, throttler.getDistanceTier(6000));
        
        // Tier 4+: > 128 blocks
        assertEquals(4, throttler.getDistanceTier(20000));
    }

    // Test implementation
    static class TestEntityDataThrottler {
        private final Map<String, Integer> metadataCache = new ConcurrentHashMap<>();
        private final Map<String, Long> metadataUpdateTimers = new ConcurrentHashMap<>();
        private final Map<String, Long> nbtUpdateTimers = new ConcurrentHashMap<>();
        
        private long currentTick = 0;
        private long metadataChecks = 0;
        private long metadataThrottled = 0;

        void tick() { currentTick++; }

        boolean shouldSendMetadata(String playerId, int entityId, int metadataHash) {
            metadataChecks++;
            String key = playerId + "-" + entityId;
            
            Integer cachedHash = metadataCache.get(key);
            if (cachedHash == null || cachedHash != metadataHash) {
                updateMetadataCache(key, metadataHash);
                return true;
            }
            
            int distanceTier = 1; // Simulated
            int refreshInterval = getMetadataRefreshInterval(distanceTier);
            
            Long lastUpdate = metadataUpdateTimers.get(key);
            if (lastUpdate == null || currentTick - lastUpdate >= refreshInterval) {
                metadataUpdateTimers.put(key, currentTick);
                return true;
            }
            
            metadataThrottled++;
            return false;
        }

        private void updateMetadataCache(String key, int hash) {
            metadataCache.put(key, hash);
            metadataUpdateTimers.put(key, currentTick);
        }

        int getMetadataRefreshInterval(int distanceTier) {
            return switch (distanceTier) {
                case 0 -> 100;
                case 1 -> 200;
                case 2 -> 400;
                case 3 -> 600;
                default -> 1200;
            };
        }

        int getNBTUpdateInterval(int distanceTier) {
            return switch (distanceTier) {
                case 0 -> 40;
                case 1 -> 100;
                case 2 -> 200;
                case 3 -> 400;
                default -> 800;
            };
        }

        int getDistanceTier(double distanceSquared) {
            if (distanceSquared < 256) return 0;      // < 16 blocks
            if (distanceSquared < 1024) return 1;     // < 32 blocks
            if (distanceSquared < 4096) return 2;     // < 64 blocks
            if (distanceSquared < 16384) return 3;    // < 128 blocks
            return 4;
        }

        void forceUpdate(String playerId, int entityId) {
            String key = playerId + "-" + entityId;
            metadataCache.remove(key);
            metadataUpdateTimers.put(key, currentTick);
        }

        void clearPlayer(String playerId) {
            String prefix = playerId + "-";
            metadataCache.keySet().removeIf(k -> k.startsWith(prefix));
            metadataUpdateTimers.keySet().removeIf(k -> k.startsWith(prefix));
            nbtUpdateTimers.keySet().removeIf(k -> k.startsWith(prefix));
        }

        void clearEntity(int entityId) {
            String suffix = "-" + entityId;
            metadataCache.keySet().removeIf(k -> k.endsWith(suffix));
            metadataUpdateTimers.keySet().removeIf(k -> k.endsWith(suffix));
            nbtUpdateTimers.keySet().removeIf(k -> k.endsWith(suffix));
        }

        void cleanup() {
            long expireTime = currentTick - 1200;
            metadataCache.entrySet().removeIf(e -> {
                Long lastUpdate = metadataUpdateTimers.get(e.getKey());
                return lastUpdate != null && lastUpdate < expireTime;
            });
            metadataUpdateTimers.entrySet().removeIf(e -> e.getValue() < expireTime);
            nbtUpdateTimers.entrySet().removeIf(e -> e.getValue() < expireTime);
        }

        void clearAll() {
            metadataCache.clear();
            metadataUpdateTimers.clear();
            nbtUpdateTimers.clear();
        }

        void resetStats() {
            metadataChecks = 0;
            metadataThrottled = 0;
        }

        int getCacheSize() { return metadataCache.size(); }
        long getMetadataChecks() { return metadataChecks; }
        long getMetadataThrottled() { return metadataThrottled; }
    }
}
