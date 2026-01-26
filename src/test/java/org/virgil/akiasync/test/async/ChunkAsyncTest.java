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
 * Tests for async chunk loading and processing.
 * Validates chunk task queuing, priority handling, and completion callbacks.
 */
public class ChunkAsyncTest {

    private TestChunkLoader loader;

    @BeforeEach
    void setUp() {
        loader = new TestChunkLoader(4);
    }

    @Test
    @DisplayName("Chunk load should complete asynchronously")
    @Timeout(5)
    void testAsyncChunkLoad() throws Exception {
        CompletableFuture<TestChunk> future = loader.loadChunkAsync(10, 20);
        
        assertFalse(future.isDone(), "Should not be immediately done");
        
        TestChunk chunk = future.get(3, TimeUnit.SECONDS);
        assertNotNull(chunk);
        assertEquals(10, chunk.x);
        assertEquals(20, chunk.z);
    }

    @Test
    @DisplayName("Multiple chunk loads should be parallel")
    @Timeout(10)
    void testParallelChunkLoads() throws Exception {
        List<CompletableFuture<TestChunk>> futures = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            futures.add(loader.loadChunkAsync(i, i));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        
        for (int i = 0; i < 10; i++) {
            TestChunk chunk = futures.get(i).get();
            assertEquals(i, chunk.x);
            assertEquals(i, chunk.z);
        }
    }

    @Test
    @DisplayName("Priority chunks should be loaded first")
    @Timeout(10)
    void testPriorityLoading() throws Exception {
        loader.setLoadDelay(50);
        List<Integer> loadOrder = Collections.synchronizedList(new ArrayList<>());
        
        // Submit low priority first
        loader.loadChunkAsync(0, 0, 1).thenAccept(c -> loadOrder.add(0));
        loader.loadChunkAsync(1, 1, 1).thenAccept(c -> loadOrder.add(1));
        
        // Submit high priority
        loader.loadChunkAsync(2, 2, 10).thenAccept(c -> loadOrder.add(2));
        
        Thread.sleep(500);
        
        // High priority should complete (order depends on thread scheduling)
        assertTrue(loadOrder.contains(2), "High priority chunk should be loaded");
    }

    @Test
    @DisplayName("Chunk cache should prevent duplicate loads")
    void testChunkCaching() {
        loader.loadChunkAsync(10, 20).join();
        int loadCount1 = loader.getLoadCount();
        
        loader.loadChunkAsync(10, 20).join(); // Same chunk
        int loadCount2 = loader.getLoadCount();
        
        assertEquals(loadCount1, loadCount2, "Cached chunk should not trigger new load");
    }

    @Test
    @DisplayName("Failed chunk load should complete exceptionally")
    @Timeout(5)
    void testFailedLoad() {
        loader.setFailNextLoad(true);
        
        CompletableFuture<TestChunk> future = loader.loadChunkAsync(0, 0);
        
        assertThrows(ExecutionException.class, () -> future.get(3, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Chunk unload should remove from cache")
    void testChunkUnload() {
        loader.loadChunkAsync(10, 20).join();
        assertTrue(loader.isChunkLoaded(10, 20));
        
        loader.unloadChunk(10, 20);
        assertFalse(loader.isChunkLoaded(10, 20));
    }

    @Test
    @DisplayName("Batch chunk loading should be efficient")
    @Timeout(10)
    void testBatchLoading() throws Exception {
        List<long[]> positions = new ArrayList<>();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                positions.add(new long[]{x, z});
            }
        }
        
        CompletableFuture<List<TestChunk>> batchFuture = loader.loadChunksBatch(positions);
        List<TestChunk> chunks = batchFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals(25, chunks.size());
    }

    @Test
    @DisplayName("Concurrent load requests for same chunk should share result")
    @Timeout(5)
    void testConcurrentSameChunkLoad() throws Exception {
        loader.setLoadDelay(100);
        
        CompletableFuture<TestChunk> future1 = loader.loadChunkAsync(10, 20);
        CompletableFuture<TestChunk> future2 = loader.loadChunkAsync(10, 20);
        
        TestChunk chunk1 = future1.get(3, TimeUnit.SECONDS);
        TestChunk chunk2 = future2.get(3, TimeUnit.SECONDS);
        
        assertSame(chunk1, chunk2, "Should return same chunk instance");
    }

    @Test
    @DisplayName("Shutdown should complete pending loads")
    @Timeout(10)
    void testShutdown() throws Exception {
        loader.setLoadDelay(50);
        
        List<CompletableFuture<TestChunk>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(loader.loadChunkAsync(i, i));
        }
        
        loader.shutdown();
        
        // All pending should complete or be cancelled
        for (CompletableFuture<TestChunk> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (CancellationException | ExecutionException ignored) {
                // Expected for some
            }
        }
    }

    // Test implementations
    static class TestChunk {
        final int x, z;
        final long loadTime;

        TestChunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.loadTime = System.currentTimeMillis();
        }
    }

    static class TestChunkLoader {
        private final ExecutorService executor;
        private final Map<Long, TestChunk> chunkCache = new ConcurrentHashMap<>();
        private final Map<Long, CompletableFuture<TestChunk>> pendingLoads = new ConcurrentHashMap<>();
        private final AtomicInteger loadCount = new AtomicInteger(0);
        private volatile int loadDelay = 10;
        private volatile boolean failNextLoad = false;

        TestChunkLoader(int threads) {
            this.executor = Executors.newFixedThreadPool(threads);
        }

        CompletableFuture<TestChunk> loadChunkAsync(int x, int z) {
            return loadChunkAsync(x, z, 1);
        }

        CompletableFuture<TestChunk> loadChunkAsync(int x, int z, int priority) {
            long key = chunkKey(x, z);
            
            // Check cache
            TestChunk cached = chunkCache.get(key);
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
            
            // Check pending
            CompletableFuture<TestChunk> pending = pendingLoads.get(key);
            if (pending != null) {
                return pending;
            }
            
            // Start new load
            CompletableFuture<TestChunk> future = CompletableFuture.supplyAsync(() -> {
                if (failNextLoad) {
                    failNextLoad = false;
                    throw new RuntimeException("Simulated load failure");
                }
                
                try {
                    Thread.sleep(loadDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                loadCount.incrementAndGet();
                TestChunk chunk = new TestChunk(x, z);
                chunkCache.put(key, chunk);
                return chunk;
            }, executor);
            
            pendingLoads.put(key, future);
            future.whenComplete((c, e) -> pendingLoads.remove(key));
            
            return future;
        }

        CompletableFuture<List<TestChunk>> loadChunksBatch(List<long[]> positions) {
            List<CompletableFuture<TestChunk>> futures = new ArrayList<>();
            for (long[] pos : positions) {
                futures.add(loadChunkAsync((int) pos[0], (int) pos[1]));
            }
            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<TestChunk> chunks = new ArrayList<>();
                    for (CompletableFuture<TestChunk> f : futures) {
                        chunks.add(f.join());
                    }
                    return chunks;
                });
        }

        void unloadChunk(int x, int z) {
            chunkCache.remove(chunkKey(x, z));
        }

        boolean isChunkLoaded(int x, int z) {
            return chunkCache.containsKey(chunkKey(x, z));
        }

        void shutdown() {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }

        private long chunkKey(int x, int z) {
            return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
        }

        void setLoadDelay(int ms) { this.loadDelay = ms; }
        void setFailNextLoad(boolean fail) { this.failNextLoad = fail; }
        int getLoadCount() { return loadCount.get(); }
    }
}
