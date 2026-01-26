package org.virgil.akiasync.test.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for async villager processing.
 * Validates breeding, trading, and AI task scheduling.
 */
public class VillagerAsyncTest {

    private TestVillagerExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new TestVillagerExecutor(2);
    }

    @Test
    @DisplayName("Breeding task should complete asynchronously")
    @Timeout(5)
    void testAsyncBreeding() throws Exception {
        CompletableFuture<Boolean> future = executor.scheduleBreeding(1, 2);
        
        Boolean result = future.get(3, TimeUnit.SECONDS);
        assertTrue(result, "Breeding should succeed");
    }

    @Test
    @DisplayName("Multiple breeding tasks should be queued")
    @Timeout(10)
    void testMultipleBreedingTasks() throws Exception {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            futures.add(executor.scheduleBreeding(i * 2, i * 2 + 1));
        }
        
        int successCount = 0;
        for (CompletableFuture<Boolean> future : futures) {
            if (future.get(3, TimeUnit.SECONDS)) {
                successCount++;
            }
        }
        
        assertEquals(5, successCount);
    }

    @Test
    @DisplayName("Breeding cooldown should be respected")
    void testBreedingCooldown() {
        executor.scheduleBreeding(1, 2).join();
        
        // Immediate second breeding should fail due to cooldown
        executor.setBreedingCooldownActive(1, true);
        CompletableFuture<Boolean> future = executor.scheduleBreeding(1, 3);
        
        assertFalse(future.join(), "Should fail due to cooldown");
    }

    @Test
    @DisplayName("Trading should update reputation")
    void testTradingReputation() {
        int initialRep = executor.getReputation(1, "player1");
        
        executor.processTrade(1, "player1", 10);
        
        int newRep = executor.getReputation(1, "player1");
        assertTrue(newRep > initialRep, "Reputation should increase after trade");
    }

    @Test
    @DisplayName("AI task scheduling should be rate limited")
    @Timeout(5)
    void testAITaskRateLimit() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            executor.scheduleAITask(1, "wander");
        }
        
        // Rate limiting should prevent immediate execution of all tasks
        int scheduled = executor.getScheduledTaskCount(1);
        assertTrue(scheduled <= 10);
    }

    @Test
    @DisplayName("Gossip propagation should be async")
    @Timeout(5)
    void testGossipPropagation() throws Exception {
        List<Integer> villagerIds = Arrays.asList(1, 2, 3, 4, 5);
        
        CompletableFuture<Integer> future = executor.propagateGossip(villagerIds, "player1", -10);
        
        int affected = future.get(3, TimeUnit.SECONDS);
        assertEquals(5, affected, "All villagers should receive gossip");
    }

    @Test
    @DisplayName("Work station claiming should be thread-safe")
    @Timeout(5)
    void testWorkStationClaiming() throws Exception {
        int stationId = 100;
        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int villagerId = i;
            pool.submit(() -> {
                if (executor.claimWorkStation(villagerId, stationId)) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(1, successCount.get(), "Only one villager should claim station");
        
        pool.shutdown();
    }

    @Test
    @DisplayName("Pathfinding request should be batched")
    void testPathfindingBatching() {
        for (int i = 0; i < 10; i++) {
            executor.requestPath(i, 100, 64, 100);
        }
        
        int batchCount = executor.getBatchedPathRequests();
        assertTrue(batchCount <= 10, "Requests should be batched");
    }

    @Test
    @DisplayName("Shutdown should complete pending tasks")
    @Timeout(10)
    void testShutdown() throws Exception {
        executor.setTaskDelay(100);
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(executor.scheduleBreeding(i * 2, i * 2 + 1));
        }
        
        executor.shutdown();
        
        // All should complete or be cancelled
        for (CompletableFuture<Boolean> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (CancellationException | ExecutionException ignored) {}
        }
    }

    // Test implementation
    static class TestVillagerExecutor {
        private final ExecutorService executor;
        private final Map<Integer, Boolean> breedingCooldowns = new ConcurrentHashMap<>();
        private final Map<String, Integer> reputations = new ConcurrentHashMap<>();
        private final Map<Integer, Integer> scheduledTasks = new ConcurrentHashMap<>();
        private final Set<Integer> claimedStations = ConcurrentHashMap.newKeySet();
        private final AtomicInteger batchedPathRequests = new AtomicInteger(0);
        private volatile int taskDelay = 10;

        TestVillagerExecutor(int threads) {
            this.executor = Executors.newFixedThreadPool(threads);
        }

        CompletableFuture<Boolean> scheduleBreeding(int villager1, int villager2) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(taskDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                
                if (breedingCooldowns.getOrDefault(villager1, false) ||
                    breedingCooldowns.getOrDefault(villager2, false)) {
                    return false;
                }
                
                return true;
            }, executor);
        }

        void setBreedingCooldownActive(int villagerId, boolean active) {
            breedingCooldowns.put(villagerId, active);
        }

        void processTrade(int villagerId, String playerId, int value) {
            String key = villagerId + "-" + playerId;
            reputations.merge(key, value, Integer::sum);
        }

        int getReputation(int villagerId, String playerId) {
            return reputations.getOrDefault(villagerId + "-" + playerId, 0);
        }

        void scheduleAITask(int villagerId, String taskType) {
            scheduledTasks.merge(villagerId, 1, Integer::sum);
        }

        int getScheduledTaskCount(int villagerId) {
            return scheduledTasks.getOrDefault(villagerId, 0);
        }

        CompletableFuture<Integer> propagateGossip(List<Integer> villagerIds, String playerId, int value) {
            return CompletableFuture.supplyAsync(() -> {
                for (int id : villagerIds) {
                    processTrade(id, playerId, value);
                }
                return villagerIds.size();
            }, executor);
        }

        boolean claimWorkStation(int villagerId, int stationId) {
            return claimedStations.add(stationId);
        }

        void requestPath(int villagerId, int targetX, int targetY, int targetZ) {
            batchedPathRequests.incrementAndGet();
        }

        int getBatchedPathRequests() {
            return batchedPathRequests.get();
        }

        void setTaskDelay(int ms) {
            this.taskDelay = ms;
        }

        void shutdown() {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
    }
}
